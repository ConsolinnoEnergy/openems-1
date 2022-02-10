package io.openems.edge.heater.heatpump.tecalor;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
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
import io.openems.edge.heater.api.SmartGridState;
import io.openems.edge.heater.heatpump.tecalor.api.HeatpumpTecalor;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

import java.util.Optional;

import io.openems.edge.heater.heatpump.tecalor.api.OperatingMode;
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

/**
 * This module reads the most important variables available via Modbus from a Tecalor heat pump and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpTecalor, HeatpumpSmartGrid and Heater.
 * The heat pump is controlled either by EnableSignal & ExceptionalState or SmartGridState. The channel UseSmartGridState
 * determines the control mode.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.HeatPump.Tecalor",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})
public class HeatPumpTecalorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpTecalor {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(HeatPumpTecalorImpl.class);
	private boolean printInfoToLog;
	private boolean readOnly;
	private boolean connectionAlive;

	private OperatingMode defaultModeOfOperation;
	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_TECALOR_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "HEAT_PUMP_TECALOR_EXCEPTIONAL_STATE_IDENTIFIER";
	private static final int wpm3iIdentifier = 391;

	// This is essential for Modbus to work.
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpTecalorImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpTecalor.ChannelId.values(),
				HeatpumpSmartGrid.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
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

		// Settings needed when not in ’read only’ mode.
		if (this.readOnly == false) {
			// Use SmartGridState or EnableSignal & ExceptionalState to control heat pump.
			this._setUseSmartGridState(config.openEmsControlMode() == HeatpumpControlMode.SMART_GRID_STATE);
			this.getUseSmartGridStateChannel().nextProcessImage();

			this.defaultModeOfOperation = config.defaultModeOfOperation();
			if (this.defaultModeOfOperation == OperatingMode.UNDEFINED) {
				// It is possible to select UNDEFINED in config, but that makes no sense.
				this.defaultModeOfOperation = OperatingMode.ANTIFREEZE;
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
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol protocol = new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(506, Priority.HIGH,
						/* The pump sends 0x8000H (= signed -32768) when a value is not available. The
						   ElementToChannelConverter function is used to replace that value with "null", as this is
						   better for the visualization. */
						m(HeatpumpTecalor.ChannelId.IR507_OUTSIDE_TEMP, new SignedWordElement(506),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR508_TEMP_HC1, new SignedWordElement(507),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR509_TEMP_HC1_SETPOINT, new SignedWordElement(508),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR510_TEMP_HC1_SETPOINT, new SignedWordElement(509),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR511_TEMP_HC2, new SignedWordElement(510),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR512_TEMP_HC2_SETPOINT, new SignedWordElement(511),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR513_FLOW_TEMP_HEAT_PUMP, new SignedWordElement(512),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR514_FLOW_TEMP_AUX, new SignedWordElement(513),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(514),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(515),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR517_CONST_TEMP_SETPOINT, new SignedWordElement(516),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR518_STORAGE_TANK_TEMP, new SignedWordElement(517),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR519_STORAGE_TANK_TEMP_SETPOINT, new SignedWordElement(518),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR520_CIRCUIT_PRESSURE, new SignedWordElement(519),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR521_CIRCUIT_CURRENT, new SignedWordElement(520),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR522_WATER_TEMP, new SignedWordElement(521),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR523_WATER_TEMP_SETPOINT, new SignedWordElement(522),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR524_VENT_COOLING_TEMP, new SignedWordElement(523),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR525_VENT_COOLING_TEMP_SETPOINT, new SignedWordElement(524),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR526_SURFACE_COOLING_TEMP, new SignedWordElement(525),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H),
						m(HeatpumpTecalor.ChannelId.IR527_SURFACE_COOLING_TEMP_SETPOINT, new SignedWordElement(526),
								ElementToChannelConverter.REPLACE_WITH_NULL_IF_0X8000H)
				),
				new FC4ReadInputRegistersTask(2500, Priority.HIGH,
						m(HeatpumpTecalor.ChannelId.IR2501_STATUSBITS, new UnsignedWordElement(2500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR2502_DSM_SWITCH, new UnsignedWordElement(2501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(2502),
						m(HeatpumpTecalor.ChannelId.IR2504_ERRORSTATUS, new UnsignedWordElement(2503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR2505_BUSSTATUS, new UnsignedWordElement(2504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR2506_DEFROST, new UnsignedWordElement(2505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR2507_ERROR_CODE, new UnsignedWordElement(2506),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(3500, Priority.LOW,
						m(HeatpumpTecalor.ChannelId.IR3501_HEATPRODUCED_CIRCUIT_DAILY, new UnsignedWordElement(3500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3502_HEATPRODUCED_CIRCUIT_SUMKWH, new UnsignedWordElement(3501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3503_HEATPRODUCED_CIRCUIT_SUMMWH, new UnsignedWordElement(3502),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3504_HEATPRODUCED_WATER_DAILY, new UnsignedWordElement(3503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3505_HEATPRODUCED_WATER_SUMKWH, new UnsignedWordElement(3504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3506_HEATPRODUCED_WATER_SUMMWH, new UnsignedWordElement(3505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3507_HEATPRODUCED_AUX_SUMKWH, new UnsignedWordElement(3506),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3508_HEATPRODUCED_AUX_SUMMWH, new UnsignedWordElement(3507),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3509_HEATPRODUCED_WATER_AUX_SUMKWH, new UnsignedWordElement(3508),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3510_HEATPRODUCED_WATER_AUX_SUMMWH, new UnsignedWordElement(3509),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3511_CONSUMEDPOWER_CIRCUIT_DAILY, new UnsignedWordElement(3510),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3512_CONSUMEDPOWER_CIRCUIT_SUMKWH, new UnsignedWordElement(3511),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3513_CONSUMEDPOWER_CIRCUIT_SUMMWH, new UnsignedWordElement(3512),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3514_CONSUMEDPOWER_WATER_DAILY, new UnsignedWordElement(3513),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3515_CONSUMEDPOWER_WATER_SUMKWH, new UnsignedWordElement(3514),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR3516_CONSUMEDPOWER_WATER_SUMMWH, new UnsignedWordElement(3515),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(5000, Priority.HIGH,
						m(HeatpumpTecalor.ChannelId.IR5001_SGREADY_OPERATINGMODE, new UnsignedWordElement(5000),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.IR5002_CONTROLLER_MODEL_ID, new UnsignedWordElement(5001),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(1500, Priority.HIGH,
						m(HeatpumpTecalor.ChannelId.HR1501_OPERATING_MODE, new UnsignedWordElement(1500),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1502_COMFORT_TEMP_HC1, new SignedWordElement(1501),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1503_ECO_TEMP_HC1, new SignedWordElement(1502),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1504_SLOPE_HC1, new SignedWordElement(1503),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1505_COMFORT_TEMP_HC2, new SignedWordElement(1504),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1506_ECO_TEMP_HC2, new SignedWordElement(1505),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1507_SLOPE_HC2, new SignedWordElement(1506),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1508_CONST_TEMP_MODE, new SignedWordElement(1507),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1509_CIRCUIT_AUX_ACT_TEMP, new SignedWordElement(1508),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1510_COMFORT_TEMP_WATER, new SignedWordElement(1509),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1511_ECO_TEMP_WATER, new SignedWordElement(1510),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1512_WATER_STAGES, new SignedWordElement(1511),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1513_WATER_AUX_ACT_TEMP, new SignedWordElement(1512),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT, new SignedWordElement(1513),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1515_SURFACE_COOLING_FLOW_TEMP_HYST, new SignedWordElement(1514),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT, new SignedWordElement(1515),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT, new SignedWordElement(1516),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1518_VENT_COOLING_FLOW_TEMP_HYST, new SignedWordElement(1517),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT, new SignedWordElement(1518),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(4000, Priority.HIGH,
						m(HeatpumpTecalor.ChannelId.HR4001_SGREADY_ONOFF, new UnsignedWordElement(4000),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR4002_SGREADY_INPUT1, new UnsignedWordElement(4001),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpTecalor.ChannelId.HR4003_SGREADY_INPUT2, new UnsignedWordElement(4002),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(1500,
							m(HeatpumpTecalor.ChannelId.HR1501_MODBUS, new UnsignedWordElement(1500),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1502_COMFORT_TEMP_HC1, new SignedWordElement(1501),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1503_ECO_TEMP_HC1, new SignedWordElement(1502),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1504_SLOPE_HC1, new SignedWordElement(1503),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1505_COMFORT_TEMP_HC2, new SignedWordElement(1504),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1506_ECO_TEMP_HC2, new SignedWordElement(1505),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1507_SLOPE_HC2, new SignedWordElement(1506),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1508_CONST_TEMP_MODE, new SignedWordElement(1507),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1509_CIRCUIT_AUX_ACT_TEMP, new SignedWordElement(1508),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1510_COMFORT_TEMP_WATER, new SignedWordElement(1509),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1511_ECO_TEMP_WATER, new SignedWordElement(1510),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1512_WATER_STAGES, new UnsignedWordElement(1511),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1513_WATER_AUX_ACT_TEMP, new SignedWordElement(1512),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT, new SignedWordElement(1513),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1515_SURFACE_COOLING_FLOW_TEMP_HYST, new SignedWordElement(1514),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT, new SignedWordElement(1515),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT, new SignedWordElement(1516),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1518_VENT_COOLING_FLOW_TEMP_HYST, new SignedWordElement(1517),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT, new SignedWordElement(1518),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(4000,
							m(HeatpumpTecalor.ChannelId.HR4001_SGREADY_ONOFF, new UnsignedWordElement(4000),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR4002_SGREADY_INPUT1, new UnsignedWordElement(4001),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpTecalor.ChannelId.HR4003_SGREADY_INPUT2, new UnsignedWordElement(4002),
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
		} else if (this.readOnly == false && this.connectionAlive && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		// Map error to Heater interface ErrorMessage.
		boolean isError = false;
		String statusMessage = "";
		if (this.getErrorStatus().isDefined()) {
			this.connectionAlive = true;
			isError = this.getErrorStatus().get();
		} else {
			this.connectionAlive = false;
		}
		int errorCode = 0;
		if (this.getErrorCode().isDefined()) {
			errorCode = this.getErrorCode().get();
		}
		if (this.connectionAlive) {
			if (errorCode > 0) {
				this._setErrorMessage("Error Code: " + errorCode);
			} else if (isError) {
				this._setErrorMessage("An unknown error occurred.");
			} else {
				this._setErrorMessage("No error");
			}
		} else {
			statusMessage = "No modbus connection, ";
			this._setErrorMessage("No modbus connection.");
		}
		this.getErrorMessageChannel().nextProcessImage();

		// Map status to Heater interface HeaterState
		int statusBits = 0;
		boolean isRunning = false;
		if (this.getStatusBits().isDefined()) {
			statusBits = this.getStatusBits().get();
			if ((statusBits & 0b01) == 0b01) {
				statusMessage = statusMessage + "Pump circuit 1 (HK1 Pumpe), ";
			}
			if ((statusBits & 0b010) == 0b010) {
				statusMessage = statusMessage + "Pump circuit 2 (HK2 Pumpe), ";
			}
			boolean warmupProgram = (statusBits & 0b0100) == 0b0100;
			if (warmupProgram) {
				statusMessage = statusMessage + "Warmup program (Aufheizprogramm), ";
			}
			boolean auxActive = (statusBits & 0b01000) == 0b01000;
			if (auxActive) {
				statusMessage = statusMessage + "Auxiliary heaters active (NHZ Stufen in Betrieb), ";
			}
			boolean circuitHeatingActive = (statusBits & 0b010000) == 0b010000;
			if (circuitHeatingActive) {
				statusMessage = statusMessage + "Circuit heating (WP im Heizbetrieb), ";
			}
			boolean waterHeatingActive = (statusBits & 0b0100000) == 0b0100000;
			if (waterHeatingActive) {
				statusMessage = statusMessage + "Domestic hot water heating (WP im Warmwasserbetrieb), ";
			}
			boolean compressorActive = (statusBits & 0b01000000) == 0b01000000;
			if (compressorActive) {
				statusMessage = statusMessage + "Compressor active (Verdichter in Betrieb), ";
			}
			if ((statusBits & 0b010000000) == 0b010000000) {
				statusMessage = statusMessage + "Summer mode active (Sommerbetrieb aktiv), ";
			}
			boolean coolingActive = (statusBits & 0b0100000000) == 0b0100000000;
			if (coolingActive) {
				statusMessage = statusMessage + "Cooling mode active (Kuehlbetrieb aktiv), ";
			}
			if ((statusBits & 0b01000000000) == 0b01000000000) {
				statusMessage = statusMessage + "Defrost active (Min. eine IWS im Abtaubetrieb), ";
			}
			if ((statusBits & 0b010000000000) == 0b010000000000) {
				statusMessage = statusMessage + "Silentmode 1 active, ";
			}
			if ((statusBits & 0b0100000000000) == 0b0100000000000) {
				statusMessage = statusMessage + "Silentmode 2 active, ";
			}
			isRunning = warmupProgram || auxActive || circuitHeatingActive || waterHeatingActive || compressorActive || coolingActive;
		}

		boolean notBlocked = true;
		if (this.getDsmSwitch().isDefined()) {
			notBlocked = this.getDsmSwitch().get();
			if (notBlocked == false) {
				statusMessage = statusMessage + "DSM off state (EVU-Sperre), ";
			}
		}
		if (statusMessage.length() > 0) {
			statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
		}
		this._setStatusMessage(statusMessage);
		this.getStatusMessageChannel().nextProcessImage();

		if (isRunning) {
			this._setHeaterState(HeaterState.RUNNING.getValue());
		} else if (notBlocked && this.connectionAlive) {
			this._setHeaterState(HeaterState.STANDBY.getValue());
		} else if (notBlocked == false || isError) {
			this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
		} else {
			// You land here when no channel has data (’this.connectionAlive == false’, ’notBlocked == true’, ’isError == false’).
			this._setHeaterState(HeaterState.OFF.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();

		// Map HeatPumpSmartGrid "SmartGridState" read values.
		if (this.getSgReadyOperatingMode().isDefined()) {
			int generalizedSgState = this.getSgReadyOperatingMode().get();
			this._setSmartGridState(generalizedSgState);
		} else {
			this._setSmartGridState(SmartGridState.UNDEFINED.getValue());
		}
		this.getSmartGridStateChannel().nextProcessImage();

		// Map "getCircuit1SetpointTemp" according to WPM version.
		if (this.getControllerModelId().isDefined()) {
			int controllerModelId = getControllerModelId().get();
			if (controllerModelId == wpm3iIdentifier) {
				// WPM version is 3i
				if (this.channel(HeatpumpTecalor.ChannelId.IR509_TEMP_HC1_SETPOINT).value().isDefined()) {
					int setpointHk1 = (Integer) this.channel(HeatpumpTecalor.ChannelId.IR509_TEMP_HC1_SETPOINT).value().get();
					this._setCircuit1SetpointTemp(setpointHk1);
				}
			} else {
				// WPM version is not 3i
				if (this.channel(HeatpumpTecalor.ChannelId.IR510_TEMP_HC1_SETPOINT).value().isDefined()) {
					int setpointHk1 = (Integer) this.channel(HeatpumpTecalor.ChannelId.IR510_TEMP_HC1_SETPOINT).value().get();
					this._setCircuit1SetpointTemp(setpointHk1);
				}
			}
			this.getCircuit1SetpointTempChannel().nextProcessImage();
		}

		// Map energy channels that are transmitted as two modbus values.
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3502_HEATPRODUCED_CIRCUIT_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3503_HEATPRODUCED_CIRCUIT_SUMMWH),
				this.getProducedHeatCircuitTotalChannel());
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3505_HEATPRODUCED_WATER_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3506_HEATPRODUCED_WATER_SUMMWH),
				this.getProducedHeatWaterTotalChannel());
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3507_HEATPRODUCED_AUX_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3508_HEATPRODUCED_AUX_SUMMWH),
				this.getProducedHeatCircuitTotalAuxChannel());
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3509_HEATPRODUCED_WATER_AUX_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3510_HEATPRODUCED_WATER_AUX_SUMMWH),
				this.getProducedHeatWaterTotalAuxChannel());
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3512_CONSUMEDPOWER_CIRCUIT_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3513_CONSUMEDPOWER_CIRCUIT_SUMMWH),
				this.getConsumedPowerCircuitTotalChannel());
		this.combineSplitModbusRegisters(this.channel(HeatpumpTecalor.ChannelId.IR3515_CONSUMEDPOWER_WATER_SUMKWH),
				this.channel(HeatpumpTecalor.ChannelId.IR3516_CONSUMEDPOWER_WATER_SUMMWH),
				this.getConsumedPowerWaterTotalChannel());
	}

	/**
	 * This method combines two modbus registers into one channel.
	 * The Tecalor heat pump has the produced / consumed heating energy values split over two modbus registers, because
	 * the values can get too large for one register. The two 16 bit modbus registers are not combined to form one
	 * 32 bit register. Instead, the high value register counts the energy value in megawatt hours (MWh), rounding down.
	 * The low value register counts the energy in kilowatt hours (kWh), resetting to 0 if the counter goes over 999 kWh.
	 * The sum value (in kWh) is then 'kWh value + (MWh value * 1000)'.
	 *
	 * @param kwhChannel the kWh value IntegerReadChannel
	 * @param mwhChannel the MWh value IntegerReadChannel
	 * @param sumChannel the sum IntegerReadChannel
	 */
	protected void combineSplitModbusRegisters(IntegerReadChannel kwhChannel, IntegerReadChannel mwhChannel, IntegerReadChannel sumChannel) {
		boolean channelsHaveValues = kwhChannel.value().isDefined() && mwhChannel.value().isDefined();
		if (channelsHaveValues) {
			int sum = kwhChannel.value().get() + (mwhChannel.value().get() * 1000);
			sumChannel.setNextValue(sum);
			sumChannel.nextProcessImage();
		}
	}

	/**
	 * Determine commands and send them to the heater.
	 * The heat pump is controlled either by EnableSignal & ExceptionalState or SmartGridState. The channel
	 * UseSmartGridState determines the control mode.
	 * The operating mode channel get special treatment because it is used by EnableSignal/ExceptionalState to switch
	 * the heat pump on/off. The write of the operating mode channel is not mapped to Modbus. This is done by a duplicate
	 * ’private’ channel. The write to the ’public’ operating mode channel is stored in a local variable and sent to
	 * Modbus using the ’private’ channel.
	 * The benefit of this design is that when EnableSignal is false (heat pump off), writing to the operating mode
	 * channels is still registered and the value saved, but not executed. The changed operating mode is then applied
	 * once EnableSignal is true. This way you don't have to pay attention to the state of the heat pump when writing
	 * in the operating mode channel.
	 */
	protected void writeCommands() {
		// Collect OperatingMode channel ’nextWrite’.
		Optional<Integer> operatingModeOptional = this.getOperatingModeChannel().getNextWriteValueAndReset();
		if (operatingModeOptional.isPresent()) {
			int enumAsInt = operatingModeOptional.get();
			// Restrict to valid write values
			if (enumAsInt >= 0 && enumAsInt <= 7) {
				this.defaultModeOfOperation = OperatingMode.valueOf(enumAsInt);
			}
		}

		int operatingModeToModbus = OperatingMode.ANTIFREEZE.getValue();	// Used when turnOnHeatpump = false
		if (this.getUseSmartGridState().isDefined() && this.getUseSmartGridState().get()) {
			// Set operating mode.
			operatingModeToModbus = this.defaultModeOfOperation.getValue();

			// Map SG generalized "SmartGridState" write values.
			Optional<Integer> sgState = this.getSmartGridStateChannel().getNextWriteValueAndReset();
			if (sgState.isPresent()) {
				SmartGridState smartGridEnum = SmartGridState.valueOf(sgState.get());
				if (smartGridEnum != SmartGridState.UNDEFINED) {
					boolean sgInput1;
					boolean sgInput2;
					switch (smartGridEnum) {
						case SG1_BLOCKED:
							// Off
							sgInput1 = false;
							sgInput2 = true;
							break;
						case SG3_STANDARD:
							// Force on, increased temperature levels. (<- description from manual)
							sgInput1 = true;
							sgInput2 = false;
							break;
						case SG4_HIGH:
							// Force on, max temperature levels. (<- description from manual)
							sgInput1 = true;
							sgInput2 = true;
							break;
						case SG2_LOW:
						default:
							// Standard. (<- description from manual)
							sgInput1 = false;
							sgInput2 = false;
							break;
					}
					try {
						this.setSgReadyOnOff(true);
						this.getSgReadyInput1Channel().setNextWriteValue(sgInput1);
						this.getSgReadyInput2Channel().setNextWriteValue(sgInput2);
					} catch (OpenemsError.OpenemsNamedException e) {
						this.logError(this.log, "Could not write to sg ready channels. "
								+ "Reason: " + e.getMessage());
					}
				}
			}
		} else {

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
				operatingModeToModbus = this.defaultModeOfOperation.getValue();
				try {
					this.setSgReadyOnOff(false);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to operating mode channel. "
							+ "Reason: " + e.getMessage());
				}
					/* if (this.useExceptionalState) {
						// Currently not used. If exceptionalStateValue should do more than just on/off, code for that goes here.
					} */
			} else {
				try {
					this.setSgReadyOnOff(false);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to operating mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}

		try {
			this.getHr1501ModbusChannel().setNextWriteValue(operatingModeToModbus);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to operating mode channel. "
					+ "Reason: " + e.getMessage());
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--Heat pump Tecalor, model " + this.getControllerModelId() + "--");
		this.logInfo(this.log, "Heater state: " + this.getHeaterState());
		this.logInfo(this.log, "Flow temp: " + this.getFlowTemperature());
		this.logInfo(this.log, "Return temp: " + this.getReturnTemperature());
		this.logInfo(this.log, "Domestic hot water temp: " + this.getDomesticHotWaterTemp());
		this.logInfo(this.log, "Outside temp: " + this.getOutsideTemp());
		this.logInfo(this.log, "Storage tank temp: " + this.getStorageTankTemp());
		this.logInfo(this.log, "Circuit 1 temp: " + this.getCircuit1Temp());
		this.logInfo(this.log, "Circuit 2 temp: " + this.getCircuit2Temp());
		this.logInfo(this.log, "SmartGrid-Mode (Tecalor, 1-4): " + this.getSgReadyOperatingMode());
		this.logInfo(this.log, "Status message: " + this.getStatusMessage().get());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
		this.logInfo(this.log, "");
		this.logInfo(this.log, "--Writable Values--");
		this.logInfo(this.log, "Operating mode: " + this.getOperatingMode().asEnum().getName());
		this.logInfo(this.log, "SmartGrid-Ready active: " + this.getSgReadyOnOff());
		this.logInfo(this.log, "SmartGridState: " + this.getSmartGridState().asEnum().getName());
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

}
