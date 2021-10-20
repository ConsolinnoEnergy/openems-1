package io.openems.edge.apartmentmodule;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.apartmentmodule.api.ApartmentModule;
import io.openems.edge.apartmentmodule.api.CommunicationCheck;
import io.openems.edge.apartmentmodule.api.OnOff;
import io.openems.edge.apartmentmodule.api.ValveDirection;
import io.openems.edge.apartmentmodule.api.ValveStatus;
import io.openems.edge.apartmentmodule.api.ValveType;
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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
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

import java.util.Optional;

/**
 * This module reads all variables available via Modbus from an  Apartment Module and maps them to OpenEMS
 * channels. Monitor an ApartmentLine / Cord Temperature as well as HeatRequest and additionally if configured as a TopAM
 * Control 2 Relays.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Miscellaneous.Apartment.Module",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class ApartmentModuleImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ApartmentModule {

    private final Logger log = LoggerFactory.getLogger(ApartmentModuleImpl.class);

    private int temperatureCalibration;
    private boolean topAM;
    private int relaysForOpeningOrDirectionHydraulicMixer;
    private static final int DEFAULT_VALVE_PERMANENT = 0;
    private static final int DEFAULT_VALVE_MAX_SECONDS = 110;
    private static final int DEFAULT_VALVE_ONE_DIRECTION = 15;
    private boolean wasDeactivatedBefore = false;
    private boolean wasActivatedBefore = false;
    private boolean isOpen = false;
    private boolean isClosed = false;
    private boolean manuallyControlled;
    private ValveType valveType;
    private ValveDirection valveDirection;
    private int maxTemperature;
    private boolean useMaxTemp;
    private int minTemperature;
    private boolean useMinTemp;
    private boolean useReferenceTemperature;

    @Reference
    protected ConfigurationAdmin cm;

    private DateTime initialDeactivationTime;
    private int referenceMinTemperature;

    Config config;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ApartmentModuleImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ApartmentModule.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.config = config;
        this.setUpIfTopAm(config.modbusUnitId());
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId().getValue(), this.cm,
                "Modbus", config.modbusBridgeId());
        if (config.turnOnOrDirectionRelay() != 1 && config.turnOnOrDirectionRelay() != 2) {
            throw new ConfigurationException("Activate of ApartmentModule " + super.id(), "Wrong turnOnRelay: Expected 1 or 2, Received: " + config.turnOnOrDirectionRelay());
        }
        this.activationOrModifiedRoutine(config);
    }

    /**
     * This applies the basic Configuration of the Component either on activation or modification.
     *
     * @param config the config of this component.
     */
    private void activationOrModifiedRoutine(Config config) {
        this.relaysForOpeningOrDirectionHydraulicMixer = config.turnOnOrDirectionRelay();
        this.temperatureCalibration = config.tempCal();
        this.manuallyControlled = config.manuallyControlled();
        this.valveDirection = config.valveDirection();
        this.valveType = config.valveType();
        this.maxTemperature = config.maxTemperature();
        this.useMaxTemp = config.useMaxTemperature();
        this.minTemperature = config.minTemperature();
        this.useMinTemp = config.useMinTemperature();
        this.useReferenceTemperature = config.useReferenceTemperature();
        if (this.useReferenceTemperature) {
            this.referenceMinTemperature = config.minTemperatureThermometer();
        }
    }

    /**
     * Called before super.activate/modified to tell the ModbusProtocol what Protocol to define.
     *
     * @param modbusUnitId the unitId, usually from config.
     */
    private void setUpIfTopAm(ModbusId modbusUnitId) {
        switch (modbusUnitId) {
            case ID_1:
            case ID_4:
            case ID_5:
                this.topAM = false;
                break;
            case ID_2:
            case ID_3:
                this.topAM = true;
                break;
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsException {
        this.config = config;
        this.setUpIfTopAm(config.modbusUnitId());
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId().getValue(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.activationOrModifiedRoutine(config);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

        if (this.topAM) {
            return this.getTopAmModbusProtocol();
        } else {
            return this.getAmModbusProtocol();
        }
    }

    /**
     * The ApartmentModule Configuration for NONE TopAMs (not controlling Relays).
     *
     * @return the ModbusProtocol
     * @throws OpenemsException on Error.
     */
    private ModbusProtocol getAmModbusProtocol() throws OpenemsException {

        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(4, Priority.HIGH,
                        m(ApartmentModule.ChannelId.IR_4_EXTERNAL_REQUEST_ACTIVE, new UnsignedWordElement(4),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(0, Priority.HIGH,
                        m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC16WriteRegistersTask(0,
                        m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );
    }


    /**
     * The ApartmentModule Configuration for TopAMs (controlling Relays).
     *
     * @return the ModbusProtocol
     * @throws OpenemsException on Error.
     */
    private ModbusProtocol getTopAmModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(4, Priority.HIGH,
                        m(ApartmentModule.ChannelId.IR_4_EXTERNAL_REQUEST_ACTIVE, new UnsignedWordElement(4),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(5),
                        m(ApartmentModule.ChannelId.IR_6_TEMPERATURE, new SignedWordElement(6),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(10, Priority.HIGH,
                        m(ApartmentModule.ChannelId.IR_10_STATE_RELAY1, new UnsignedWordElement(10),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(20, Priority.HIGH,
                        m(ApartmentModule.ChannelId.IR_20_STATE_RELAY2, new UnsignedWordElement(20),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(0, Priority.HIGH,
                        m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC16WriteRegistersTask(0,
                        m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC16WriteRegistersTask(10,
                        m(ApartmentModule.ChannelId.HR_10_COMMAND_RELAY1, new UnsignedWordElement(10),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_11_TIMING_RELAY1, new UnsignedWordElement(11),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC16WriteRegistersTask(20,
                        m(ApartmentModule.ChannelId.HR_20_COMMAND_RELAY2, new UnsignedWordElement(20),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentModule.ChannelId.HR_21_TIMING_RELAY2, new UnsignedWordElement(21),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

    }

    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                if (this.topAM) {
                    this.valveUpdate();
                    this.temperatureUpdate();
                }
                    try {
                        if (this.setTemperatureCalibrationChannel().value().orElse(TEMP_CALIBRATION_ALTERNATE_VALUE) != this.temperatureCalibration) {
                            this.setTemperatureCalibrationChannel().setNextWriteValueFromObject(this.temperatureCalibration);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't write into TemperatureCalibrationChannel");
                    }
                this.communicationUpdate();
                this.requestUpdate();
                break;
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                if (this.topAM && this.manuallyControlled == false) {
                    if (this.useMaxTemp
                            && this.getLastKnowTemperatureChannel().value().orElse(0) >= this.maxTemperature
                            || this.isReferenceTemperatureTooLow()) {
                        this.closeHydraulicMixer();
                    } else if (this.useMinTemp
                            && this.getLastKnowTemperatureChannel().value().orElse(0) <= this.minTemperature
                            && this.isActivationRequestChannel().getNextWriteValue().orElse(false)) {
                        this.turnOnHydraulicMixer();
                    } else {
                        this.listenToActivation();
                    }
                }

        }
    }

    /**
     * Check if the referenceTemperature is too low. The Reference Temperature will be set by a (ApartmentModule)Controller.
     *
     * @return a boolean.
     */
    private boolean isReferenceTemperatureTooLow() {
        if (this.useReferenceTemperature) {
            return this.getReferenceTemperatureChannel().getNextWriteValueAndReset().orElse(DEFAULT_REFERENCE_TEMPERATURE) < this.referenceMinTemperature
                    && this.getLastKnowTemperatureChannel().value().orElse(DEFAULT_LAST_KNOWN_TEMPERATURE) >= this.minTemperature
                    && this.heatRequestInApartmentCord().getNextWriteValueAndReset().orElse(false);
        }
        return false;
    }

    /**
     * Check if an external Request was set.
     * Determined by a short Incoming heatRequest is active or a permanent HeatRequest.
     */
    private void requestUpdate() {
        Value<Boolean> externalHeatFlag = this.getSetExternalRequestFlagChannel().value();
        if (externalHeatFlag.isDefined() && externalHeatFlag.get()) {
            try {
                this.getSetExternalRequestFlagChannel().setNextWriteValue(false);
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.log.warn("Couldn't write in own Channel of HoldingRegister "
                        + this.getSetExternalRequestFlagChannel().channelId() + " of AM:" + super.id());
            }
        }
        boolean externalRequestPresent = this.getExternalRequestCurrent().isDefined();
        boolean externalRequest = externalRequestPresent ? this.getExternalRequestCurrent().get() : false;
        boolean applyHeat = externalHeatFlag.orElse(false) || externalRequest;
        if (this.getLastKnownRequestStatusChannel().value().isDefined() && this.getLastKnownRequestStatusChannel().value().get()) {
            if (externalHeatFlag.isDefined() && externalRequestPresent) {
                this.getLastKnownRequestStatusChannel().setNextValue(applyHeat);
            }
        } else if (externalHeatFlag.isDefined() || externalRequestPresent) {
            this.getLastKnownRequestStatusChannel().setNextValue(applyHeat);
        }
    }

    /**
     * Writes {@link CommunicationCheck#RECEIVED} to the CommunicationCheckChannel.
     */
    private void communicationUpdate() {
        if (getSetCommunicationCheckChannel().value().asEnum() != CommunicationCheck.RECEIVED) {
            try {
                this.getSetCommunicationCheckChannel().setNextWriteValue(CommunicationCheck.RECEIVED);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.logError(this.log, "Couldn't set Next Write Value of CommunicationCheck Channel");
            }
        }
    }

    /**
     * Updates the TemperatureValue.
     */
    private void temperatureUpdate() {
        if (this.getTemperatureChannel().value().isDefined()) {
            this.getLastKnowTemperatureChannel().setNextValue(this.getTemperatureChannel().value().get());
        }
    }

    /**
     * Initiates the Valveupdate if State of Relay1 and 2 is defined.
     */
    private void valveUpdate() {
        if (this.getStateRelay1().equals(OnOff.UNDEFINED) == false
                && this.getStateRelay2().equals(OnOff.UNDEFINED) == false) {
            this.updateValveStatus();
        }
    }

    /**
     * Updates the Valve status depending on: ValveType, what Relay is configured for opening/closing/direction/motor.
     */
    private void updateValveStatus() {
        boolean relay1 = getStateRelay1().getValue() == 1;
        boolean relay2 = getStateRelay2().getValue() == 1;
        switch (this.valveType) {

            case ONE_OPEN_ONE_CLOSE:
                boolean relay1IsActivation = this.relaysForOpeningOrDirectionHydraulicMixer == 1;
                if (relay1 && relay2) {
                    this.getValveStatusChannel().setNextValue(ValveStatus.ERROR);
                } else if (relay1) {
                    if (relay1IsActivation) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
                    } else {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
                    }
                } else if (relay2) {
                    if (relay1IsActivation) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
                    } else {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
                    }
                } else {
                    //both off:
                    if (this.isOpen || this.getValveStatusChannel().value().isDefined()
                            && this.getValveStatusChannel().value().asEnum().equals(ValveStatus.OPENING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPEN);
                    } else if (this.isClosed || this.getValveStatusChannel().value().isDefined()
                            && this.getValveStatusChannel().value().asEnum().equals(ValveStatus.CLOSING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSED);
                    }

                }
                break;

            case ONE_MOTOR_ONE_DIRECTION:
                boolean relay1IsDirection = this.relaysForOpeningOrDirectionHydraulicMixer == 1;
                boolean relay2IsDirection = this.relaysForOpeningOrDirectionHydraulicMixer == 2;
                //Both on -> it only matters what it means to activate the direction Relay
                if (relay1 && relay2) {
                    if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_CLOSING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
                    } else if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_OPENING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
                    }
                    // ONLY ONE Relay is active an either one is for direction
                } else if ((relay1IsDirection && !relay1 && relay2) || (relay2IsDirection && relay1)) {
                    if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_OPENING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
                    } else if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_CLOSING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
                    }
                    //error case where only Direction relay is on but not the other
                } else if ((relay1IsDirection && relay1) || (relay2IsDirection && relay2)) {
                    this.getValveStatusChannel().setNextValue(ValveStatus.ERROR);
                } else if (!relay1 && !relay2) {
                    if (this.isOpen || this.getValveStatusChannel().value().isDefined()
                            && this.getValveStatusChannel().value().asEnum().equals(ValveStatus.OPENING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.OPEN);
                    } else if (this.isClosed || this.getValveStatusChannel().value().isDefined()
                            && this.getValveStatusChannel().value().asEnum().equals(ValveStatus.CLOSING)) {
                        this.getValveStatusChannel().setNextValue(ValveStatus.CLOSED);
                    }
                }
                break;
        }
    }

    /**
     * Depending if the TopAM should start to heat up the Cord or not -> open/close valve for a certain amount of time.
     */
    private void listenToActivation() {
        Optional<Boolean> activation = this.isActivationRequestChannel().getNextWriteValue();
        boolean hydraulicMixerActivation = activation.orElse(false);
        this.isActivationRequestChannel().setNextValue(hydraulicMixerActivation);
        ValveStatus valveStatus = this.getValveStatusChannel().value().isDefined() ? this.getValveStatusChannel().value().asEnum() : ValveStatus.UNDEFINED;
        boolean valveShouldOpen = this.wasActivatedBefore == false
                || valveStatus.equals(ValveStatus.CLOSED) || valveStatus.equals(ValveStatus.CLOSING) || valveStatus.equals(ValveStatus.ERROR);
        boolean valveShouldClose = this.wasDeactivatedBefore == false
                || valveStatus.equals(ValveStatus.OPEN) || valveStatus.equals(ValveStatus.OPENING) || valveStatus.equals(ValveStatus.ERROR);
        if (hydraulicMixerActivation) {
            if (valveShouldOpen) {
                this.turnOnHydraulicMixer();
                this.wasDeactivatedBefore = false;
                if (valveStatus.equals(ValveStatus.OPENING)) {
                    this.wasActivatedBefore = true;
                    this.initialDeactivationTime = new DateTime();
                }
            } else if (!valveStatus.equals(ValveStatus.OPEN)) {
                this.shutDownRelays();
            }
        } else {
            if (valveShouldClose) {
                this.closeHydraulicMixer();
                this.wasActivatedBefore = false;
                if (valveStatus.equals(ValveStatus.CLOSING)) {
                    this.wasDeactivatedBefore = true;
                    this.initialDeactivationTime = new DateTime();
                }
            } else if (!valveStatus.equals(ValveStatus.CLOSED)) {
                this.shutDownRelays();
            }
        }
    }

    /**
     * Shuts down the Relays permanently e.g. stopping the Opening/Closing progress.
     */
    private void shutDownRelays() {
        DateTime now = new DateTime();
        DateTime compare = new DateTime(this.initialDeactivationTime).plusSeconds((DEFAULT_VALVE_MAX_SECONDS));
        if (this.valveType.equals(ValveType.ONE_MOTOR_ONE_DIRECTION)) {
            compare = new DateTime(this.initialDeactivationTime).plusSeconds((DEFAULT_VALVE_ONE_DIRECTION));
        }
        if (now.isAfter(compare)) {
            this._setRelay1(OnOff.OFF, DEFAULT_VALVE_PERMANENT);
            this._setRelay2(OnOff.OFF, DEFAULT_VALVE_PERMANENT);
        }
    }

    /**
     * Closes the Valve connected to the AM, depending on the Configuration.
     */
    private void closeHydraulicMixer() {
        boolean closingOrActivationForOneDirection = this.relaysForOpeningOrDirectionHydraulicMixer == 1;
        switch (this.valveType) {

            case ONE_OPEN_ONE_CLOSE:

                this._setRelay1((closingOrActivationForOneDirection ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
                this._setRelay2((closingOrActivationForOneDirection ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
                this.isClosed = true;
                this.isOpen = false;
                break;
            case ONE_MOTOR_ONE_DIRECTION:
                if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_CLOSING)) {
                    this._setRelay1(OnOff.ON, DEFAULT_VALVE_PERMANENT);
                    this._setRelay2(OnOff.ON, DEFAULT_VALVE_PERMANENT);
                } else {
                    this._setRelay1((closingOrActivationForOneDirection ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
                    this._setRelay2((closingOrActivationForOneDirection ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
                    this.isClosed = true;
                    this.isOpen = false;
                    break;
                }
                break;
        }

    }

    /**
     * Opens the Valve connected to the TopAM. The way the valve is controlled, depends on the Configuration.
     */
    private void turnOnHydraulicMixer() {
        boolean activation = this.relaysForOpeningOrDirectionHydraulicMixer == 1;
        switch (this.valveType) {

            case ONE_OPEN_ONE_CLOSE:
                this._setRelay1((activation ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
                this._setRelay2((activation ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
                break;
            case ONE_MOTOR_ONE_DIRECTION:
                if (this.valveDirection.equals(ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_OPENING)) {
                    this._setRelay1(OnOff.ON, DEFAULT_VALVE_PERMANENT);
                    this._setRelay2(OnOff.ON, DEFAULT_VALVE_PERMANENT);
                } else {
                    this._setRelay1((activation ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
                    this._setRelay2((activation ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
                }
                break;
        }

        this.isOpen = true;
        this.isClosed = false;
    }

    @Override
    public String debugLog() {
        String debug =
                "Input Register"
                        + "4 External Request Active: " + getExternalRequestCurrent().orElse(false)
                        + "\nHolding Registers\n"
                        + " 0 Modbus Communication Check: " + getSetCommunicationCheckChannel().value().asEnum().getName()
                        + " 1 External Request Flag: " + getSetExternalRequestFlagChannel().value().orElse(false);

        if (this.topAM) {
            debug +=
                    "\n--- TOP AM ---\n"
                            + "Input Register\n"
                            + "6 Temperature: " + getLastKnowTemperatureChannel().value().orElse(0) / 10.0 + "°C"
                            + " 10 State Relay1: " + getStateRelay1().getName()
                            + " 20 State Relay2: " + getStateRelay2().getName()
                            + "\nHolding Registers\n"
                            + " 2 Temperature Calibration: " + this.setTemperatureCalibrationChannel().value().orElse(TEMP_CALIBRATION_ALTERNATE_VALUE)
                            + " ValveStatus: " + this.getValveStatusChannel().value().asEnum().getName();
        }
        return debug;
    }

    /**
     * Tells calling Component if this AM is a TOP ApartmentModule.
     *
     * @return true if this is a TopAM.
     */
    @Override
    public boolean isTopAm() {
        return this.topAM;
    }
}
