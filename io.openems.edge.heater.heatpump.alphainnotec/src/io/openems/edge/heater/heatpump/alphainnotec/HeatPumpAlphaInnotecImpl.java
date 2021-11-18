package io.openems.edge.heater.heatpump.alphainnotec;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyCoilElement;
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
import io.openems.edge.heater.api.HeatpumpControlMode;
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
	private boolean printInfoToLog;
	private boolean readOnly;

	private HeatingMode heatingModeSetting;
	private HeatingMode domesticHotWaterModeSetting;
	private HeatingMode mixingCircuit2ModeSetting;
	private HeatingMode mixingCircuit3ModeSetting;
	private CoolingMode coolingModeSetting;
	private PoolMode poolModeSetting;
	private VentilationMode ventilationModeSetting;

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

		this.printInfoToLog = config.printInfoToLog();
		this.readOnly = config.readOnly();
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			// Use SmartGridState or EnableSignal & ExceptionalState to control heat pump.
			this._setUseSmartGridState(config.openEmsControlMode() == HeatpumpControlMode.SMART_GRID_STATE);
			this.getUseSmartGridStateChannel().nextProcessImage();

			this.heatingModeSetting = config.defaultHeatingMode();
			this.domesticHotWaterModeSetting = config.defaultDomesticHotWaterMode();
			this.mixingCircuit2ModeSetting = config.defaultMixingCircuit2Mode();
			this.mixingCircuit3ModeSetting = config.defaultMixingCircuit3Mode();
			this.coolingModeSetting = config.defaultCoolingMode();
			this.poolModeSetting = config.defaultPoolMode();
			this.ventilationModeSetting = config.defaultVentilationMode();
			this.initializeTimers(config);
		}
	}

	private void initializeTimers(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
		timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
		this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
		this.useExceptionalState = config.useExceptionalState();
		if (this.useExceptionalState) {
			timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
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
						m(HeatpumpAlphaInnotec.ChannelId.DI_0_DMS_BLOCK, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpAlphaInnotec.ChannelId.DI_1_DMS_BLOCK_SG, new CoilElement(1),
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
							m(HeatpumpAlphaInnotec.ChannelId.COIL_0_ERRORRESET, new CoilElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(2,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_2_HUP, new CoilElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(3,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_3_VEN, new CoilElement(3),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(4,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_4_ZUP, new CoilElement(4),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(5,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_5_BUP, new CoilElement(5),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(6,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_6_BOSUP, new CoilElement(6),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(7,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_7_ZIP, new CoilElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(8,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_8_FUP2, new CoilElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(9,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_9_FUP3, new CoilElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(10,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_10_SLP, new CoilElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(11,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_11_SUP, new CoilElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(12,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_12_VSK, new CoilElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC5WriteCoilTask(13,
							m(HeatpumpAlphaInnotec.ChannelId.COIL_13_FRH, new CoilElement(13),
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
							m(HeatpumpAlphaInnotec.ChannelId.HR_7_MODBUS, new UnsignedWordElement(7),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_8_MODBUS, new UnsignedWordElement(8),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_9_MODBUS, new UnsignedWordElement(9),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_10_MODBUS, new UnsignedWordElement(10),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_11_MODBUS, new UnsignedWordElement(11),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_12_MODBUS, new UnsignedWordElement(12),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpAlphaInnotec.ChannelId.HR_13_MODBUS, new UnsignedWordElement(13),
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
		if (this.isEnabled() == false) {
			return;
		}
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
			this.channelmapping();
			if (this.printInfoToLog) {
				this.printInfo();
			}
		} else if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		if (this.getHeatpumpOperatingMode().isDefined()) {
			SystemStatus heatpumpSystemStatus = this.getHeatpumpOperatingMode().asEnum();
			switch (heatpumpSystemStatus) {
				case OFF:
					this._setHeaterState(HeaterState.OFF.getValue());
					break;
				case BLOCKED:
					this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
					break;
				case DEFROST:
				case COOLING:
				case POOL_HEATING:
				case ROOM_HEATING:
				case DOMESTIC_HOT_WATER_HEATING:
				case EXTERNAL_ENERGY_SOURCE:
					this._setHeaterState(HeaterState.RUNNING.getValue());
					break;
				default:
					this._setHeaterState(HeaterState.UNDEFINED.getValue());
					break;
			}
		} else {
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();

		// Map smart grid from modbus.
		if (getSmartGridFromModbus().isDefined()) {
			int smartGridFromModbus = getSmartGridFromModbus().get();
			switch (smartGridFromModbus) {
				case 0:
					this._setSmartGridState(SmartGridState.SG1_BLOCKED.getValue());
					break;
				case 1:
					this._setSmartGridState(SmartGridState.SG2_LOW.getValue());
					break;
				case 2:
					this._setSmartGridState(SmartGridState.SG3_STANDARD.getValue());
					break;
				case 3:
					this._setSmartGridState(SmartGridState.SG4_HIGH.getValue());
					break;
				default:
					this._setSmartGridState(SmartGridState.UNDEFINED.getValue());
			}
		} else {
			this._setSmartGridState(SmartGridState.UNDEFINED.getValue());
		}
		this.getSmartGridStateChannel().nextProcessImage();

		// Get errors
		if (this.getErrorCode().isDefined()) {
			int errorCode = this.getErrorCode().get();
			if (errorCode != 0) {
				this._setErrorMessage("Error code: " + errorCode);
			} else {
				this._setErrorMessage("No error");
			}
		} else {
			this._setErrorMessage("No Modbus connection");
		}
		this.getErrorMessageChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 * The heat pump is controlled either by EnableSignal & ExceptionalState or SmartGridState. The channel
	 * UseSmartGridState determines the control mode.
	 * Operating mode channels get special treatment because these are used by EnableSignal/ExceptionalState to switch
	 * the heat pump on/off. The write of the operating mode channels is not mapped to Modbus. This is done by duplicate
	 * ’private’ channels. The write to the ’public’ operating mode channels is stored in a local variable and sent to
	 * Modbus using the ’private’ channels.
	 * The benefit of this design is that when EnableSignal is false (heat pump off), writing to the operating mode
	 * channels is still registered and the value saved, but not executed. The changed operating mode is then applied
	 * once EnableSignal is true. This way you don't have to pay attention to the state of the heat pump when writing
	 * in the operating mode channels.
	 */
	protected void writeCommands() {
		// Collect operating mode channels ’nextWrite’.
		Optional<Integer> heatingModeOptional = this.getHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (heatingModeOptional.isPresent()) {
			int enumAsInt = heatingModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 4) {
				this.heatingModeSetting = HeatingMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> domesticHotWaterModeOptional = this.getDomesticHotWaterOperationModeChannel().getNextWriteValueAndReset();
		if (domesticHotWaterModeOptional.isPresent()) {
			int enumAsInt = domesticHotWaterModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 4) {
				this.domesticHotWaterModeSetting = HeatingMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> mixingCircuit2ModeOptional = this.getCircuit2OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit2ModeOptional.isPresent()) {
			int enumAsInt = mixingCircuit2ModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 4) {
				this.mixingCircuit2ModeSetting = HeatingMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> mixingCircuit3ModeOptional = this.getCircuit3OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit3ModeOptional.isPresent()) {
			int enumAsInt = mixingCircuit3ModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 4) {
				this.mixingCircuit3ModeSetting = HeatingMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> coolingModeOptional = this.getCoolingOperationModeChannel().getNextWriteValueAndReset();
		if (coolingModeOptional.isPresent()) {
			int enumAsInt = coolingModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 1) {
				this.coolingModeSetting = CoolingMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> poolModeOptional = this.getPoolHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (poolModeOptional.isPresent()) {
			int enumAsInt = poolModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != 1) {
				this.poolModeSetting = PoolMode.valueOf(enumAsInt);
			}
		}
		Optional<Integer> ventilationModeOptional = this.getVentilationOperationModeChannel().getNextWriteValueAndReset();
		if (ventilationModeOptional.isPresent()) {
			int enumAsInt = ventilationModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 3) {
				this.ventilationModeSetting = VentilationMode.valueOf(enumAsInt);
			}
		}

		int heatingModeToModbus = HeatingMode.OFF.getValue();			// These values are used if ’turnOnHeatpump = false’
		int domesticHotWaterModeToModbus = HeatingMode.OFF.getValue();
		int mixingCircuit2ModeToModbus = HeatingMode.OFF.getValue();
		int mixingCircuit3ModeToModbus = HeatingMode.OFF.getValue();
		int coolingModeToModbus = CoolingMode.OFF.getValue();
		int poolModeToModbus = PoolMode.OFF.getValue();
		int ventilationModeToModbus = VentilationMode.OFF.getValue();
		if (this.getUseSmartGridState().isDefined() && this.getUseSmartGridState().get()) {
			// Set operating mode values from config/channels.
			heatingModeToModbus = this.heatingModeSetting.getValue();
			domesticHotWaterModeToModbus = this.domesticHotWaterModeSetting.getValue();
			mixingCircuit2ModeToModbus = this.mixingCircuit2ModeSetting.getValue();
			mixingCircuit3ModeToModbus = this.mixingCircuit3ModeSetting.getValue();
			coolingModeToModbus = this.coolingModeSetting.getValue();
			poolModeToModbus = this.poolModeSetting.getValue();
			ventilationModeToModbus = this.ventilationModeSetting.getValue();

			// Map smart grid write.
			Optional<Integer> smartGridStateWrite = this.getSmartGridStateChannel().getNextWriteValueAndReset();
			if (smartGridStateWrite.isPresent()) {
				try {
					switch (SmartGridState.valueOf(smartGridStateWrite.get())) {
						case SG1_BLOCKED:
							this.setSmartGridToModbus(0);	// Blocked
							break;
						case SG2_LOW:
							this.setSmartGridToModbus(1);	// Smart Grid Low
							break;
						case SG3_STANDARD:
							this.setSmartGridToModbus(2);	// Standard
							break;
						case SG4_HIGH:
							this.setSmartGridToModbus(3);	// Smart Grid High
							break;
						default:
							/* Manual says: "If a value is no longer to be predefined, a value outside the defined
							   limits must be transferred." So hopefully this is how you switch smart grid off. */
							this.setSmartGridToModbus(smartGridStateWrite.get());
					}
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Unable to set Smart Grid State.");
				}
			}
		} else {
			// Make sure smart grid is disabled
			if (getSmartGridFromModbus().isDefined()) {
				int smartGridFromModbus = getSmartGridFromModbus().get();
				if (smartGridFromModbus >= 0 && smartGridFromModbus <= 3) {
					try {
						// Set a value outside the the limit to disable smart grid.
						this.setSmartGridToModbus(10);
					} catch (OpenemsError.OpenemsNamedException e) {
						this.logError(this.log, "Unable to disable Smart Grid State.");
					}
				}
			}

			// Handle EnableSignal.
			boolean turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);

			// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
			int exceptionalStateValue = 0;
			if (this.useExceptionalState) {
				boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
				if (exceptionalStateActive) {
					exceptionalStateValue = this.getExceptionalStateValue();
					if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
						turnOnHeatpump = false;
					} else {
						turnOnHeatpump = true;
					}
				}
			}

			if (turnOnHeatpump) {

				// Make sure heat pump is not blocked or partially blocked.
				if (this.getBlockRelease().isDefined()) {
					if ((this.getBlockRelease().asEnum() == BlockRelease.RELEASE_2_COMPRESSORS) == false) {
						try {
							this.setBlockRelease(BlockRelease.RELEASE_2_COMPRESSORS.getValue());
						} catch (OpenemsError.OpenemsNamedException e) {
							this.logError(this.log, "Could not write to channel 'HR_6_BLOCK_RELEASE'. "
									+ "Reason: " + e.getMessage());
						}
					}
				} else {
					try {
						this.setBlockRelease(BlockRelease.RELEASE_2_COMPRESSORS.getValue());
					} catch (OpenemsError.OpenemsNamedException e) {
						this.logError(this.log, "Could not write to channel 'HR_6_BLOCK_RELEASE'. "
								+ "Reason: " + e.getMessage());
					}
				}

				// Set values for ’heat pump = on’
				heatingModeToModbus = this.heatingModeSetting.getValue();
				domesticHotWaterModeToModbus = this.domesticHotWaterModeSetting.getValue();
				mixingCircuit2ModeToModbus = this.mixingCircuit2ModeSetting.getValue();
				mixingCircuit3ModeToModbus = this.mixingCircuit3ModeSetting.getValue();
				coolingModeToModbus = this.coolingModeSetting.getValue();
				poolModeToModbus = this.poolModeSetting.getValue();
				ventilationModeToModbus = this.ventilationModeSetting.getValue();

				/*
				if (this.useExceptionalState) {
					// Currently not used. If exceptionalStateValue should do more than just on/off, code for that goes here.
					   ToDo: maybe use special settings for the operating modes in exceptional state? Right now it is
					    possible that all operating modes are set to ’off’ during exceptional state, meaning the pump
					    won't switch on. (Also possible for EnableSignal)
				}
				*/
			}
		}

		// Write operating modes.
		try {
			this.getHr7ModbusChannel().setNextWriteValue(heatingModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to heating mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr8ModbusChannel().setNextWriteValue(domesticHotWaterModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to domestic hot water mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr9ModbusChannel().setNextWriteValue(mixingCircuit2ModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to mixing circuit 2 mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr10ModbusChannel().setNextWriteValue(mixingCircuit3ModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to mixing circuit 3 mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr11ModbusChannel().setNextWriteValue(coolingModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to cooling mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr12ModbusChannel().setNextWriteValue(ventilationModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to ventilation mode channel. "
					+ "Reason: " + e.getMessage());
		}
		try {
			this.getHr13ModbusChannel().setNextWriteValue(poolModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to pool mode channel. "
					+ "Reason: " + e.getMessage());
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--Heat pump Alpha Innotec--");
		this.logInfo(this.log, "System State: " + this.getHeatpumpOperatingMode().asEnum().getName());
		this.logInfo(this.log, "Use Smart Grid State: " + this.getUseSmartGridState().get());
		this.logInfo(this.log, "Smart Grid State name: " + this.getSmartGridState().asEnum().getName());
		this.logInfo(this.log, "Block / release: " + this.getBlockRelease().asEnum().getName());
		this.logInfo(this.log, "Heating mode: " + this.getHeatingOperationMode().asEnum().getName());
		this.logInfo(this.log, "Domestic hot water mode: " + this.getDomesticHotWaterOperationMode().asEnum().getName());
		this.logInfo(this.log, "Cooling mode: " + this.getCoolingOperationMode().asEnum().getName());
		this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
		this.logInfo(this.log, "Flow temp circuit 1: " + this.getCircuit1FlowTemp());
		this.logInfo(this.log, "Flow temp circuit 1 setpoint: " + this.getCircuit1FlowTempSetpoint());
		this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
		this.logInfo(this.log, "Return temp setpoint: " + this.getReturnTempSetpoint());
		this.logInfo(this.log, "Outside temperature: " + this.getOutsideTemp());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
		this.logInfo(this.log, "");
	}

	/**
	 * Returns the debug message.
	 *
	 * @return the debug message.
	 */
	public String debugLog() {
		String debugMessage = this.getHeaterState().asEnum().asCamelCase() //
				+ "|F:" + this.getFlowTemperature().asString() //
				+ "|R:" + this.getReturnTemperature().asString(); //
		if (this.getErrorMessage().get().equals("No error") == false) {
			debugMessage = debugMessage + "|Error";
		}
		return debugMessage;
	}

	// Just for testing. Needs to be added to handleEvent() to work.
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
