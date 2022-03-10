package io.openems.edge.heater.biomassheater.gilles;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
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
import io.openems.edge.heater.api.StartupCheckHandler;
import io.openems.edge.heater.biomassheater.gilles.api.BiomassHeaterGilles;
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
import java.util.Optional;

// Todo: Den Kommentar irgendwo anders speichern.
// Die Steuerung der Hackgutanlage ist etwas eigen. An/aus erfolgt über den nicht dokumentierten Coil mit Register
// Nummer 16387. Leistungsregelung erfolgt über den slide-in-max Regler. Slide-in ist der Einschub an Brennmaterial.
// Es wird nicht direkt die Einschub % eingestellt (Register 24578), sondern nur der Max Einschub limitiert.
// Grund: Der Kessel soll sich selber regeln und etwas Spielraum haben, damit er die Verbrennung optimal einstellen
// kann. Einschub-min der Anlage ist im Moment auf 40%, also weniger als das kann nicht an Einschub-max eingestellt
// werden. Dieser Min-Wert soll nicht verändert werden.
// Der Kessel hat einen Register für Vorlauf-Min, aber keinen für Vorlauf. Auf heater interface FLOW_TEMPERATURE ist die
// Kesseltemperatur gemapped (sollte ähnlich sein). SET_POINT_TEMPERATURE von heater.api ist dann Kesseltemperatur soll.
// Mir ist nicht bekannt wie der Kessel das nach-Temperatur-regeln vom nach-Einschub-regeln trennt. Es gibt keinen
// Register um das eine oder andere einzustellen.
//
// Für etwaige Fragen ist der Ansprechpartner bei Gilles (mittlerweile von Hargassner übernommen):
// Wolfgang Mampel, Tel. +43 664 8573374

/**
 * This module reads the most important variables available via Modbus from a Gilles (now Hargassner) biomass heater and
 * maps them to OpenEMS channels. The module is written to be used with the Heater interface methods (EnableSignal) and
 * ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like temperature specified,
 * the heater will turn on with default settings. The default settings are configurable in the config.
 * The heater can be controlled with setHeatingPowerPercentSetpoint() (set power in %) or setTemperatureSetpoint().
 * setHeatingPowerSetpoint() (set power in kW) and related methods are currently not supported by this heater.
 * <p>
 * ToDo:
 * Heater can do setTemperatureSetpoint(), but unclear how this works together with setHeatingPowerPercentSetpoint() and
 * EnableSignal. Common sense would suggest that if setTemperatureSetpoint() is used, the heater turns itself on and off
 * and also automatically regulate the heating power.
 */
@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Heater.Biomass.Gilles",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
        })
public class BiomassHeaterGillesImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, BiomassHeaterGilles {

    private final Logger log = LoggerFactory.getLogger(BiomassHeaterGillesImpl.class);

    @Reference
    protected ComponentManager cpm;

    @Reference
    protected ConfigurationAdmin ca;

    private boolean printInfoToLog;
    private boolean readOnly;
    private boolean startupStateChecked = false;
    private int defaultSetPointPowerPercent;
    private double powerPercentSetpoint = 0.d;
    private boolean checkLimitsOfConfigEntries;
    private boolean checkLimitsAndUpdateChannel = false;
    private int exceptionalStateValue = 0;
    private boolean exceptionalStateActive;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "GILLES_WOODCHIPS_HEATER_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "GILLES_WOODCHIPS_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean onlyOnOff;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public BiomassHeaterGillesImpl() {
        super(OpenemsComponent.ChannelId.values(),
                BiomassHeaterGilles.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modBusUnitId(), this.ca, "Modbus", config.modBusBridgeId());
        this.readOnly = config.readOnly();
        this.printInfoToLog = config.printInfoToLog();

        if (this.isEnabled() == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }

        if (this.readOnly == false) {
            this.onlyOnOff = config.onlyOnOff();
            this.startupStateChecked = false;
            this.defaultSetPointPowerPercent = config.defaultSetPointPowerPercent();
            this.checkLimitsOfConfigEntries = true;
            this.powerPercentSetpoint = config.defaultSetPointPowerPercent();
            this.checkLimitsAndUpdateChannel = true;
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

                new FC2ReadInputsTask(10000, Priority.HIGH,
                        m(BiomassHeaterGilles.ChannelId.DI_10000_ERROR, new CoilElement(10000)),
                        m(BiomassHeaterGilles.ChannelId.DI_10001_WARNING, new CoilElement(10001)),
                        m(BiomassHeaterGilles.ChannelId.DI_10002_CLEANING, new CoilElement(10002)),
                        m(BiomassHeaterGilles.ChannelId.DI_10003_FUME_EXTRACTOR, new CoilElement(10003)),
                        m(BiomassHeaterGilles.ChannelId.DI_10004_AIR_BLOWER, new CoilElement(10004)),
                        m(BiomassHeaterGilles.ChannelId.DI_10005_PRIMARY_AIR_BLOWER, new CoilElement(10005)),
                        m(BiomassHeaterGilles.ChannelId.DI_10006_SECONDARY_AIR_BLOWER, new CoilElement(10006)),
                        m(BiomassHeaterGilles.ChannelId.DI_10007_STOKER, new CoilElement(10007)),
                        m(BiomassHeaterGilles.ChannelId.DI_10008_ROTARY_VALVE, new CoilElement(10008)),
                        m(BiomassHeaterGilles.ChannelId.DI_10009_DOSI, new CoilElement(10009)),
                        m(BiomassHeaterGilles.ChannelId.DI_10010_SCREW_CONVEYOR_1, new CoilElement(10010)),
                        m(BiomassHeaterGilles.ChannelId.DI_10011_SCREW_CONVEYOR_2, new CoilElement(10011)),
                        m(BiomassHeaterGilles.ChannelId.DI_10012_SCREW_CONVEYOR_3, new CoilElement(10012)),
                        m(BiomassHeaterGilles.ChannelId.DI_10013_CROSS_CONVEYOR, new CoilElement(10013)),
                        m(BiomassHeaterGilles.ChannelId.DI_10014_MOVING_FLOOR_1, new CoilElement(10014)),
                        m(BiomassHeaterGilles.ChannelId.DI_10015_MOVING_FLOOR_2, new CoilElement(10015)),
                        m(BiomassHeaterGilles.ChannelId.DI_10016_IGNITION, new CoilElement(10016)),
                        m(BiomassHeaterGilles.ChannelId.DI_10017_LS_1, new CoilElement(10017)),
                        m(BiomassHeaterGilles.ChannelId.DI_10018_LS_2, new CoilElement(10018)),
                        m(BiomassHeaterGilles.ChannelId.DI_10019_LS_3, new CoilElement(10019)),
                        m(BiomassHeaterGilles.ChannelId.DI_10020_LS_LATERAL, new CoilElement(10020)),
                        m(BiomassHeaterGilles.ChannelId.DI_10021_LS_MOVING_FLOOR, new CoilElement(10021)),
                        m(BiomassHeaterGilles.ChannelId.DI_10022_ASH_SCREW_CONVEYOR_1, new CoilElement(10022)),
                        m(BiomassHeaterGilles.ChannelId.DI_10023_ASH_SCREW_CONVEYOR_2, new CoilElement(10023)),
                        m(BiomassHeaterGilles.ChannelId.DI_10024_SIGNAL_CONTACT_1, new CoilElement(10024)),
                        m(BiomassHeaterGilles.ChannelId.DI_10025_SIGNAL_CONTACT_2, new CoilElement(10025))

                ),
                new FC4ReadInputRegistersTask(20000, Priority.HIGH,
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(20000),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(20001),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20002_EXHAUST_TEMPERATURE, new UnsignedWordElement(20002),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20003_FURNACE_TEMPERATURE, new UnsignedWordElement(20003),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(20004),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20005_OXYGEN_PERCENT, new UnsignedWordElement(20005),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20006_NEGATIVE_PRESSURE, new UnsignedWordElement(20006),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER, new UnsignedWordElement(20007),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20008_HEATING_AMOUNT_TOTAL, new UnsignedWordElement(20008),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20009_PERCOLATION, new UnsignedWordElement(20009),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20010_RETURN_TEMPERATURE_HEAT_NETWORK, new UnsignedWordElement(20010),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20011_STORAGE_TANK_TEMPERATURE_SENSOR_1, new UnsignedWordElement(20011),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20012_STORAGE_TANK_TEMPERATURE_SENSOR_2, new UnsignedWordElement(20012),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20013_STORAGE_TANK_TEMPERATURE_SENSOR_3, new UnsignedWordElement(20013),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20014_STORAGE_TANK_TEMPERATURE_SENSOR_4, new UnsignedWordElement(20014),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20015_STORAGE_TANK_TEMPERATURE_SENSOR_5, new UnsignedWordElement(20015),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20016_STORAGE_TANK_TEMPERATURE_SENSOR_6, new UnsignedWordElement(20016),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20017_STORAGE_TANK_TEMPERATURE_SENSOR_7, new UnsignedWordElement(20017),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20018_STORAGE_TANK_TEMPERATURE_SENSOR_8, new UnsignedWordElement(20018),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20019_STORAGE_TANK_TEMPERATURE_SENSOR_9, new UnsignedWordElement(20019),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20020_STORAGE_TANK_TEMPERATURE_SENSOR_10, new UnsignedWordElement(20020),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20021_STORAGE_TANK_TEMPERATURE_SENSOR_11, new UnsignedWordElement(20021),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20022_STORAGE_TANK_TEMPERATURE_SENSOR_12, new UnsignedWordElement(20022),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20023_STORAGE_TANK_TEMPERATURE_SENSOR_13, new UnsignedWordElement(20023),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20024_STORAGE_TANK_TEMPERATURE_SENSOR_14, new UnsignedWordElement(20024),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20025_STORAGE_TANK_TEMPERATURE_SENSOR_15, new UnsignedWordElement(20025),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20026_STORAGE_TANK_TEMPERATURE_SENSOR_16, new UnsignedWordElement(20026),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20027_BOILER_TEMPERATURE_SET_POINT_READ, new UnsignedWordElement(20027),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20028_MINIMUM_FLOW_TEMPERATURE_SET_POINT_READ, new UnsignedWordElement(20028),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20029_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT_READ, new UnsignedWordElement(20029),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT, new UnsignedWordElement(20030),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT, new UnsignedWordElement(20031),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20032_OXYGEN_PERCENT_MIN_SET_POINT_READ, new UnsignedWordElement(20032),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20033_OXYGEN_PERCENT_MAX_SET_POINT_READ, new UnsignedWordElement(20033),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20034_SLIDE_IN_MIN_SET_POINT_READ, new UnsignedWordElement(20034),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.IR_20035_SLIDE_IN_MAX_SET_POINT_READ, new UnsignedWordElement(20035),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(24576, Priority.HIGH,
                        m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(24576),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24577_MINIMUM_FLOW_TEMPERATURE_SET_POINT, new UnsignedWordElement(24577),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24578_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT, new UnsignedWordElement(24578),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT, new UnsignedWordElement(24579),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT, new UnsignedWordElement(24580),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24581_OXYGEN_PERCENT_MIN_SET_POINT, new UnsignedWordElement(24581),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24582_OXYGEN_PERCENT_MAX_SET_POINT, new UnsignedWordElement(24582),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24583_SLIDE_IN_MIN_SET_POINT, new UnsignedWordElement(24583),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(BiomassHeaterGilles.ChannelId.HR_24584_SLIDE_IN_MAX_SET_POINT, new UnsignedWordElement(24584),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC1ReadCoilsTask(16387, Priority.HIGH,
                        m(BiomassHeaterGilles.ChannelId.DO_16387_ON_OFF_SWITCH, new CoilElement(16387)))
        );
        if (this.readOnly == false) {
            if (this.onlyOnOff == false) {
                protocol.addTasks(
                        new FC16WriteRegistersTask(24576,
                                m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(24576),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24577_MINIMUM_FLOW_TEMPERATURE_SET_POINT, new UnsignedWordElement(24577),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24578_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT, new UnsignedWordElement(24578),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT, new UnsignedWordElement(24579),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT, new UnsignedWordElement(24580),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24581_OXYGEN_PERCENT_MIN_SET_POINT, new UnsignedWordElement(24581),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24582_OXYGEN_PERCENT_MAX_SET_POINT, new UnsignedWordElement(24582),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24583_SLIDE_IN_MIN_SET_POINT, new UnsignedWordElement(24583),
                                        ElementToChannelConverter.DIRECT_1_TO_1),
                                m(BiomassHeaterGilles.ChannelId.HR_24584_SLIDE_IN_MAX_SET_POINT, new UnsignedWordElement(24584),
                                        ElementToChannelConverter.DIRECT_1_TO_1)
                        ));
            }
            protocol.addTask(new FC5WriteCoilTask(16387,
                    m(BiomassHeaterGilles.ChannelId.DO_16387_ON_OFF_SWITCH, new CoilElement(16387)))
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
                if (this.readOnly == false) {
                    this.writeCommands();
                }
                break;
        }
    }

    protected void channelmapping() {

        // Map warning and error.
        if (this.getWarningIndicator().isDefined()) {
            if (this.getWarningIndicator().get()) {
                this._setWarningMessage("Warning flag active.");
            } else {
                this._setWarningMessage("No warning");
            }
        } else {
            this._setWarningMessage("Modbus not connected");
        }
        if (this.getErrorIndicator().isDefined()) {
            if (this.getErrorIndicator().get()) {
                this._setErrorMessage("Error flag active");
            } else {
                this._setErrorMessage("No error");
            }
        } else {
            this._setErrorMessage("Modbus not connected");
        }
        this.getWarningMessageChannel().nextProcessImage();
        this.getErrorMessageChannel().nextProcessImage();

        if (this.getOnOffSwitch().isDefined() && this.getEffectiveHeatingPowerPercent().isDefined()) {
            if (this.getOnOffSwitch().get()) {
                int powerPercentRead = (int) Math.round(this.getEffectiveHeatingPowerPercent().get());
                if (powerPercentRead > 0) {
                    this._setHeaterState(HeaterState.RUNNING.getValue());
                } else {
                    this._setHeaterState(HeaterState.STANDBY.getValue());
                }
            } else {
                this._setHeaterState(HeaterState.OFF.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }
        // Switch process image because result is needed immediately for startupCheck.
        this.getHeaterStateChannel().nextProcessImage();
    }

    /**
     * Determine commands and send them to the heater.
     * Control of this device is a bit special. The register for the on/off command is not documented, it is coil 16387.
     * The power percent can be controlled by the amount of fuel the heater uses. However, the heater is not given a
     * fixed fuel consumption (could be done with holding register 24578). Instead, the heater is given a range for the
     * amount of fuel it can use (SlideInMinSetpoint - SlideInMaxSetpoint). To reduce the power, the upper limit of this
     * range is reduced. The lower limit should not be changed.
     * Reason: The heater should be allowed to regulate itself for optimal incineration conditions. To do that, it needs
     * a bit of leeway in the fuel set point. Because of this, there is also the config option "On/Off only" that
     * operates the heater without sending a power set point.
     * The channel SET_POINT_HEATING_POWER_PERCENT is not directly mapped to modbus. Its nextWriteValue is collected
     * manually, so the value can be stored locally and manipulated before sending it to the heater. A duplicate ’private’
     * channel is then used for the modbus writes.
     * The benefit of this design is that when ExceptionalState is active and applies it's own heatingPowerPercentSetpoint,
     * the previous set point is saved. Also, it is still possible to write to the channel during ExceptionalState, to
     * change the value that will be used after exceptional state ends.
     */
    protected void writeCommands() {

        // Map SET_POINT_HEATING_POWER_PERCENT from Heater interface.
        Optional<Double> writeValueOptional = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
        if (writeValueOptional.isPresent()) {
            this.powerPercentSetpoint = writeValueOptional.get();
            this.checkLimitsAndUpdateChannel = true;
            if (this.onlyOnOff) {
                this.log.info("Detected write to power percent set point. Ignored because of config setting \"On/Off only\".");
            }
        }

        /* Check Modbus connection. If this value is present, the connection is alive and heater status is available.
           Don't send commands to heater without checking heater status. */
        if (this.getSlideInMinSetPointRead().isDefined()) {
            int slideInMinValue = this.getSlideInMinSetPointRead().get();

            /* Do a range check on the value from the config. If value is out of range, update config with corrected value.
               Do a check on both defaultSetPointPowerPercent and powerPercentSetpoint, because these might be different
               at this point. */
            if (this.checkLimitsOfConfigEntries) {
                boolean updateConfig = false;
                if (this.defaultSetPointPowerPercent > 100) {
                    this.defaultSetPointPowerPercent = 100;
                    updateConfig = true;
                }
                if (this.defaultSetPointPowerPercent < slideInMinValue) {
                    this.defaultSetPointPowerPercent = slideInMinValue;
                    updateConfig = true;
                }
                this.checkLimitsOfConfigEntries = false;

                // A config update restarts the module.
                if (updateConfig) {
                    try {
                        ConfigurationUpdate.updateConfig(ca, this.servicePid(), "defaultSetPointPowerPercent", this.defaultSetPointPowerPercent);
                    } catch (IOException e) {
                        this.log.warn("Couldn't save new power percent setting to config. " + e.getMessage());
                    }
                }
            }

            if (this.checkLimitsAndUpdateChannel && this.onlyOnOff == false) {
                this.powerPercentSetpoint = TypeUtils.fitWithin((double) slideInMinValue, 100.d, this.powerPercentSetpoint);
                this._setHeatingPowerPercentSetpoint(this.powerPercentSetpoint);
                this.checkLimitsAndUpdateChannel = false;
            }
            int slideInSetpoint = (int) Math.round(this.powerPercentSetpoint);

            // Handle EnableSignal.
            boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
            if (this.useExceptionalState) {
                this.exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (this.exceptionalStateActive) {
                    this.exceptionalStateValue = this.getExceptionalStateValue();
                    if (this.exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
                        turnOnHeater = false;
                    } else {
                        turnOnHeater = true;
                        this.exceptionalStateValue = TypeUtils.fitWithin(slideInMinValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE, this.exceptionalStateValue);
                        slideInSetpoint = this.exceptionalStateValue;
                        this._setHeatingPowerPercentSetpoint(slideInSetpoint);
                    }
                }
            }

            // Check heater state at startup. Avoid turning off heater just because EnableSignal initial value is ’false’.
            if (this.startupStateChecked == false) {
                this.startupStateChecked = true;
                turnOnHeater = StartupCheckHandler.deviceAlreadyHeating(this, this.log);
            }

            if (turnOnHeater) {
                try {
                    this.setOnOffSwitch(true);
                    if (slideInSetpoint > 0 && this.onlyOnOff == false) {
                        this.setSlideInMaxSetPoint(slideInSetpoint);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            } else {
                try {
                    this.setOnOffSwitch(false);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }
    }

    /**
     * Information that is printed to the log if ’print info to log’ option is enabled.
     */
    protected void printInfo() {
        this.logInfo(this.log, "--Heater Gilles Woodchip--");
        this.logInfo(this.log, "Power percent set point (=slide in max): " + this.getHeatingPowerPercentSetpoint().orElse(-1.d));
        this.logInfo(this.log, "Power percent actual (=slide in): " + this.getEffectiveHeatingPowerPercent().orElse(-1.d));
        this.logInfo(this.log, "Slide in min set point: " + this.getSlideInMinSetPointRead().orElse(-1));
        this.logInfo(this.log, "Slide in max set point: " + this.getSlideInMaxSetPointRead().orElse(-1));
        this.logInfo(this.log, "Heating power actual: " + this.getEffectiveHeatingPower().orElse(-1.d));
        this.logInfo(this.log, "Boiler temperature: " + this.getFlowTemperature().orElse(-1));
        this.logInfo(this.log, "Boiler temperature set point: " + this.getBoilerTemperatureSetPointRead().orElse(-1));
        this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature().orElse(-1));
        this.logInfo(this.log, "Heater state: " + this.getHeaterState().asEnum());
        if (this.useExceptionalState) {
            this.logInfo(this.log, "Exceptional state: " + this.exceptionalStateActive + ", value: " + this.exceptionalStateValue);
        }
        this.logInfo(this.log, "Warning message: " + this.getWarningMessage().orElse("no Warnings"));
        this.logInfo(this.log, "Error message: " + this.getErrorMessage().orElse("No Errors"));
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
        if (this.getWarningMessage().get().equals("No warning") == false) {
            debugMessage = debugMessage + "|Warning";
        }
        if (this.getErrorMessage().get().equals("No error") == false) {
            debugMessage = debugMessage + "|Error";
        }
        return debugMessage;
    }
}

