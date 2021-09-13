package io.openems.edge.heater.biomassheater.gilles;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
import io.openems.edge.heater.biomassheater.gilles.api.MassHeaterWoodChips;
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

import java.util.Optional;

/**
 * Die Steuerung der Hackgutanlage ist etwas eigen. An/aus erfolgt über den nicht dokumentierten Coil mit Register
 * Nummer 16387. Leistungsregelung erfolgt über den slide-in-max Regler. Slide-in ist der Einschub an Brennmaterial.
 * Es wird nicht direkt die Einschub % eingestellt (Register 24578), sondern nur der Max Einschub limitiert.
 * Grund: Der Kessel soll sich selber regeln und etwas Spielraum haben, damit er die Verbrennung optimal einstellen
 * kann. Einschub-min der Anlage ist im Moment auf 40%, also weniger als das kann nicht an Einschub-max eingestellt
 * werden. Dieser Min-Wert soll nicht verändert werden.
 * Der Kessel hat einen Register für Vorlauf-Min, aber keinen für Vorlauf. Auf heater interface FLOW_TEMPERATURE ist die
 * Kesseltemperatur gemapped (sollte ähnlich sein). SET_POINT_TEMPERATURE von heater.api ist dann Kesseltemperatur soll.
 * Mir ist nicht bekannt wie der Kessel das nach-Temperatur-regeln vom nach-Einschub-regeln trennt. Es gibt keinen
 * Register um das eine oder andere einzustellen.
 *
 * Für etwaige Fragen ist der Ansprechpartner bei Gilles (mittlerweile von Hargassner übernommen):
 * Wolfgang Mampel, Tel. +43 664 8573374
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Gilles.WoodChip",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class MassHeaterWoodChipsWoodChipsImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, MassHeaterWoodChips {

    private final Logger log = LoggerFactory.getLogger(MassHeaterWoodChipsWoodChipsImpl.class);

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    private boolean printInfoToLog;
    private boolean componentEnabled;
    private boolean readOnly;
    private double powerPercentSetpoint;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "GILLES_WOODCHIPS_HEATER_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "GILLES_WOODCHIPS_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";

    private Config config;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public MassHeaterWoodChipsWoodChipsImpl() {
        super(OpenemsComponent.ChannelId.values(),
                MassHeaterWoodChips.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modBusUnitId(), this.cm, "Modbus", config.modBusBridgeId());


        this.componentEnabled = config.enabled();
        this.printInfoToLog = config.printInfoToLog();
        this.readOnly = config.readOnly();

        if (this.readOnly == false) {
            this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());

            TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
            String timerTypeEnableSignal;
            if (config.enableSignalTimerIsCyclesNotSeconds()) {
                timerTypeEnableSignal = "TimerByCycles";
            } else {
                timerTypeEnableSignal = "TimerByTime";
            }
            timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerTypeEnableSignal, config.waitTimeEnableSignal());
            this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
            this.useExceptionalState = config.useExceptionalState();
            if (this.useExceptionalState) {
                String timerTypeExceptionalState;
                if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
                    timerTypeExceptionalState = "TimerByCycles";
                } else {
                    timerTypeExceptionalState = "TimerByTime";
                }
                timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
            }
        }

        if (this.componentEnabled == false) {
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

                new FC2ReadInputsTask(10000, Priority.HIGH,
                        m(MassHeaterWoodChips.ChannelId.DISTURBANCE, new CoilElement(10000)),
                        m(MassHeaterWoodChips.ChannelId.WARNING, new CoilElement(10001)),
                        m(MassHeaterWoodChips.ChannelId.CLEARING_ACTIVE, new CoilElement(10002)),
                        m(MassHeaterWoodChips.ChannelId.VACUUM_CLEANING_ON, new CoilElement(10003)),
                        m(MassHeaterWoodChips.ChannelId.FAN_ON, new CoilElement(10004)),
                        m(MassHeaterWoodChips.ChannelId.FAN_PRIMARY_ON, new CoilElement(10005)),
                        m(MassHeaterWoodChips.ChannelId.FAN_SECONDARY_ON, new CoilElement(10006)),
                        m(MassHeaterWoodChips.ChannelId.STOKER_ON, new CoilElement(10007)),
                        m(MassHeaterWoodChips.ChannelId.ROTARY_VALVE_ON, new CoilElement(10008)),
                        m(MassHeaterWoodChips.ChannelId.DOSI_ON, new CoilElement(10009)),
                        m(MassHeaterWoodChips.ChannelId.HELIX_1_ON, new CoilElement(10010)),
                        m(MassHeaterWoodChips.ChannelId.HELIX_2_ON, new CoilElement(10011)),
                        m(MassHeaterWoodChips.ChannelId.HELIX_3_ON, new CoilElement(10012)),
                        m(MassHeaterWoodChips.ChannelId.CROSS_CONVEYOR, new CoilElement(10013)),
                        m(MassHeaterWoodChips.ChannelId.SLIDING_FLOOR_1_ON, new CoilElement(10014)),
                        m(MassHeaterWoodChips.ChannelId.SLIDING_FLOOR_2_ON, new CoilElement(10015)),
                        m(MassHeaterWoodChips.ChannelId.IGNITION_ON, new CoilElement(10016)),
                        m(MassHeaterWoodChips.ChannelId.LS_1, new CoilElement(10017)),
                        m(MassHeaterWoodChips.ChannelId.LS_2, new CoilElement(10018)),
                        m(MassHeaterWoodChips.ChannelId.LS_3, new CoilElement(10019)),
                        m(MassHeaterWoodChips.ChannelId.LS_LATERAL, new CoilElement(10020)),
                        m(MassHeaterWoodChips.ChannelId.LS_PUSHING_FLOOR, new CoilElement(10021)),
                        m(MassHeaterWoodChips.ChannelId.HELIX_ASH_1, new CoilElement(10022)),
                        m(MassHeaterWoodChips.ChannelId.HELIX_ASH_2, new CoilElement(10023)),
                        m(MassHeaterWoodChips.ChannelId.SIGNAL_CONTACT_1, new CoilElement(10024)),
                        m(MassHeaterWoodChips.ChannelId.SIGNAL_CONTACT_2, new CoilElement(10025))

                ),
                new FC4ReadInputRegistersTask(20000, Priority.HIGH,
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(20000)),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(20001)),
                        m(MassHeaterWoodChips.ChannelId.EXHAUST_TEMPERATURE, new UnsignedWordElement(20002)),
                        m(MassHeaterWoodChips.ChannelId.FIRE_ROOM_TEMPERATURE, new UnsignedWordElement(20003)),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(20004)),
                        m(MassHeaterWoodChips.ChannelId.OXYGEN_ACTIVE, new UnsignedWordElement(20005)),
                        m(MassHeaterWoodChips.ChannelId.VACUUM_ACTIVE, new UnsignedWordElement(20006)),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER, new UnsignedWordElement(20007)),
                        m(MassHeaterWoodChips.ChannelId.PERFORMANCE_WM, new UnsignedWordElement(20008)),
                        m(MassHeaterWoodChips.ChannelId.PERCOLATION, new UnsignedWordElement(20009)),
                        m(MassHeaterWoodChips.ChannelId.REWIND_GRID, new UnsignedWordElement(20010)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_1, new UnsignedWordElement(20011)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_2, new UnsignedWordElement(20012)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_3, new UnsignedWordElement(20013)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_4, new UnsignedWordElement(20014)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_5, new UnsignedWordElement(20015)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_6, new UnsignedWordElement(20016)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_7, new UnsignedWordElement(20017)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_8, new UnsignedWordElement(20018)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_9, new UnsignedWordElement(20019)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_10, new UnsignedWordElement(20020)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_11, new UnsignedWordElement(20021)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_12, new UnsignedWordElement(20022)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_13, new UnsignedWordElement(20023)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_14, new UnsignedWordElement(20024)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_15, new UnsignedWordElement(20025)),
                        m(MassHeaterWoodChips.ChannelId.BUFFER_SENSOR_16, new UnsignedWordElement(20026)),
                        m(MassHeaterWoodChips.ChannelId.BOILER_TEMPERATURE_SET_POINT_READ_ONLY, new UnsignedWordElement(20027)),
                        m(MassHeaterWoodChips.ChannelId.TEMPERATURE_FORWARD_MIN, new UnsignedWordElement(20028)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY, new UnsignedWordElement(20029)),
                        m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(20030)),
                        m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(20031)),
                        m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MIN_READ_ONLY, new UnsignedWordElement(20032)),
                        m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MAX_READ_ONLY, new UnsignedWordElement(20033)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MIN_READ_ONLY, new UnsignedWordElement(20034)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MAX_READ_ONLY, new UnsignedWordElement(20035))
                ),
                new FC3ReadRegistersTask(24576, Priority.HIGH,
                        m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(24576)),
                        m(MassHeaterWoodChips.ChannelId.BOILER_TEMPERATURE_MINIMAL_FORWARD, new UnsignedWordElement(24577)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_PERCENTAGE_VALUE_SETPOINT, new UnsignedWordElement(24578)),
                        m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(24579)),
                        m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(24580)),
                        m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MIN, new UnsignedWordElement(24581)),
                        m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MAX, new UnsignedWordElement(24582)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MIN_SETPOINT, new UnsignedWordElement(24583)),
                        m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MAX_SETPOINT, new UnsignedWordElement(24584))
                ),
                new FC1ReadCoilsTask(16387, Priority.HIGH,
                        m(MassHeaterWoodChips.ChannelId.EXTERNAL_CONTROL, new CoilElement(16387)))
        );
        if (this.readOnly == false) {
            protocol.addTasks(
                    new FC6WriteRegisterTask(24576,
                            m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(24576))),
                    new FC6WriteRegisterTask(24577,
                            m(MassHeaterWoodChips.ChannelId.BOILER_TEMPERATURE_MINIMAL_FORWARD, new UnsignedWordElement(24577))),
                    new FC6WriteRegisterTask(24578,
                            m(MassHeaterWoodChips.ChannelId.SLIDE_IN_PERCENTAGE_VALUE_SETPOINT, new UnsignedWordElement(24578))),
                    new FC6WriteRegisterTask(24579,
                            m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(24579))),
                    new FC6WriteRegisterTask(24580,
                            m(MassHeaterWoodChips.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(24580))),
                    new FC6WriteRegisterTask(24581,
                            m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MIN, new UnsignedWordElement(24581))),
                    new FC6WriteRegisterTask(24582,
                            m(MassHeaterWoodChips.ChannelId.OXYGEN_PERFORMANCE_MAX, new UnsignedWordElement(24582))),
                    new FC6WriteRegisterTask(24583,
                            m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MIN_SETPOINT, new UnsignedWordElement(24583))),
                    new FC6WriteRegisterTask(24584,
                            m(MassHeaterWoodChips.ChannelId.SLIDE_IN_MAX_SETPOINT, new UnsignedWordElement(24584))),
                    new FC5WriteCoilTask(16387,
                            m(MassHeaterWoodChips.ChannelId.EXTERNAL_CONTROL, new CoilElement(16387)))
            );
        }
        return protocol;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            this.channelmapping();
        }
    }

    protected void channelmapping() {

        // Map warning and error.
        if (this.getWarning().value().isDefined()) {
            if (this.getWarning().value().get()) {
                this._setWarningMessage("Warning flag active.");
            } else {
                this._setWarningMessage("No warning");
            }
        } else {
            this._setWarningMessage("Modbus not connected");
        }
        if (this.getDisturbance().value().isDefined()) {
            if (this.getDisturbance().value().get()) {
                this._setErrorMessage("Error flag active");
            } else {
                this._setErrorMessage("No error");
            }
        } else {
            this._setErrorMessage("Modbus not connected");
        }

        if (this.getExternalControl().value().isDefined() && this.getEffectiveHeatingPowerPercent().isDefined()) {
            if (this.getExternalControl().value().get()) {
                int powerPercentRead = (int)Math.round(this.getEffectiveHeatingPowerPercent().get());
                if (powerPercentRead > 0) {
                    this._setHeaterState(HeaterState.HEATING.getValue());
                } else {
                    // ToDo: probably the state ’starting up or preheat’ can also be determined.
                    this._setHeaterState(HeaterState.STANDBY.getValue());
                }
            } else {
                this._setHeaterState(HeaterState.OFF.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }

        if (this.readOnly == false) {

            // Map SET_POINT_HEATING_POWER_PERCENT from Heater interface.
            Optional<Double> writeValue = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
            if (writeValue.isPresent() && this.getSlideInMinReadOnly().value().isDefined()) {
                this.powerPercentSetpoint = writeValue.get();
                int slideInMinValue = this.getSlideInMinReadOnly().value().get();
                if (this.powerPercentSetpoint < slideInMinValue) {
                    this.powerPercentSetpoint = slideInMinValue;
                }
                this._setHeatingPowerPercentSetpoint(this.powerPercentSetpoint);
            }

            // Handle EnableSignal.
            boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
            if (this.useExceptionalState) {
                boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    int exceptionalStateValue = this.getExceptionalStateValue();
                    if (exceptionalStateValue <= 0) {
                        // Turn off heater when ExceptionalStateValue = 0.
                        turnOnHeater = false;
                    } else {
                        // When ExceptionalStateValue is between 0 and 100, set heater to this PowerPercentage.
                        turnOnHeater = true;
                        if (exceptionalStateValue > 100) {
                            exceptionalStateValue = 100;
                        }
                        try {
                            this.setHeatingPowerPercentSetpoint(exceptionalStateValue);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't write in Channel " + e.getMessage());
                        }
                    }
                }
            }

            if (turnOnHeater) {
                try {
                    this.getExternalControl().setNextWriteValue(true);
                    this.getSlideInMaxSetpoint().setNextWriteValue((int)Math.round(this.powerPercentSetpoint));
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            } else {
                try {
                    this.getExternalControl().setNextWriteValue(false);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }

        if (this.printInfoToLog) {
            this.logInfo(this.log, "--Heater Gilles Woodchip--");
            this.logInfo(this.log, "Power percent set point (write mode only): " + this.getHeatingPowerPercentSetpoint());
            this.logInfo(this.log, "Power percent actual: " + this.getEffectiveHeatingPowerPercent());
            this.logInfo(this.log, "Heating power actual: " + this.getEffectiveHeatingPower());
            this.logInfo(this.log, "Boiler temperature: " + this.getFlowTemperature());
            this.logInfo(this.log, "Boiler temperature setpoint: " + this.getBoilerTemperatureSetPointReadOnly().value());
            this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
            this.logInfo(this.log, "Heater state: " + this.getHeaterState());
            this.logInfo(this.log, "Warning message: " + this.getWarningMessage().get());
            this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
            this.logInfo(this.log, "");
        }
    }

}

