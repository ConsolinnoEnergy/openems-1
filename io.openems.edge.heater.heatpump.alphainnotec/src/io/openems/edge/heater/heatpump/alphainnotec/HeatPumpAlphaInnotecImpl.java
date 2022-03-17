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
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.HeatpumpControlMode;
import io.openems.edge.heater.api.HeatpumpSmartGrid;
import io.openems.edge.heater.api.SmartGridState;
import io.openems.edge.heater.api.StartupCheckHandler;
import io.openems.edge.heater.heatpump.alphainnotec.api.BlockRelease;
import io.openems.edge.heater.heatpump.alphainnotec.api.CoolingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatpumpAlphaInnotec;
import io.openems.edge.heater.heatpump.alphainnotec.api.PoolMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.SystemStatus;
import io.openems.edge.heater.heatpump.alphainnotec.api.VentilationMode;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This module reads the most important variables available via Modbus from an Alpha Innotec heat pump and maps them to
 * OpenEMS channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpAlphaInnotecChannel, HeatpumpSmartGrid and Heater.
 */

@Designate(ocd = Config.class, factory = true)
@Component(
		name = "Heater.HeatPump.AlphaInnotec",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = {
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS
		})
public class HeatPumpAlphaInnotecImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpAlphaInnotec {

	@Reference
	protected ComponentManager cpm;

	@Reference
	protected ConfigurationAdmin ca;
	
	private final Logger log = LoggerFactory.getLogger(HeatPumpAlphaInnotecImpl.class);
	private boolean printInfoToLog;
	private boolean readOnly;
	private boolean startupStateChecked = false;
	private boolean readyForCommands;
	private boolean fullRemoteMode;

	private HeatingMode heatingModeSetting;
	private HeatingMode domesticHotWaterModeSetting;
	private HeatingMode mixingCircuit2ModeSetting;
	private HeatingMode mixingCircuit3ModeSetting;
	private CoolingMode coolingModeSetting;
	private PoolMode poolModeSetting;
	private VentilationMode ventilationModeSetting;

	private static final int RETURN_TEMP_SET_POINT_MIN = 150; // As per manual, unit is d°C.
	private static final int RETURN_TEMP_SET_POINT_MAX = 500; // Manual says 800, but pump won't accept anything higher than 500. Unit is d°C.

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
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.ca,
				"Modbus", config.modbus_id());

		this.printInfoToLog = config.printInfoToLog();
		this.readOnly = config.readOnly();
		this.fullRemoteMode = config.fullRemoteMode();
		this._setClearError(false);
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			// Use SmartGridState or EnableSignal & ExceptionalState to control heat pump.
			this._setUseSmartGridState(config.openEmsControlMode() == HeatpumpControlMode.SMART_GRID_STATE);
			this.getUseSmartGridStateChannel().nextProcessImage();

			this.startupStateChecked = false;

			// In case any control mode is set to UNDEFINED, change to a default and save value to config.
			Map<String, Object> keyValueMap = new HashMap<>();
			this.heatingModeSetting = config.defaultHeatingMode();
			if (this.heatingModeSetting == HeatingMode.UNDEFINED) {
				keyValueMap.put("defaultHeatingMode", HeatingMode.AUTOMATIC);
			}
			this.domesticHotWaterModeSetting = config.defaultDomesticHotWaterMode();
			if (this.domesticHotWaterModeSetting == HeatingMode.UNDEFINED) {
				keyValueMap.put("defaultDomesticHotWaterMode", HeatingMode.OFF);
			}
			this.mixingCircuit2ModeSetting = config.defaultMixingCircuit2Mode();
			if (this.mixingCircuit2ModeSetting == HeatingMode.UNDEFINED) {
				keyValueMap.put("defaultMixingCircuit2Mode", HeatingMode.OFF);
			}
			this.mixingCircuit3ModeSetting = config.defaultMixingCircuit3Mode();
			if (this.mixingCircuit3ModeSetting == HeatingMode.UNDEFINED) {
				keyValueMap.put("defaultMixingCircuit3Mode", HeatingMode.OFF);
			}
			this.coolingModeSetting = config.defaultCoolingMode();
			if (this.coolingModeSetting == CoolingMode.UNDEFINED) {
				keyValueMap.put("defaultCoolingMode", CoolingMode.OFF);
			}
			this.poolModeSetting = config.defaultPoolMode();
			if (this.poolModeSetting == PoolMode.UNDEFINED) {
				keyValueMap.put("defaultPoolMode", PoolMode.OFF);
			}
			this.ventilationModeSetting = config.defaultVentilationMode();
			if (this.ventilationModeSetting == VentilationMode.UNDEFINED) {
				keyValueMap.put("defaultVentilationMode", VentilationMode.OFF);
			}
			if (keyValueMap.isEmpty() == false) { // Updating config restarts the module.
				try {
					ConfigurationUpdate.updateConfig(ca, this.servicePid(), keyValueMap);
				} catch (IOException e) {
					this.log.warn("Couldn't save new settings to config. " + e.getMessage());
				}
			}

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
							m(HeatpumpAlphaInnotec.ChannelId.HR_1_MODBUS, new UnsignedWordElement(1),
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
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
				this.channelmapping();
				if (this.printInfoToLog) {
					if (this.fullRemoteMode) {
						this.printInfoRemote();
					} else {
						this.printInfo();
					}
				}
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
				if (this.readOnly == false && this.readyForCommands) {
					if (this.fullRemoteMode) {
						this.writeCommandsRemote();
					} else {
						this.writeCommands();
					}
				}
				break;
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		if (this.getHeatpumpOperatingMode().isDefined()) {
			this.readyForCommands = true;
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
			this.readyForCommands = false;
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

			// ToDo: Manually write a value in IR_46_ERROR to indicate Modbus is not connected?
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
	 * Also, write values are saved to config so that an OpenEMS restart does not disrupt operation. In case of a
	 * restart, the module will load the value from the config and immediately return to its previous state.
	 */
	protected void writeCommands() {

		// Collect operating mode channels ’nextWrite’.
		boolean updateConfig = false;
		Map<String, Object> keyValueMap = new HashMap<>();
		Optional<Integer> heatingModeOptional = this.getHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (heatingModeOptional.isPresent()) {
			int enumAsInt = heatingModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != this.heatingModeSetting.getValue()) {
				this.heatingModeSetting = HeatingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultHeatingMode", this.heatingModeSetting);
			}
		}
		Optional<Integer> domesticHotWaterModeOptional = this.getDomesticHotWaterOperationModeChannel().getNextWriteValueAndReset();
		if (domesticHotWaterModeOptional.isPresent()) {
			int enumAsInt = domesticHotWaterModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != this.domesticHotWaterModeSetting.getValue()) {
				this.domesticHotWaterModeSetting = HeatingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultDomesticHotWaterMode", this.domesticHotWaterModeSetting);
			}
		}
		Optional<Integer> mixingCircuit2ModeOptional = this.getCircuit2OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit2ModeOptional.isPresent()) {
			int enumAsInt = mixingCircuit2ModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != this.mixingCircuit2ModeSetting.getValue()) {
				this.mixingCircuit2ModeSetting = HeatingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultMixingCircuit2Mode", this.mixingCircuit2ModeSetting);
			}
		}
		Optional<Integer> mixingCircuit3ModeOptional = this.getCircuit3OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit3ModeOptional.isPresent()) {
			int enumAsInt = mixingCircuit3ModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != this.mixingCircuit3ModeSetting.getValue()) {
				this.mixingCircuit3ModeSetting = HeatingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultMixingCircuit3Mode", this.mixingCircuit3ModeSetting);
			}
		}
		Optional<Integer> coolingModeOptional = this.getCoolingOperationModeChannel().getNextWriteValueAndReset();
		if (coolingModeOptional.isPresent()) {
			int enumAsInt = coolingModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 1 && enumAsInt != this.coolingModeSetting.getValue()) {
				this.coolingModeSetting = CoolingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultCoolingMode", this.coolingModeSetting);
			}
		}
		Optional<Integer> poolModeOptional = this.getPoolHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (poolModeOptional.isPresent()) {
			int enumAsInt = poolModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 4 && enumAsInt != 1 && enumAsInt != this.poolModeSetting.getValue()) {
				this.poolModeSetting = PoolMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultPoolMode", this.poolModeSetting);
			}
		}
		Optional<Integer> ventilationModeOptional = this.getVentilationOperationModeChannel().getNextWriteValueAndReset();
		if (ventilationModeOptional.isPresent()) {
			int enumAsInt = ventilationModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 3 && enumAsInt != this.ventilationModeSetting.getValue()) {
				this.ventilationModeSetting = VentilationMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultVentilationMode", this.ventilationModeSetting);
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

			// Check heater state at startup. Avoid turning off heater just because EnableSignal initial value is ’false’.
			if (this.startupStateChecked == false) {
				this.startupStateChecked = true;
				turnOnHeatpump = StartupCheckHandler.deviceAlreadyHeating(this, this.log);
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

				// Set values for ’heat pump = on’. Note: All these values can be ’off’ and the heatpump won't switch on.
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

		/* Sanitize input for channel HR_1_RETURN_TEMP_SETPOINT. If you write a value that is outside the valid range,
		   it writes 350 instead. Don't want that behaviour, better to cap it to min or max value if outside the range. */
		Optional<Integer> returnTempSetpointOptional = this.getReturnTempSetpointChannel().getNextWriteValueAndReset();
		if (returnTempSetpointOptional.isPresent()) {
			int writeToModbus = returnTempSetpointOptional.get();
			writeToModbus = TypeUtils.fitWithin(RETURN_TEMP_SET_POINT_MIN, RETURN_TEMP_SET_POINT_MAX, writeToModbus);
			try {
				this.getHr1ModbusChannel().setNextWriteValue(writeToModbus);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Could not write to return temp set point channel. Reason: " + e.getMessage());
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

		// Forward error reset write to modbus.
		Optional<Boolean> errorResetOptional = this.getClearErrorChannel().getNextWriteValueAndReset();
		if (errorResetOptional.isPresent()) {
			boolean errorResetWrite = errorResetOptional.get();
			if (errorResetWrite) {
				try {
					// Send ’clear error’ once to Modbus.
					this.getCoil0ModbusChannel().setNextWriteValue(true);
				} catch (OpenemsError.OpenemsNamedException e) {
					e.printStackTrace();
				}
			}
		}

		// Updating config restarts the module.
		if (updateConfig) {
			try {
				ConfigurationUpdate.updateConfig(ca, this.servicePid(), keyValueMap);
			} catch (IOException e) {
				this.log.warn("Couldn't save new settings to config. " + e.getMessage());
			}
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
	 * Information that is printed to the log if ’print info to log’ option is enabled. For full remote mode.
	 */
	protected void printInfoRemote() {
		this.logInfo(this.log, "--Heat pump Alpha Innotec, Full Remote mode--");
		this.logInfo(this.log, "HR_6_BLOCK_RELEASE: " + this.getBlockRelease().asEnum().getName());
		this.logInfo(this.log, "DI_0_DMS_BLOCK: " + getDmsBlockActive());
		this.logInfo(this.log, "DI_1_DMS_BLOCK_SG: " + getEvu2Active());
		this.logInfo(this.log, "DI_3_COMPRESSOR1: " + getVD1active());
		this.logInfo(this.log, "DI_4_COMPRESSOR2: " + getVD1active());
		this.logInfo(this.log, "DI_5_AUX1: " + getZwe1Active());
		this.logInfo(this.log, "DI_6_AUX2: " + getZwe2Active());
		this.logInfo(this.log, "COIL_2_HUP: " + getForceOnHup());
		this.logInfo(this.log, "COIL_4_ZUP: " + getForceOnZup());
		this.logInfo(this.log, "COIL_7_ZIP: " + getForceOnZip());
		this.logInfo(this.log, "COIL_8_FUP2: " + getForceOnFup2());
		this.logInfo(this.log, "COIL_9_FUP3: " + getForceOnFup3());
		this.logInfo(this.log, "COIL_13_FRH: " + getForceOnFrh());
		this.logInfo(this.log, "HR_7_CIRCUIT_HEATING_OPERATION_MODE: " + this.getHeatingOperationMode().asEnum().getName());
		this.logInfo(this.log, "HR_8_WATER_OPERATION_MODE: " + this.getDomesticHotWaterOperationMode().asEnum().getName());
		this.logInfo(this.log, "HR_11_COOLING_OPERATION_MODE: " + this.getCoolingOperationMode().asEnum().getName());
		this.logInfo(this.log, "HR_14_SMART_GRID: " + this.getSmartGridFromModbus());
		this.logInfo(this.log, "FLOW_TEMPERATURE: " + this.getFlowTemperature());
		this.logInfo(this.log, "HR_0_OUTSIDETEMP: " + this.getOutsideTemp());
		this.logInfo(this.log, "IR_5_FLOW_TEMP_MC1: " + this.getCircuit1FlowTemp());
		this.logInfo(this.log, "HR_2_FLOW_TEMP_SETPOINT_MC1: " + this.getCircuit1FlowTempSetpoint());
		this.logInfo(this.log, "IR_6_FLOW_TEMP_MC2: " + this.getCircuit2FlowTemp());
		this.logInfo(this.log, "HR_3_FLOW_TEMP_SETPOINT_MC2: " + this.getCircuit2FlowTempSetpoint());
		this.logInfo(this.log, "IR_7_FLOW_TEMP_MC3: " + this.getCircuit3FlowTemp());
		this.logInfo(this.log, "HR_4_FLOW_TEMP_SETPOINT_MC3: " + this.getCircuit3FlowTempSetpoint());
		this.logInfo(this.log, "IR_18_EXTRACT_AIR_TEMP: " + getExtractAirTemp());
		this.logInfo(this.log, "IR_0_AVERAGE_TEMP: " + getAverageTemp());
		this.logInfo(this.log, "HR_23_TEMP_PM: " + getTempPlusMinus());
		this.logInfo(this.log, "RETURN_TEMPERATURE: " + this.getReturnTemperature());
		this.logInfo(this.log, "HR_1_RETURN_TEMP_SETPOINT: " + this.getReturnTempSetpoint());
		this.logInfo(this.log, "IR_9_HEAT_SOURCE_INLET_TEMP: " + getHeatSourceInletTemp());
		this.logInfo(this.log, "IR_10_HEAT_SOURCE_OUTLET_TEMP: " + getHeatSourceOutletTemp());
		this.logInfo(this.log, "IR_4_WATER_TEMP: " + getDomesticHotWaterTemp());
		this.logInfo(this.log, "HR_5_WATER_TEMP_SETPOINT: " + getDomesticHotWaterTempSetpoint());
		this.logInfo(this.log, "IR_24_RBE_ROOM_TEMP: " + getRbeRoomTempActual());
		this.logInfo(this.log, "IR_25_RBE_ROOM_TEMP_SETPOINT: " + getRbeRoomTempSetpoint());
		this.logInfo(this.log, "IR_44_ENERGY_TOTAL: " + getHeatAmountAll());
		this.logInfo(this.log, "IR_40_ENERGY_WATER: " + getHeatAmountDomesticHotWater());
		this.logInfo(this.log, "IR_38_ENERGY_CIRCUIT_HEATING: " + getHeatAmountHeating());
		this.logInfo(this.log, "IR_33_HOURS_HEAT_PUMP: " + getHoursHeatPump());
		this.logInfo(this.log, "IR_34_HOURS_CIRCUIT_HEATING: " + getHoursHeating());
		this.logInfo(this.log, "IR_35_HOURS_WATER_HEATING: " + getHoursDomesticHotWater());
		this.logInfo(this.log, "IR_28_HOURS_COMP1: " + getHoursVD1());
		this.logInfo(this.log, "IR_29_HOURS_COMP2: " + getHoursVD2());
		this.logInfo(this.log, "IR_30_HOURS_AUX1: " + getHoursZwe1());
		this.logInfo(this.log, "IR_31_HOURS_AUX2: " + getHoursZwe2());
		this.logInfo(this.log, "HR_15_CURVE_CIRCUIT_HEATING_END_POINT: " + getHeatingCurveEndPoint());
		this.logInfo(this.log, "HR_16_CURVE_CIRCUIT_HEATING_SHIFT: " + getHeatingCurveParallelShift());
		this.logInfo(this.log, "HR_17_CURVE_MC1_END_POINT: " + getHeatingCurveCircuit1EndPoint());
		this.logInfo(this.log, "HR_18_CURVE_MC1_SHIFT: " + getHeatingCurveCircuit1ParallelShift());
		this.logInfo(this.log, "HR_19_CURVE_MC2_END_POINT: " + getHeatingCurveCircuit2EndPoint());
		this.logInfo(this.log, "HR_20_CURVE_MC2_SHIFT: " + getHeatingCurveCircuit2ParallelShift());
		this.logInfo(this.log, "HR_21_CURVE_MC3_END_POINT: " + getHeatingCurveCircuit3EndPoint());
		this.logInfo(this.log, "HR_22_CURVE_MC3_SHIFT: " + getHeatingCurveCircuit3ParallelShift());
		this.logInfo(this.log, "IR_46_ERROR: " + this.getErrorCode());
		this.logInfo(this.log, "COIL_0_ERRORRESET: " + this.getClearError());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage());
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

	protected void writeCommandsRemote() {
		// Error reset special treatment:
		// If channel COIL_0_ERRORRESET contains ’false’, you can write ’true’ in nextWrite to execute an error reset.
		// The value of channel COIL_0_ERRORRESET will then switch to ’true’, to indicate the error reset was executed.
		// To do another error reset, you first have to write ’false’ in nextWrite of COIL_0_ERRORRESET which changes
		// the channel value back to ’false’.
		// In short: an error reset is executed when the channel value changes from ’false’ to ’true’. The channel value
		// does not change back to ’false’ by itself.
		Optional<Boolean> errorResetOptional = this.getClearErrorChannel().getNextWriteValueAndReset();
		if (errorResetOptional.isPresent()) {
			boolean errorResetWrite = errorResetOptional.get();
			boolean currentChannelValue = this.getClearError().get();
			if (currentChannelValue == false && errorResetWrite) {
				try {
					// Send ’clear error’ once to Modbus.
					this.getCoil0ModbusChannel().setNextWriteValue(true);
					this._setClearError(true);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to channel COIL_0_MODBUS. Reason: " + e.getMessage());
				}
			} else if (errorResetWrite == false) {
				this._setClearError(false);
			}
		}

		/* Sanitize input for channel HR_1_RETURN_TEMP_SETPOINT. If you write a value that is outside the valid range,
		   it writes 350 instead. Don't want that behaviour, better to cap it to min or max value if outside the range. */
		Optional<Integer> returnTempSetpointOptional = this.getReturnTempSetpointChannel().getNextWriteValueAndReset();
		if (returnTempSetpointOptional.isPresent()) {
			int writeToModbus = returnTempSetpointOptional.get();
			writeToModbus = TypeUtils.fitWithin(RETURN_TEMP_SET_POINT_MIN, RETURN_TEMP_SET_POINT_MAX, writeToModbus);
			try {
				this.getHr1ModbusChannel().setNextWriteValue(writeToModbus);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.logError(this.log, "Could not write to return temp set point channel. Reason: " + e.getMessage());
			}
		}

		// Collect operating mode channels ’nextWrite’.
		Optional<Integer> heatingModeOptional = this.getHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (heatingModeOptional.isPresent()) {
			int writeToModbus = heatingModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 4) {
				try {
					this.getHr7ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to heating mode channel. Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> domesticHotWaterModeOptional = this.getDomesticHotWaterOperationModeChannel().getNextWriteValueAndReset();
		if (domesticHotWaterModeOptional.isPresent()) {
			int writeToModbus = domesticHotWaterModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 4) {
				try {
					this.getHr8ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to domestic hot water mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> mixingCircuit2ModeOptional = this.getCircuit2OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit2ModeOptional.isPresent()) {
			int writeToModbus = mixingCircuit2ModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 4) {
				try {
					this.getHr9ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to mixing circuit 2 mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> mixingCircuit3ModeOptional = this.getCircuit3OperationModeChannel().getNextWriteValueAndReset();
		if (mixingCircuit3ModeOptional.isPresent()) {
			int writeToModbus = mixingCircuit3ModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 4) {
				try {
					this.getHr10ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to mixing circuit 3 mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> coolingModeOptional = this.getCoolingOperationModeChannel().getNextWriteValueAndReset();
		if (coolingModeOptional.isPresent()) {
			int writeToModbus = coolingModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 1) {
				try {
					this.getHr11ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to cooling mode channel. Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> poolModeOptional = this.getPoolHeatingOperationModeChannel().getNextWriteValueAndReset();
		if (poolModeOptional.isPresent()) {
			int writeToModbus = poolModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 4 && writeToModbus != 1) {
				try {
					this.getHr12ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to ventilation mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}
		Optional<Integer> ventilationModeOptional = this.getVentilationOperationModeChannel().getNextWriteValueAndReset();
		if (ventilationModeOptional.isPresent()) {
			int writeToModbus = ventilationModeOptional.get();
			// Restrict to valid write values
			if (writeToModbus >= 0 && writeToModbus <= 3) {
				try {
					this.getHr13ModbusChannel().setNextWriteValue(writeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to pool mode channel. Reason: " + e.getMessage());
				}
			}
		}
	}
}
