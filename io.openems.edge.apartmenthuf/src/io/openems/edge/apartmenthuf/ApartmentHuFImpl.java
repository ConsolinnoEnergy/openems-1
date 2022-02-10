package io.openems.edge.apartmenthuf;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.apartmenthuf.api.ApartmentHuF;
import io.openems.edge.apartmenthuf.api.CommunicationCheck;
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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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
 * This module reads all variables available via Modbus from an Apartment HuF and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the Apartment HuF via "setNextWriteValue" method.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Miscellaneous.Apartment.HuF",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})


public class ApartmentHuFImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ApartmentHuF {

    @Reference
    protected ConfigurationAdmin cm;


    private final Logger log = LoggerFactory.getLogger(ApartmentHuFImpl.class);

    private int temperatureCalibration;
    private boolean detailedDebug;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ApartmentHuFImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ApartmentHuF.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.detailedDebug = config.detailedDebug();
        this.temperatureCalibration = config.tempCal();

    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.detailedDebug = config.detailedDebug();
        this.temperatureCalibration = config.tempCal();
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(0, Priority.LOW,
                        m(ApartmentHuF.ChannelId.IR_0_VERSION, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuF.ChannelId.IR_2_ERROR, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuF.ChannelId.IR_3_LOOP_TIME, new UnsignedWordElement(3),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(4),
                        new DummyRegisterElement(5),
                        m(ApartmentHuF.ChannelId.IR_6_WALL_TEMPERATURE_HUF, new SignedWordElement(6),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuF.ChannelId.IR_7_AIR_TEMPERATURE_HUF, new SignedWordElement(7),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuF.ChannelId.IR_8_AIR_HUMIDITY_HUF, new SignedWordElement(8),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(ApartmentHuF.ChannelId.IR_9_AIR_PRESSURE_HUF, new SignedWordElement(9),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
                ),

                new FC3ReadRegistersTask(0, Priority.HIGH,
                        m(ApartmentHuF.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuF.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC16WriteRegistersTask(0,
                        m(ApartmentHuF.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuF.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            switch (event.getTopic()) {
                case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                    this.updateHufChannelForInternalUse();
                    break;
                case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                    if (getSetCommunicationCheckChannel().value().asEnum() != CommunicationCheck.RECEIVED) {
                        try {
                            this.getSetCommunicationCheckChannel().setNextWriteValue(CommunicationCheck.RECEIVED);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.logError(this.log, "Couldn't write into CommunicationCheckChannel!");
                        }
                    } else {
                        this.logWarn(this.log, "Communication failed!");
                    }
                    try {
                        if (this.setTemperatureCalibrationChannel().value().orElse(TEMP_CALIBRATION_ALTERNATE_VALUE) != this.temperatureCalibration) {
                            this.setTemperatureCalibrationChannel().setNextWriteValue(this.temperatureCalibration);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logError(this.log, "Couldn't calibrate TemperatureSensor");
                    }
                    break;
            }
        }
    }

    /**
     * Maps ModbusChannel to "Real" Channel -> if modbus communication fails no null values will be written.
     * Since it's called before the Event {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE} it needs
     * to call the {@link Channel#getNextValue()} method.
     */
    private void updateHufChannelForInternalUse() {
        if (this._getWallTemperatureToHufChannel().getNextValue().isDefined()) {
            this.getWallTemperatureChannel().setNextValue(this._getWallTemperatureToHufChannel().value().get());
        }
        if (this._getAirTemperatureToHufChannel().getNextValue().isDefined()) {
            this.getAirTemperatureChannel().setNextValue(this._getAirTemperatureToHufChannel().value().get());
        }
        if (this._getAirHumidityToHufChannel().getNextValue().isDefined()) {
            this.getAirHumidityChannel().setNextValue(this._getAirHumidityToHufChannel().value().get());
        }
        if (this._getAirPressureToHufChannel().getNextValue().isDefined()) {
            this.getAirPressureChannel().setNextValue(this._getAirPressureToHufChannel().value().get());
        }
    }

    /**
     * DetailedDebug Message. Called when {@link #detailedDebug} is set to true and Controller Debug Log is active.
     *
     * @return the detailed Debug Message String.
     */
    private String detailedDebug() {

        return "Input Registers"
                + "Version Number: " + this.getVersionNumber().get()
                + "Error: " + this.getError().getName()
                + "Loop Time: " + this.getLoopTime().get() + " ms"
                + this.basicDebugMessage()
                + "Holding Registers"
                + "Modbus Communication Check: " + this.getSetCommunicationCheckChannel().value().asEnum().getName()
                + "Temperature Calibration: " + this.setTemperatureCalibrationChannel().value().get();
    }

    @Override
    public String debugLog() {
        if (this.detailedDebug) {
            return this.detailedDebug();

        } else {
            return this.basicDebugMessage();
        }
    }

    /**
     * Basic DebugMessage. Called by {@link #debugLog()} when {@link #detailedDebug} is false or when set to true: Called
     * by {@link #detailedDebug()}.
     *
     * @return the basic DebugMessage
     */
    private String basicDebugMessage() {

        return "Wall Temperature: " + getWallTemperature().orElse(0) / 10.0 + "°C"
                + "Air Temperature: " + getAirTemperature().orElse(0) / 10.0 + "°C"
                + "Air Humidity: " + getAirHumidity().orElse(0.0f) + "%"
                + "Air Pressure: " + getAirPressure().orElse(0.0f) + "hPa";
    }
}
