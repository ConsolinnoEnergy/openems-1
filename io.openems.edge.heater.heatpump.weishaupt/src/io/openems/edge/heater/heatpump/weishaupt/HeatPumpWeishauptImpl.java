package io.openems.edge.heater.heatpump.weishaupt;

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
import io.openems.edge.heater.api.StartupCheckHandler;
import io.openems.edge.heater.heatpump.weishaupt.api.FlowTempRegulationMode;
import io.openems.edge.heater.heatpump.weishaupt.api.HeatpumpWeishaupt;
import io.openems.edge.heater.heatpump.weishaupt.api.OperatingMode;
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
 * This module reads the most important variables available via Modbus from a Weishaupt heat pump and maps them to
 * OpenEMS channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpWeishaupt and Heater.
 * Since the heat pump does not have an off switch available via Modbus, EnableSignal uses OperatingMode.THROTTLING as
 * the off state.
 *
 * ToDo: Add SmartGrid functionality, if possible.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.HeatPump.Weishaupt",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})

public class HeatPumpWeishauptImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeatpumpWeishaupt {

	@Reference
	protected ComponentManager cpm;

	@Reference
	protected ConfigurationAdmin ca;

	private final Logger log = LoggerFactory.getLogger(HeatPumpWeishauptImpl.class);
	private boolean printInfoToLog;
	private boolean readOnly;
	private boolean startupStateChecked = false;
	private boolean connectionAlive;
	private OperatingMode modeOfOperation;
	private FlowTempRegulationMode flowTempRegulationMode;
	private int flowTempSetpoint;
	private static final int TEMPERATURE_SET_POINT_MAX = 600;
	private static final int TEMPERATURE_SET_POINT_MIN = 180;

	private boolean useEnableSignal;
	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_HELIOTHERM_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "HEAT_PUMP_HELIOTHERM_EXCEPTIONAL_STATE_IDENTIFIER";

	// This is essential for Modbus to work.
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpWeishauptImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpWeishaupt.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.ca,
				"Modbus", config.modbusBridgeId());
		this.printInfoToLog = config.printInfoToLog();
		this.readOnly = config.readOnly();
		if (this.readOnly == false) {
			this.startupStateChecked = false;

			// Check if values are valid. If not, update config with a valid entry.
			Map<String, Object> keyValueMap = new HashMap<>();
			this.modeOfOperation = config.defaultModeOfOperation();
			if (this.modeOfOperation == OperatingMode.UNDEFINED) {
				this.modeOfOperation = OperatingMode.AUTOMATIC;
				keyValueMap.put("defaultModeOfOperation", this.modeOfOperation);
			}
			this.flowTempRegulationMode = config.defaultFlowTempRegulationMode();
			if (this.flowTempRegulationMode == FlowTempRegulationMode.UNDEFINED) {
				this.flowTempRegulationMode = FlowTempRegulationMode.OUTSIDE_TEMP;
				keyValueMap.put("defaultFlowTempRegulationMode", this.flowTempRegulationMode);
			}
			this.flowTempSetpoint = config.defaultFlowTempSetpoint() * 10; // Convert to d°C.
			this.flowTempSetpoint = TypeUtils.fitWithin(TEMPERATURE_SET_POINT_MIN, TEMPERATURE_SET_POINT_MAX, this.flowTempSetpoint);
			if (this.flowTempSetpoint != config.defaultFlowTempSetpoint() * 10) {
				keyValueMap.put("defaultFlowTempSetpoint", this.flowTempSetpoint / 10);
			}

			if (keyValueMap.isEmpty() == false) { // Updating config restarts the module.
				try {
					ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), keyValueMap);
				} catch (IOException e) {
					this.log.warn("Couldn't save new settings to config. " + e.getMessage());
				}
			}

			this.useEnableSignal = config.useEnableSignal();
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
				new FC4ReadInputRegistersTask(1, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.IR1_OUTSIDE_TEMP, new SignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.IR3_DOMESTIC_HOT_WATER, new SignedWordElement(3),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(4),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(5),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(72, Priority.LOW,
						m(HeatpumpWeishaupt.ChannelId.IR72_OPERATING_HOURS_COMPRESSOR1, new UnsignedWordElement(72),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.IR73_OPERATING_HOURS_COMPRESSOR2, new UnsignedWordElement(73),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(103, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.IR103_STATUS_CODE, new UnsignedWordElement(103),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.IR104_BLOCKED_CODE, new UnsignedWordElement(104),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.IR105_ERROR_CODE, new UnsignedWordElement(105),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(46, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.HR46_ROOM_TEMPERATURE_SET_POINT, new UnsignedWordElement(46),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(222, Priority.HIGH,
				m(HeatpumpWeishaupt.ChannelId.HR222_OPERATING_MODE, new UnsignedWordElement(222),
						ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(244, Priority.HIGH,
						m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(244),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(247, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.HR247_FLOW_TEMP_REGULATION_MODE, new UnsignedWordElement(247),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(254, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.HR254_DOMESTIC_HOT_WATER_SET_POINT, new UnsignedWordElement(254),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT, new UnsignedWordElement(255),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(46,
							m(HeatpumpWeishaupt.ChannelId.HR46_ROOM_TEMPERATURE_SET_POINT, new UnsignedWordElement(46),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(222,
							m(HeatpumpWeishaupt.ChannelId.HR222_MODBUS, new UnsignedWordElement(222),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(244,
							m(HeatpumpWeishaupt.ChannelId.HR244_MODBUS, new UnsignedWordElement(244),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(247,
							m(HeatpumpWeishaupt.ChannelId.HR247_MODBUS, new UnsignedWordElement(247),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(254,
							m(HeatpumpWeishaupt.ChannelId.HR254_DOMESTIC_HOT_WATER_SET_POINT, new UnsignedWordElement(254),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeatpumpWeishaupt.ChannelId.HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT, new UnsignedWordElement(255),
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
					this.printInfo();
				}
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
				if (this.readOnly == false && this.connectionAlive) {
					this.writeCommands();
				}
				break;
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		// Evaluate error indicator
		boolean heatpumpError = false;
		if (this.getErrorCode().isDefined()) {
			this.connectionAlive = true;
			int errorCode = getErrorCode().get();
			if (errorCode > 0) {
				heatpumpError = true;
				this._setErrorMessage("Error code " + errorCode + ".");
			} else {
				this._setErrorMessage("No error");
			}
		} else {
			this.connectionAlive = false;
			this._setErrorMessage("No Modbus connection.");
		}
		this.getErrorMessageChannel().nextProcessImage();

		// Parse status code (HR103).
		boolean offIndicator = false;
		boolean blockedIndicator = false;
		boolean runningIndicator = false;
		if (this.getStatusCode().isDefined()) {
			int statusCode = this.getStatusCode().get();
			String statusMessage = "Status code " + statusCode + ": ";
			switch (statusCode) {
				case 0:
				case 1:
					offIndicator = true;
					statusMessage = statusMessage + "Off";
					break;
				case 2:
					runningIndicator = true;
					statusMessage = statusMessage + "Heating";
					break;
				case 3:
					runningIndicator = true;
					statusMessage = statusMessage + "Swimming pool";
					break;
				case 4:
					runningIndicator = true;
					statusMessage = statusMessage + "Domestic hot water";
					break;
				case 5:
					runningIndicator = true;
					statusMessage = statusMessage + "Cooling";
					break;
				case 10:
					statusMessage = statusMessage + "Defrost";
					break;
				case 11:
					statusMessage = statusMessage + "Monitoring percolation";
					break;
				case 24:
					statusMessage = statusMessage + "Delayed operating mode switch (Verzoegerte Betriebsmodusumschaltung)";
					break;
				case 30:
					statusMessage = statusMessage + "Blocked";
					break;
				default:
					statusMessage = statusMessage + "Not in list";
					break;
			}
			if (getBlockedCode().isDefined()) {
				int blockedCode = this.getBlockedCode().get();
				if (blockedCode > 0) {
					blockedIndicator = true;
					statusMessage = statusMessage + ", blocked code " + blockedCode + ".";
				}
			}
			this._setStatusMessage(statusMessage);
		} else {
			this._setStatusMessage("No Modbus connection");
		}
		this.getStatusMessageChannel().nextProcessImage();

		// Set Heater interface STATUS channel
		if (this.connectionAlive) {
			if (runningIndicator) {
				this._setHeaterState(HeaterState.RUNNING.getValue());
			} else if (blockedIndicator || heatpumpError) {
				this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
			} else if (offIndicator) {	// Maybe this should be "standby" instead of "off".
				this._setHeaterState(HeaterState.OFF.getValue());
			} else {
				this._setHeaterState(HeaterState.UNDEFINED.getValue());
			}
		} else {
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 */
	protected void writeCommands() {

		// Collect flow temperature set point channel ’nextWrite’.
		Optional<Integer> flowTempSetpointOptional = this.getTemperatureSetpointChannel().getNextWriteValueAndReset();
		if (flowTempSetpointOptional.isPresent()) {
			this.flowTempSetpoint = flowTempSetpointOptional.get();
			this.flowTempSetpoint = TypeUtils.fitWithin(TEMPERATURE_SET_POINT_MIN, TEMPERATURE_SET_POINT_MAX, this.flowTempSetpoint);

		}
		if (this.getTemperatureSetpoint().isDefined()) {	// Check flow temperature set point, send write if it is not what it should be.
			int currentTemperatureSetpoint = this.getTemperatureSetpoint().get();
			if (currentTemperatureSetpoint != this.flowTempSetpoint) {
				try {
					this.getHr244ModbusChannel().setNextWriteValue(this.flowTempSetpoint);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to temperature set point channel. Reason: " + e.getMessage());
				}
			}
		}

		// Collect flow temperature regulation mode channel ’nextWrite’.
		boolean updateConfig = false;
		Map<String, Object> keyValueMap = new HashMap<>();
		if (this.getFlowTempRegulationMode().isDefined()) {
			Optional<Integer> flowTempRegulationModeOptional = this.getFlowTempRegulationModeChannel().getNextWriteValueAndReset();
			if (flowTempRegulationModeOptional.isPresent()) {
				int enumAsInt = flowTempRegulationModeOptional.get();
				// Restrict to valid write values, check if new value is different.
				if (enumAsInt >= 0 && enumAsInt <= 2 && enumAsInt != this.getFlowTempRegulationMode().get()) {
					this.flowTempRegulationMode = FlowTempRegulationMode.valueOf(enumAsInt);

					// Save setting to config, so setting does not go back to default on restart.
					updateConfig = true;
					keyValueMap.put("defaultFlowTempRegulationMode", this.flowTempRegulationMode);
				}
			}

			// Check flow temperature regulation mode, send write if it is not what it should be.
			FlowTempRegulationMode currentFlowTempRegulationMode = this.getFlowTempRegulationMode().asEnum();
			if (currentFlowTempRegulationMode != this.flowTempRegulationMode) {
				try {
					this.getHr247ModbusChannel().setNextWriteValue(this.flowTempRegulationMode.getValue());
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to flow temperature regulation mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}

		// Collect operating mode channel ’nextWrite’.
		Optional<Integer> operatingModeOptional = this.getOperatingModeChannel().getNextWriteValueAndReset();
		if (operatingModeOptional.isPresent()) {
			int enumAsInt = operatingModeOptional.get();
			// Restrict to valid write values, check if new value is different.
			if (enumAsInt >= 0 && enumAsInt <= 5 && enumAsInt != this.getOperatingMode().get()) {
				this.modeOfOperation = OperatingMode.valueOf(enumAsInt);

				// Save setting to config, so setting does not go back to default on restart.
				updateConfig = true;
				keyValueMap.put("defaultModeOfOperation", this.modeOfOperation);
			}
		}
		int operatingModeToModbus = this.modeOfOperation.getValue();

		if (this.useEnableSignal) {

			// Handle EnableSignal.
			boolean turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);

			// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
			if (this.useExceptionalState) {
				boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
				if (exceptionalStateActive) {
					int exceptionalStateValue = this.getExceptionalStateValue();
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

			// Turn on heater when enableSignal == true.
			if (turnOnHeatpump) {
				operatingModeToModbus = OperatingMode.AUTOMATIC.getValue();
			} else {
				// The heat pump does not have an off switch. This is the closest thing to off.
				operatingModeToModbus = OperatingMode.THROTTLING.getValue();
			}
		}

		// Check operating mode, send write if it is not what it should be.
		if (this.getOperatingMode().isDefined()) {
			OperatingMode currentOperatingMode = this.getOperatingMode().asEnum();
			if (currentOperatingMode != OperatingMode.valueOf(operatingModeToModbus)) {
				try {
					this.getHr222ModbusChannel().setNextWriteValue(operatingModeToModbus);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.logError(this.log, "Could not write to operating mode channel. "
							+ "Reason: " + e.getMessage());
				}
			}
		}

		// Updating config restarts the module.
		if (updateConfig) {
			try {
				ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), keyValueMap);
			} catch (IOException e) {
				this.log.warn("Couldn't save new settings to config. " + e.getMessage());
			}
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--Heat pump Weishaupt--");
		this.logInfo(this.log, "Outside temp: " + this.getOutsideTemp());
		this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
		this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
		this.logInfo(this.log, "Domestic hot water temp: " + this.getDomesticHotWaterTemp());
		this.logInfo(this.log, "Operating mode: " + this.getOperatingMode().asEnum().getName());
		this.logInfo(this.log, "Flow temp regulation mode: " + this.getFlowTempRegulationMode().asEnum().getName());
		if (this.getFlowTempRegulationMode().asEnum() == FlowTempRegulationMode.MANUAL) {
			this.logInfo(this.log, "Flow temperature set point: " + this.getTemperatureSetpoint());
		}
		this.logInfo(this.log, "Blocked code: " + this.getBlockedCode());
		this.logInfo(this.log, "Error code: " + this.getErrorCode());
		this.logInfo(this.log, "Status message: " + this.getStatusMessage());
	}

}
