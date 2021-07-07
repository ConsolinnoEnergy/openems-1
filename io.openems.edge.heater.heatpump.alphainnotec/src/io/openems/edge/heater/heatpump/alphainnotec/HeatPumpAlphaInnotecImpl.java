package io.openems.edge.heater.heatpump.alphainnotec;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.heatpump.alphainnotec.api.BlockRelease;
import io.openems.edge.heater.heatpump.alphainnotec.api.CoolingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.PoolMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.VentilationMode;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.SystemStatus;
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatpumpAlphaInnotecChannel;
import io.openems.edge.heater.api.HeatpumpSmartGrid;
import io.openems.edge.heater.api.SmartGridState;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * This module reads the most important variables available via Modbus from an Alpha Innotec heat pump and maps them to
 * OpenEMS channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpAlphaInnotecChannel, HeatpumpSmartGrid and Heater.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatPumpAlphaInnotec",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HeatPumpAlphaInnotecImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpAlphaInnotecChannel {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;
	
	private final Logger log = LoggerFactory.getLogger(HeatPumpAlphaInnotecImpl.class);
	private boolean debug;
	private boolean componentEnabled;
	private boolean readOnly;

	private boolean turnOnHeatpump;
	private String[] defaultModesOfOperation;
	private boolean useEnableSignal;
	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_ALPHA_INNOTEC_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "HEAT_PUMP_ALPHA_INNOTEC_EXCEPTIONAL_STATE_IDENTIFIER";

	private int testcounter = 0;

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpAlphaInnotecImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpAlphaInnotecChannel.ChannelId.values(),
				HeatpumpSmartGrid.ChannelId.values(),	// Even though HeatpumpAlphaInnotecChannel extends this channel, it needs to be added separately.
				Heater.ChannelId.values());		// Even though HeatpumpSmartGrid extends this channel, it needs to be added separately.
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.debug = config.debug();
		this.componentEnabled = config.enabled();
		this.readOnly = config.readOnly();
		TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
		this.useEnableSignal = config.useEnableSignalChannel();
		this.defaultModesOfOperation = config.defaultModesOfOperation();
		if (this.useEnableSignal) {
			String timerIdEnableSignal;
			if (config.enableSignalTimerIsCyclesNotSeconds()) {
				timerIdEnableSignal = "TimerByCycles";
			} else {
				timerIdEnableSignal = "TimerByTime";
			}
			timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerIdEnableSignal, config.waitTimeEnableSignal());
			this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
		}
		this.useExceptionalState = config.useExceptionalState();
		if (this.useExceptionalState) {
			String timerIdEnableSignal;
			if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
				timerIdEnableSignal = "TimerByCycles";
			} else {
				timerIdEnableSignal = "TimerByTime";
			}
			timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerIdEnableSignal, config.waitTimeExceptionalState());
			this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
		}

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol protocol = new ModbusProtocol(this,
				new FC2ReadInputsTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_0_EVU, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_1_EVU2, new CoilElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_2_SWT, new CoilElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_3_VD1, new CoilElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_4_VD2, new CoilElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_5_ZWE1, new CoilElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_6_ZWE2, new CoilElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.DI_7_ZWE3, new CoilElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_0_MITTELTEMP, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_3_RUECKEXTERN, new UnsignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_4_TRINKWWTEMP, new UnsignedWordElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_5_MK1VORLAUF, new UnsignedWordElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_6_MK2VORLAUF, new UnsignedWordElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_7_MK3VORLAUF, new UnsignedWordElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_8_HEISSGASTEMP, new UnsignedWordElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_9_WQEINTRITT, new UnsignedWordElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_10_WQAUSTRITT, new UnsignedWordElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_11_RAUMFV1, new UnsignedWordElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_12_RAUMFV2, new UnsignedWordElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_13_RAUMFV3, new UnsignedWordElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_14_SOLARKOLLEKTOR, new UnsignedWordElement(14),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_15_SOLARSPEICHER, new UnsignedWordElement(15),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_16_EXTEQ, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_17_ZULUFTTEMP, new UnsignedWordElement(17),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_18_ABLUFTTEMP, new UnsignedWordElement(18),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_19_ANSAUGTEMPVDICHTER, new UnsignedWordElement(19),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_20_ANSAUGTEMPVDAMPFER, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_21_TEMPVDHEIZUNG, new UnsignedWordElement(21),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_22_UEBERHITZ, new UnsignedWordElement(22),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_23_UEBERHITZSOLL, new UnsignedWordElement(23),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_24_RBERAUMTEMPIST, new UnsignedWordElement(24),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_25_RBERAUMTEMPSOLL, new UnsignedWordElement(25),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_26_DRUCKHD, new UnsignedWordElement(26),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_27_DRUCKND, new UnsignedWordElement(27),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_28_TVD1, new UnsignedWordElement(28),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_29_TVD2, new UnsignedWordElement(29),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_30_TZWE1, new UnsignedWordElement(30),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_31_TZWE2, new UnsignedWordElement(31),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_32_TZWE3, new UnsignedWordElement(32),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_33_TWAERMEPUMPE, new UnsignedWordElement(33),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_34_THEIZUNG, new UnsignedWordElement(34),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_35_TTRINKWW, new UnsignedWordElement(35),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_36_TSWOPV, new UnsignedWordElement(36),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_37_STATUS, new UnsignedWordElement(37),
								ElementToChannelConverter.DIRECT_1_TO_1),
						/* A double word combines two 16 bit registers to a 32 bit value. This reads two registers, so
						   the next element address is +2 instead of +1 for a regular register. */
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_38_WHHEIZUNG, new UnsignedDoublewordElement(38),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_40_WHTRINKWW, new UnsignedDoublewordElement(40),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_42_WHPOOL, new UnsignedDoublewordElement(42),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_44_WHTOTAL, new UnsignedDoublewordElement(44),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.IR_46_ERROR, new UnsignedWordElement(46),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC1ReadCoilsTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_0_ERRORRESET, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						/* A Modbus read commands reads everything from start address to finish address. If there is a
						   gap, you must place a dummy element to fill the gap or end the read command there and start
						   with a new read where you want to continue. */
						new DummyCoilElement(1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_2_HUP, new CoilElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_3_VEN, new CoilElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_4_ZUP, new CoilElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_5_BUP, new CoilElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_6_BOSUP, new CoilElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_7_ZIP, new CoilElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_8_FUP2, new CoilElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_9_FUP3, new CoilElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_10_SLP, new CoilElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_11_SUP, new CoilElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_12_VSK, new CoilElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_13_FRH, new CoilElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				new FC3ReadRegistersTask(0, Priority.LOW,
						/* Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
						   to negative. That means if the value you read is positive and <32767, there is no difference
						   between signed and unsigned. */
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_0_OUTSIDETEMP, new SignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_1_RUECKTEMPSOLL, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_2_MK1VORTEMPSOLL, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_3_MK2VORTEMPSOLL, new UnsignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_4_MK3VORTEMPSOLL, new UnsignedWordElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_5_TRINKWWTEMPSOLL, new UnsignedWordElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_6_BLOCK_RELEASE, new UnsignedWordElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_7_HEIZUNGRUNSTATE, new UnsignedWordElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_8_TRINKWWRUNSTATE, new UnsignedWordElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_9_MK2RUNSTATE, new UnsignedWordElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_10_MK3RUNSTATE, new UnsignedWordElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_11_COOLINGRUNSTATE, new UnsignedWordElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_12_VENTILATIONRUNSTATE, new UnsignedWordElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_13_POOLRUNSTATE, new UnsignedWordElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_14_SMART_GRID, new UnsignedWordElement(14),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_15_HKHEIZUNGENDPKT, new UnsignedWordElement(15),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_16_HKHEIZUNGPARAVER, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_17_HKMK1ENDPKT, new UnsignedWordElement(17),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_18_HKMK1PARAVER, new UnsignedWordElement(18),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_19_HKMK2ENDPKT, new UnsignedWordElement(19),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_20_HKMK2PARAVER, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_21_HKMK3ENDPKT, new UnsignedWordElement(21),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_22_HKMK3PARAVER, new UnsignedWordElement(22),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotecChannel.ChannelId.HR_23_TEMPPM, new SignedWordElement(23),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					/* Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
				       Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
				       "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
				       "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
				       two separate channels.
				       That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
				       watch the order in which you call read and write tasks.
				       Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
				       not be transferred to the "value" field of the channel, unless you add code that does that. */

					// There is no "write-multiple-coils" command implementation in OpenEMS, so you need a separate write call for each coil.
					new FC5WriteCoilTask(0,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_0_ERRORRESET, new CoilElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(2,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_2_HUP, new CoilElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(3,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_3_VEN, new CoilElement(3),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(4,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_4_ZUP, new CoilElement(4),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(5,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_5_BUP, new CoilElement(5),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(6,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_6_BOSUP, new CoilElement(6),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(7,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_7_ZIP, new CoilElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(8,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_8_FUP2, new CoilElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(9,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_9_FUP3, new CoilElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(10,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_10_SLP, new CoilElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(11,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_11_SUP, new CoilElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(12,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_12_VSK, new CoilElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(13,
							(ModbusCoilElement) m(HeatpumpAlphaInnotecChannel.ChannelId.COIL_13_FRH, new CoilElement(13),
									ElementToChannelConverter.DIRECT_1_TO_1)),

					new FC16WriteRegistersTask(0,
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_0_OUTSIDETEMP, new SignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_1_RUECKTEMPSOLL, new UnsignedWordElement(1),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_2_MK1VORTEMPSOLL, new UnsignedWordElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_3_MK2VORTEMPSOLL, new UnsignedWordElement(3),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_4_MK3VORTEMPSOLL, new UnsignedWordElement(4),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_5_TRINKWWTEMPSOLL, new UnsignedWordElement(5),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_6_BLOCK_RELEASE, new UnsignedWordElement(6),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_7_HEIZUNGRUNSTATE, new UnsignedWordElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_8_TRINKWWRUNSTATE, new UnsignedWordElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_9_MK2RUNSTATE, new UnsignedWordElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_10_MK3RUNSTATE, new UnsignedWordElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_11_COOLINGRUNSTATE, new UnsignedWordElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_12_VENTILATIONRUNSTATE, new UnsignedWordElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_13_POOLRUNSTATE, new UnsignedWordElement(13),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_14_SMART_GRID, new UnsignedWordElement(14),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_15_HKHEIZUNGENDPKT, new UnsignedWordElement(15),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_16_HKHEIZUNGPARAVER, new UnsignedWordElement(16),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_17_HKMK1ENDPKT, new UnsignedWordElement(17),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_18_HKMK1PARAVER, new UnsignedWordElement(18),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_19_HKMK2ENDPKT, new UnsignedWordElement(19),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_20_HKMK2PARAVER, new UnsignedWordElement(20),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_21_HKMK3ENDPKT, new UnsignedWordElement(21),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_22_HKMK3PARAVER, new UnsignedWordElement(22),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotecChannel.ChannelId.HR_23_TEMPPM, new SignedWordElement(23),
									ElementToChannelConverter.DIRECT_1_TO_1)
					)
			);
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.componentEnabled && EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE.equals(event.getTopic())) {
			//channeltest();	// Just for testing
			this.channelmapping();
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		SystemStatus heatpumpSystemStatus = getHeatpumpOperatingMode().asEnum();
		switch (heatpumpSystemStatus) {
			case OFF:
				_setHeaterState(HeaterState.OFF.getValue());
				break;
			case BLOCKED:
				_setHeaterState(HeaterState.BLOCKED.getValue());
				break;
			case DEFROST:
			case COOLING:
			case POOL_HEATING:
			case ROOM_HEATING:
			case DOMESTIC_HOT_WATER_HEATING:
			case EXTERNAL_ENERGY_SOURCE:
				_setHeaterState(HeaterState.HEATING.getValue());
				break;
			case UNDEFINED:
			default:
				_setHeaterState(HeaterState.UNDEFINED.getValue());
				break;
		}

		// Map smart grid from modbus.
		if (getSmartGridFromModbus().isDefined()) {
			int smartGridFromModbus = getSmartGridFromModbus().get();
			switch (smartGridFromModbus) {
				case 0:
					_setSmartGridState(SmartGridState.SG1_BLOCKED.getValue());
					break;
				case 1:
					_setSmartGridState(SmartGridState.SG2_LOW.getValue());
					break;
				case 2:
					_setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
					break;
				case 3:
					_setSmartGridState(SmartGridState.SG4_HIGH.getValue());
					break;
				default:
					this._setSmartGridState(SmartGridState.UNDEFINED.getValue());
			}
		} else {
			_setSmartGridState(SmartGridState.UNDEFINED.getValue());
		}

		// The value in the channel can be null. Use "orElse" to avoid null pointer exception.
		int errorCode = getErrorCode().orElse(0);
		if (errorCode != 0) {
			_setErrorMessage("Error code: " + errorCode);
		} else {
			_setErrorMessage("No error");
		}

		if (this.readOnly == false) {

			// Map smart grid write.
			Optional<Integer> smartGridStateWrite = getSmartGridStateChannel().getNextWriteValueAndReset();
			if (smartGridStateWrite.isPresent()) {
				try {
					switch (smartGridStateWrite.get()) {
						case 1:
							setSmartGridToModbus(0);	// Blocked
							break;
						case 2:
							setSmartGridToModbus(1);	// Smart Grid Low
							break;
						case 3:
							setSmartGridToModbus(2);	// Standard
							break;
						case 4:
							setSmartGridToModbus(3);	// Smart Grid High
							break;
						default:
							/* Manual says: "If a value is no longer to be predefined, a value outside the defined
							   limits must be transferred." So hopefully this is how you switch smart grid off. */
							setSmartGridToModbus(smartGridStateWrite.get());
					}
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Unable to set Smart Grid State.");
				}
			}

			if (this.useEnableSignal || this.useExceptionalState) {

				// Handle EnableSignal.
				if (this.useEnableSignal) {
					this.turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);
				}

				// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
				int exceptionalStateValue = 0;
				boolean exceptionalStateActive = false;
				if (this.useExceptionalState) {
					exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
					if (exceptionalStateActive) {
						exceptionalStateValue = this.getExceptionalStateValue();
						if (exceptionalStateValue <= 0) {
							// Turn off heat pump when ExceptionalStateValue = 0.
							this.turnOnHeatpump = false;
						} else {
							// When ExceptionalStateValue is > 0, turn heat pump on.
							this.turnOnHeatpump = true;
							/*
							if (exceptionalStateValue > 100) {
								exceptionalStateValue = 100;
							}
							*/
						}
					}
				}

				if (this.turnOnHeatpump) {

					// Make sure heat pump is not blocked or partially blocked.
					if (getBlockRelease().isDefined()) {
						if ((getBlockRelease().asEnum() == BlockRelease.RELEASE_2_COMPRESSORS) == false) {
							try {
								setBlockRelease(BlockRelease.RELEASE_2_COMPRESSORS.getValue());
							} catch (OpenemsError.OpenemsNamedException e) {
								e.printStackTrace();
							}
						}
					} else {
						try {
							setBlockRelease(BlockRelease.RELEASE_2_COMPRESSORS.getValue());
						} catch (OpenemsError.OpenemsNamedException e) {
							e.printStackTrace();
						}
					}

					Arrays.stream(this.defaultModesOfOperation).forEach(string -> {
						try {
							if (string.equals("") == false) {
								switch (string) {
									case "Heating":
										setHeatingOperationMode(HeatingMode.AUTOMATIC.getValue());
										break;
									case "DomesticHotWater":
										setDomesticHotWaterOperationMode(HeatingMode.AUTOMATIC.getValue());
										break;
									case "MixingCircuit2":
										setCircuit2OperationMode(HeatingMode.AUTOMATIC.getValue());
										break;
									case "MixingCircuit3":
										setCircuit3OperationMode(HeatingMode.AUTOMATIC.getValue());
										break;
									case "Cooling":
										setCoolingOperationMode(CoolingMode.AUTOMATIC.getValue());
										break;
									case "Ventilation":
										setVentilationOperationMode(VentilationMode.AUTOMATIC.getValue());
										break;
									case "SwimmingPool":
										setPoolHeatingOperationMode(PoolMode.AUTOMATIC.getValue());
										break;
								}
							}
						} catch (OpenemsError.OpenemsNamedException e) {
							e.printStackTrace();
						}

					});
					/* if (this.useExceptionalState) {
						// Currently not used. If exceptionalStateValue should do more than just on/off, code for that goes here.
					} */
				} else {
					try {
						setHeatingOperationMode(HeatingMode.OFF.getValue());
						setDomesticHotWaterOperationMode(HeatingMode.OFF.getValue());
						setCircuit2OperationMode(HeatingMode.OFF.getValue());
						setCircuit3OperationMode(HeatingMode.OFF.getValue());
						setCoolingOperationMode(CoolingMode.OFF.getValue());
						setVentilationOperationMode(VentilationMode.OFF.getValue());
						setPoolHeatingOperationMode(PoolMode.OFF.getValue());
						// setBlockRelease(BlockRelease.BLOCKED.getValue());	// Better not use that. If the heat pump has frost protection, this might disable it.
					} catch (OpenemsError.OpenemsNamedException e) {
						e.printStackTrace();
					}
				}
			}

		}

		
		if (this.debug) {
			this.logInfo(this.log, "--Heat pump Alpha Innotec--");
			this.logInfo(this.log, "System State: " + getHeatpumpOperatingMode().asEnum().getName());
			this.logInfo(this.log, "Smart Grid State name: " + getSmartGridState().asEnum().getName());
			this.logInfo(this.log, "Block / release: " + getBlockRelease().asEnum().getName());
			this.logInfo(this.log, "Heating mode: " + getHeatingOperationMode().asEnum().getName());
			this.logInfo(this.log, "Domestic hot water mode: " + getDomesticHotWaterOperationMode().asEnum().getName());
			this.logInfo(this.log, "Cooling mode: " + getCoolingOperationMode().asEnum().getName());
			this.logInfo(this.log, "Flow temperature: " + getFlowTemperature());
			this.logInfo(this.log, "Flow temp circuit 1: " + getCircuit1FlowTemp());
			this.logInfo(this.log, "Flow temp circuit 1 setpoint: " + getCircuit1FlowTempSetpoint());
			this.logInfo(this.log, "Return temperature: " + getReturnTemperature());
			this.logInfo(this.log, "Return temp setpoint: " + getReturnTempSetpoint());
			this.logInfo(this.log, "Outside temperature: " + getOutsideTemp());
			this.logInfo(this.log, "Error Code: " + getErrorCode().get());	// Code "0" means no error. "Null" means no reading (yet).
			this.logInfo(this.log, "");
		}

	}

	// Just for testing. Also, example code with some explanations.
	protected void channeltest() {
		this.logInfo(this.log, "--Testing Channels--");
		this.logInfo(this.log, "State: " + getHeatpumpOperatingMode().asEnum().getName());
		this.logInfo(this.log, "Smart Grid State name: " + getSmartGridState().asEnum().getName());	// Gets the "name" field of the Enum.
		this.logInfo(this.log, "Smart Grid State number: " + getSmartGridState().get());	// The variable in the channel is actually the integer of the Enum "value" field.
		this.logInfo(this.log, "Run clearance: " + getBlockRelease().asEnum().getName());
		this.logInfo(this.log, "Heizung State: " + getHeatingOperationMode().asEnum().getName());
		this.logInfo(this.log, "Kühlung State: " + getCoolingOperationMode().asEnum().getName());
		this.logInfo(this.log, "Heizkurve MK1 Parallelversch.: " + getHeatingCurveCircuit1ParallelShift());
		this.logInfo(this.log, "Temp +- (signed): " + getTempPlusMinus());
		this.logInfo(this.log, "Mitteltemp: " + getAverageTemp().get());
		this.logInfo(this.log, "Vorlauftemp: " + getFlowTemperature());
		this.logInfo(this.log, "Rücklauftemp: " + getReturnTemperature());
		this.logInfo(this.log, "Aussentemp (signed): " + getOutsideTemp());	// Test if variables that can be negative (signed) display correctly. Could not test as temperature was not negative.
		this.logInfo(this.log, "Rücklauftemp soll (unsigned): " + getReturnTempSetpoint());
		this.logInfo(this.log, "Wärmemenge Heizung (double): " + getHeatAmountHeating());	// Test if 32 bit integers (doubleword) are translated correctly.
		this.logInfo(this.log, "RBE ist: " + getRbeRoomTempActual());
		this.logInfo(this.log, "RBE soll: " + getRbeRoomTempSetpoint());
		this.logInfo(this.log, "EVU: " + getEvuActive());	// Not sure what this is doing. When testing, this was "true" when pump state said "cooling mode", even though pump state has a "EVU-Sperre" status.
		this.logInfo(this.log, "EVU2: " + getEvu2Active());	// Not sure what this is doing. I expected setting smart grid status to "off" would trigger this, but it remained "false" when smart grid state was "off". Documentation says EVU2 = "true" when smart grid state = "off".
		this.logInfo(this.log, "Verdichter1: " + getVD1active());
		this.logInfo(this.log, "Verdichter2: " + getVD1active());
		this.logInfo(this.log, "ZWE3 (optional): " + getVD1active());	// Test what readings you get from Modbus variables that are not supported by the heat pump model.
		this.logInfo(this.log, "HUP: " + getForceOnHup());
		this.logInfo(this.log, "Error Code: " + getErrorCode());	// Code "0" means no error. "Null" means no reading (yet).
		this.logInfo(this.log, "");


		// Test Modbus write. Example using Enum name field to set value. In effect, this writes an integer.
		if (this.testcounter == 5) {
			this.logInfo(this.log, "Smart Grid off");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG1_BLOCKED.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to OFF.");
			}

		}

		if (this.testcounter == 10) {
			this.logInfo(this.log, "Smart Grid standard");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to Standard.");
			}
		}

		// Test trying to write unsupported values (by Modbus device). Apparently nothing happens.
		if (this.testcounter == 15) {
			this.logInfo(this.log, "Smart Grid undefined");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.UNDEFINED.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to Undefined.");
			}
			this.logInfo(this.log, "Channel setNextWriteValue: " + getSmartGridState());
			this.logInfo(this.log, "");
		}

		if (this.testcounter == 20) {
			this.logInfo(this.log, "Smart Grid standard");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to Standard.");
			}
		}

		/* The channel is an Enum channel. But since Enum channels are Integer channels, you can just write an integer
		   in them. Better to use SmartGridState.OFF.getValue(), as this gives the reader more information what the
		   value does. */
		if (this.testcounter == 25) {
			this.logInfo(this.log, "Smart Grid 0");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(0);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to OFF.");
			}
		}

		if (this.testcounter == 30) {
			this.logInfo(this.log, "Smart Grid 2");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(2);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to Standard.");
			}
		}

		/* You can write any integer into an Enum channel. The value will be in the channel and will be sent as a
		   Modbus write, but the device will just ignore it if it is outside the valid values.
		   Beware that there is no warning or error message that tells you that the value is not valid. Creating
		   the channel as an Enum channel does not limit the input to valid values. */
		if (this.testcounter == 35) {
			this.logInfo(this.log, "Smart Grid 6");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(6);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to unreasonable values.");
			}
			this.logInfo(this.log, "Channel setNextWriteValue: " + getSmartGridState());
			this.logInfo(this.log, "");
		}

		if (this.testcounter == 40) {
			this.logInfo(this.log, "Smart Grid standard");
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to Standard.");
			}
		}

		this.testcounter++;
	}
}
