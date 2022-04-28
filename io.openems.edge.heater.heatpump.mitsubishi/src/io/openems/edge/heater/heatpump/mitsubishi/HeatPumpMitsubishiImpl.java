package io.openems.edge.heater.heatpump.mitsubishi;

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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeatpumpSmartGrid;
import io.openems.edge.heater.heatpump.mitsubishi.api.HeatpumpMitsubishi;
import io.openems.edge.heater.heatpump.mitsubishi.api.SystemOnOff;
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
 * This module reads the most important variables available via Modbus from a Mitsubishi heat pump and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpMitsubishi, HeatpumpSmartGrid and Heater.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.HeatPump.Mitsubishi",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})

public class HeatPumpMitsubishiImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		HeatpumpMitsubishi {

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(HeatPumpMitsubishiImpl.class);
	private boolean printInfoToLog;
	private boolean readOnly;
	private SystemOnOff systemOnOff = SystemOnOff.OFF;

	// This is essential for Modbus to work.
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatPumpMitsubishiImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeatpumpMitsubishi.ChannelId.values(),
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
			if (config.turnOnPump()) {
				this.systemOnOff = SystemOnOff.ON;
			} else {
				this.systemOnOff = SystemOnOff.OFF;
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		ModbusProtocol protocol = new ModbusProtocol(this,
				// Input registers read.
				new FC4ReadInputRegistersTask(8, Priority.HIGH,
						m(HeatpumpMitsubishi.ChannelId.IR8_ERROR_CODE, new UnsignedWordElement(8),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(58, Priority.HIGH,
						m(HeatpumpMitsubishi.ChannelId.IR58_OUTSIDE_TEMP, new SignedWordElement(58),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(59),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(60),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(61),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(62),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				// Holding registers read.
				new FC3ReadRegistersTask(25, Priority.HIGH,
				m(HeatpumpMitsubishi.ChannelId.HR25_SYSTEM_ON_OFF, new UnsignedWordElement(25),
						ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);

		if (this.readOnly == false) {
			protocol.addTasks(
					// Holding registers write.
					new FC16WriteRegistersTask(25,
							m(HeatpumpMitsubishi.ChannelId.HR25_SYSTEM_ON_OFF, new UnsignedWordElement(25),
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
		if (this.getErrorCode().isDefined()) {
			int errorCode = this.getErrorCode().get();
			String statusMessage = "";
			switch (errorCode) {
				case 8000:
					statusMessage = "No error (Code 8000)";
					break;
				default:
					statusMessage = "Error code " + errorCode;
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

		// Test Modbus write by setting system on/off.
		try {
			this.setSystemOnOff(this.systemOnOff);
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
		this.logInfo(this.log, "System on/off: " + this.getSystemOnOff().asEnum().getName());
		this.logInfo(this.log, "Error code: " + this.getErrorCode());
		this.logInfo(this.log, "Status message: " + this.getStatusMessage());
	}

}
