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
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeatpumpSmartGrid;
import io.openems.edge.heater.heatpump.weishaupt.api.HeatpumpWeishaupt;
import io.openems.edge.heater.heatpump.weishaupt.api.OperatingMode;
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
 * This module reads the most important variables available via Modbus from a Weihaupt heat pump and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpWeishaupt, HeatpumpSmartGrid and Heater.
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
		HeatpumpWeishaupt {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(HeatPumpWeishauptImpl.class);
	private boolean printInfoToLog;
	private boolean readOnly;
	private OperatingMode defaultModeOfOperation = OperatingMode.UNDEFINED;

	// This is essential for Modbus to work.
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpWeishauptImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpWeishaupt.ChannelId.values(),
				HeatpumpSmartGrid.ChannelId.values(),
				Heater.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.printInfoToLog = config.printInfoToLog();
		this.readOnly = config.readOnly();
		if (this.readOnly == false) {
			this.defaultModeOfOperation = config.defaultModeOfOperation();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		ModbusProtocol protocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.HR1_OUTSIDE_TEMP, new SignedWordElement(0),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(1),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(HeatpumpWeishaupt.ChannelId.HR3_DOMESTIC_HOT_WATER, new SignedWordElement(2),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(3),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(4),
								ElementToChannelConverter.SCALE_FACTOR_1)
				),
				new FC3ReadRegistersTask(102, Priority.HIGH,
						m(HeatpumpWeishaupt.ChannelId.HR103_STATUS_CODE, new UnsignedWordElement(102),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.HR104_BLOCKED_CODE, new UnsignedWordElement(103),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeatpumpWeishaupt.ChannelId.HR105_ERROR_CODE, new UnsignedWordElement(104),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(141, Priority.HIGH,
				m(HeatpumpWeishaupt.ChannelId.HR142_OPERATING_MODE, new UnsignedWordElement(141),
						ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(141,
							m(HeatpumpWeishaupt.ChannelId.HR142_OPERATING_MODE, new UnsignedWordElement(141),
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
		}
		if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		// Parse status code.
		if (this.getStatusCode().isDefined()) {
			int statusCode = this.getStatusCode().get();
			String statusMessage = "Status code " + statusCode + ": ";
			switch (statusCode) {
				case 0:
				case 1:
					statusMessage = statusMessage + "Off";
					break;
				case 2:
					statusMessage = statusMessage + "Heating";
					break;
				case 3:
					statusMessage = statusMessage + "Swimming pool";
					break;
				case 4:
					statusMessage = statusMessage + "Domestic hot water";
					break;
				case 5:
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
			this._setStatusMessage(statusMessage);
		} else {
			this._setStatusMessage("No Modbus connection");
		}
		this.getStatusMessageChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 */
	protected void writeCommands() {

		// Test Modbus write by setting operating mode.
		try {
			this.setOperatingMode(this.defaultModeOfOperation.getValue());
		} catch (OpenemsError.OpenemsNamedException e) {
			this.logError(this.log, "Could not write to operating mode channel. "
					+ "Reason: " + e.getMessage());
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
		this.logInfo(this.log, "Blocked code: " + this.getBlockedCode());
		this.logInfo(this.log, "Error code: " + this.getErrorCode());
		this.logInfo(this.log, "Status message: " + this.getStatusMessage());
	}

}
