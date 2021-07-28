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
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatpumpAlphaInnotec;
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
@Component(name = "Heater.HeatPump.AlphaInnotec",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HeatPumpAlphaInnotecImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpAlphaInnotec {

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

	// This is essential for Modbus to work.
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpAlphaInnotecImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpAlphaInnotec.ChannelId.values(),
				HeatpumpSmartGrid.ChannelId.values(),
				Heater.ChannelId.values());
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
						m(HeatpumpAlphaInnotec.ChannelId.DI_0_EL_SUP_BLOCK, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_1_EL_SUP_BLOCK_SG, new CoilElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_2_POOL_THERMOSTAT, new CoilElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_3_COMPRESSOR1, new CoilElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_4_COMPRESSOR2, new CoilElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_5_AUX1, new CoilElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_6_AUX2, new CoilElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_7_AUX3, new CoilElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotec.ChannelId.IR_0_AVERAGE_TEMP, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_3_RETURN_TEMP_EXT, new UnsignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_4_WATER_TEMP, new UnsignedWordElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_5_FLOW_TEMP_MC1, new UnsignedWordElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_6_FLOW_TEMP_MC2, new UnsignedWordElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_7_FLOW_TEMP_MC3, new UnsignedWordElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_8_HOT_GAS_TEMP, new UnsignedWordElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_9_HEAT_SOURCE_INLET_TEMP, new UnsignedWordElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_10_HEAT_SOURCE_OUTLET_TEMP, new UnsignedWordElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_11_ROOM_REMOTE_ADJ1_TEMP, new UnsignedWordElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_12_ROOM_REMOTE_ADJ2_TEMP, new UnsignedWordElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_13_ROOM_REMOTE_ADJ3_TEMP, new UnsignedWordElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_14_SOLAR_COLLECTOR_TEMP, new UnsignedWordElement(14),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_15_SOLAR_STORAGE_TANK_TEMP, new UnsignedWordElement(15),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_16_EXT_ENERGY_SOURCE_TEMP, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_17_SUPPLY_AIR_TEMP, new UnsignedWordElement(17),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_18_EXTRACT_AIR_TEMP, new UnsignedWordElement(18),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_19_COMPRESSOR_INTAKE_TEMP, new UnsignedWordElement(19),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_20_EVAPORATOR_INTAKE_TEMP, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_21_COMPRESSOR_HEATER_TEMP, new UnsignedWordElement(21),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_22_OVERHEAT, new UnsignedWordElement(22),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_23_OVERHEAT_SETPOINT, new UnsignedWordElement(23),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_24_RBE_ROOM_TEMP, new UnsignedWordElement(24),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_25_RBE_ROOM_TEMP_SETPOINT, new UnsignedWordElement(25),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_26_HIGH_PRESSURE, new UnsignedWordElement(26),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_27_LOW_PRESSURE, new UnsignedWordElement(27),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_28_HOURS_COMP1, new UnsignedWordElement(28),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_29_HOURS_COMP2, new UnsignedWordElement(29),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_30_HOURS_AUX1, new UnsignedWordElement(30),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_31_HOURS_AUX2, new UnsignedWordElement(31),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_32_HOURS_AUX3, new UnsignedWordElement(32),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_33_HOURS_HEAT_PUMP, new UnsignedWordElement(33),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_34_HOURS_CIRCUIT_HEATING, new UnsignedWordElement(34),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_35_HOURS_WATER_HEATING, new UnsignedWordElement(35),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_36_HOURS_POOL_SOLAR, new UnsignedWordElement(36),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_37_STATUS, new UnsignedWordElement(37),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_38_ENERGY_CIRCUIT_HEATING, new UnsignedDoublewordElement(38),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_40_ENERGY_WATER, new UnsignedDoublewordElement(40),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_42_ENERGY_POOL, new UnsignedDoublewordElement(42),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_44_ENERGY_TOTAL, new UnsignedDoublewordElement(44),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.IR_46_ERROR, new UnsignedWordElement(46),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC1ReadCoilsTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotec.ChannelId.COIL_0_ERRORRESET, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyCoilElement(1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_2_HUP, new CoilElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_3_VEN, new CoilElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_4_ZUP, new CoilElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_5_BUP, new CoilElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_6_BOSUP, new CoilElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_7_ZIP, new CoilElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_8_FUP2, new CoilElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_9_FUP3, new CoilElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_10_SLP, new CoilElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_11_SUP, new CoilElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_12_VSK, new CoilElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.COIL_13_FRH, new CoilElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				new FC3ReadRegistersTask(0, Priority.LOW,
						m(HeatpumpAlphaInnotec.ChannelId.HR_0_OUTSIDETEMP, new SignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_1_RETURN_TEMP_SETPOINT, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_2_FLOW_TEMP_SETPOINT_MC1, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_3_FLOW_TEMP_SETPOINT_MC2, new UnsignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_4_FLOW_TEMP_SETPOINT_MC3, new UnsignedWordElement(4),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_5_WATER_TEMP_SETPOINT, new UnsignedWordElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_6_BLOCK_RELEASE, new UnsignedWordElement(6),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_7_CIRCUIT_HEATING_OPERATION_MODE, new UnsignedWordElement(7),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_8_WATER_OPERATION_MODE, new UnsignedWordElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_9_MC2_OPERATION_MODE, new UnsignedWordElement(9),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_10_MC3_OPERATION_MODE, new UnsignedWordElement(10),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_11_COOLING_OPERATION_MODE, new UnsignedWordElement(11),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_12_VENTILATION_OPERATION_MODE, new UnsignedWordElement(12),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_13_POOL_OPERATION_MODE, new UnsignedWordElement(13),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_14_SMART_GRID, new UnsignedWordElement(14),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_15_CURVE_CIRCUIT_HEATING_END_POINT, new UnsignedWordElement(15),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_16_CURVE_CIRCUIT_HEATING_SHIFT, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_17_CURVE_MC1_END_POINT, new UnsignedWordElement(17),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_18_CURVE_MC1_SHIFT, new UnsignedWordElement(18),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_19_CURVE_MC2_END_POINT, new UnsignedWordElement(19),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_20_CURVE_MC2_SHIFT, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_21_CURVE_MC3_END_POINT, new UnsignedWordElement(21),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_22_CURVE_MC3_SHIFT, new UnsignedWordElement(22),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.HR_23_TEMP_PM, new SignedWordElement(23),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					// There is no "write-multiple-coils" command implementation in OpenEMS (yet), so you need a separate write call for each coil.
					new FC5WriteCoilTask(0,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_0_ERRORRESET, new CoilElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(2,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_2_HUP, new CoilElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(3,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_3_VEN, new CoilElement(3),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(4,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_4_ZUP, new CoilElement(4),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(5,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_5_BUP, new CoilElement(5),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(6,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_6_BOSUP, new CoilElement(6),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(7,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_7_ZIP, new CoilElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(8,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_8_FUP2, new CoilElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(9,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_9_FUP3, new CoilElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(10,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_10_SLP, new CoilElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(11,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_11_SUP, new CoilElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(12,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_12_VSK, new CoilElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(13,
							(ModbusCoilElement) m(HeatpumpAlphaInnotec.ChannelId.COIL_13_FRH, new CoilElement(13),
									ElementToChannelConverter.DIRECT_1_TO_1)),

					new FC16WriteRegistersTask(0,
							m(HeatpumpAlphaInnotec.ChannelId.HR_0_OUTSIDETEMP, new SignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_1_RETURN_TEMP_SETPOINT, new UnsignedWordElement(1),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_2_FLOW_TEMP_SETPOINT_MC1, new UnsignedWordElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_3_FLOW_TEMP_SETPOINT_MC2, new UnsignedWordElement(3),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_4_FLOW_TEMP_SETPOINT_MC3, new UnsignedWordElement(4),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_5_WATER_TEMP_SETPOINT, new UnsignedWordElement(5),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_6_BLOCK_RELEASE, new UnsignedWordElement(6),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_7_CIRCUIT_HEATING_OPERATION_MODE, new UnsignedWordElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_8_WATER_OPERATION_MODE, new UnsignedWordElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_9_MC2_OPERATION_MODE, new UnsignedWordElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_10_MC3_OPERATION_MODE, new UnsignedWordElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_11_COOLING_OPERATION_MODE, new UnsignedWordElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_12_VENTILATION_OPERATION_MODE, new UnsignedWordElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_13_POOL_OPERATION_MODE, new UnsignedWordElement(13),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_14_SMART_GRID, new UnsignedWordElement(14),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_15_CURVE_CIRCUIT_HEATING_END_POINT, new UnsignedWordElement(15),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_16_CURVE_CIRCUIT_HEATING_SHIFT, new UnsignedWordElement(16),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_17_CURVE_MC1_END_POINT, new UnsignedWordElement(17),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_18_CURVE_MC1_SHIFT, new UnsignedWordElement(18),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_19_CURVE_MC2_END_POINT, new UnsignedWordElement(19),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_20_CURVE_MC2_SHIFT, new UnsignedWordElement(20),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_21_CURVE_MC3_END_POINT, new UnsignedWordElement(21),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_22_CURVE_MC3_SHIFT, new UnsignedWordElement(22),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_23_TEMP_PM, new SignedWordElement(23),
									ElementToChannelConverter.DIRECT_1_TO_1)
					)
			);
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.componentEnabled && EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE.equals(event.getTopic())) {
			//pumpTest();	// Just for testing
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
								this.logError(this.log, "Could not write to channel 'HR_6_BLOCK_RELEASE'. "
										+ "Reason: " + e.getMessage());
							}
						}
					} else {
						try {
							setBlockRelease(BlockRelease.RELEASE_2_COMPRESSORS.getValue());
						} catch (OpenemsError.OpenemsNamedException e) {
							this.logError(this.log, "Could not write to channel 'HR_6_BLOCK_RELEASE'. "
									+ "Reason: " + e.getMessage());
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
							this.logError(this.log, "Could not write to operating mode channel. "
									+ "Reason: " + e.getMessage());
						}

					});
					/*
					if (this.useExceptionalState) {
						// Currently not used. If exceptionalStateValue should do more than just on/off, code for that goes here.
					}
					*/
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
						this.logError(this.log, "Could not write to operating mode channels. "
								+ "Reason: " + e.getMessage());
					}
				}
			}

		}
		this.logInfo(this.log, "Outside temperature: " + getOutsideTemp().asEnum());
		
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

	// Just for testing. Needs to be uncommented in handleEvent() to work.
	/*
	protected void pumpTest() {
		this.logInfo(this.log, "--Testing Channels--");
		this.logInfo(this.log, "State: " + getHeatpumpOperatingMode().asEnum().getName());
		this.logInfo(this.log, "Smart Grid State name: " + getSmartGridState().asEnum().getName());	// Gets the "name" field of the Enum.
		this.logInfo(this.log, "Smart Grid State number: " + getSmartGridState().get());	// This gets the integer value.
		this.logInfo(this.log, "Block/release: " + getBlockRelease().asEnum().getName());
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


		// Test Modbus write. Write an integer that is supplied by the enum.
		if (this.testcounter == 5) {
			this.logInfo(this.log, "Set " + SmartGridState.SG1_BLOCKED.getName());
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG1_BLOCKED.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to "
						+ SmartGridState.SG1_BLOCKED.getName());
			}
		}

		if (this.testcounter == 10) {
			this.logInfo(this.log, "Set " + SmartGridState.SG3_STANDARD.getName());
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to "
						+ SmartGridState.SG3_STANDARD.getName());
			}
		}

		// Test trying to write unsupported values (by Modbus device). Apparently nothing happens.
		if (this.testcounter == 15) {
			this.logInfo(this.log, "Set " + SmartGridState.UNDEFINED.getName());
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.UNDEFINED.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to "
						+ SmartGridState.UNDEFINED.getName());
			}
			this.logInfo(this.log, "Channel setNextWriteValue: "
					+ getSmartGridStateChannel().getNextWriteValue().get());
			this.logInfo(this.log, "");
		}

		if (this.testcounter == 20) {
			this.logInfo(this.log, "Set " + SmartGridState.SG3_STANDARD.getName());
			this.logInfo(this.log, "");
			try {
				setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Unable to set SmartGridState to "
						+ SmartGridState.SG3_STANDARD.getName());
			}
		}

		this.testcounter++;
	}
	*/
}
