package io.openems.edge.heater.analogue;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.analogue.component.AnalogueHeaterAIO;
import io.openems.edge.heater.analogue.component.AnalogueHeaterComponent;
import io.openems.edge.heater.analogue.component.AnalogueHeaterLucidControl;
import io.openems.edge.heater.analogue.component.AnalogueHeaterPWM;
import io.openems.edge.heater.analogue.component.AnalogueHeaterRelay;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Optional;

/**
 * The Analogue Heater. It is a Heater, that controls a Analogue Component, connected to a Heater, such as a CHP or other
 * PowerPlant.
 * ATM Possible Analogue Heaters can be controlled by:
 * <p>
 *     <ul>
 * <li>{@link io.openems.edge.consolinno.aio.api.AioChannel}</li>
 * <li>{@link io.openems.edge.lucidcontrol.device.api.LucidControlDeviceOutput}</li>
 * <li>{@link io.openems.edge.consolinno.pwm.api.Pwm} or {@link io.openems.edge.pwm.device.api.PwmPowerLevelChannel}</li>
 * <li>{@link io.openems.edge.consolinno.relay.api.Relay}</li>
 * </ul>
 * </p>
 * <p>
 *     Important note: The subClasses / extending classes of {@link io.openems.edge.heater.analogue.component.AbstractAnalogueHeaterComponent}
 *     will always calculate a percent value. Depending on the config if the Heater is set by a definite KW value or Percent, it will always
 *     transform the Value to a PERCENT value. Since it's easier to manage and calculate PercentValues from/for analogue Devices.
 * </p>
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "AnalogueHeater", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class AnalogueHeater extends AbstractOpenemsComponent implements OpenemsComponent, Heater, EventHandler {

    @Reference
    ConfigurationAdmin ca;
    @Reference
    ComponentManager cpm;


    private final Logger log = LoggerFactory.getLogger(AnalogueHeater.class);

    private TimerHandler timer;
    private static final String ENABLE_IDENTIFIER = "ENABLE_SIGNAL_IDENTIFIER";
    private static final String POWER_IDENTIFIER = "POWER_IDENTIFIER";
    private boolean isActive = false;
    private int overwritePower;
    private boolean isAutoRun = false;
    private int maxPowerKw;

    private Config config;

    private ControlType type = ControlType.PERCENT;


    private AnalogueHeaterComponent heaterComponent;


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        CURRENT_POWER_KW(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        CURRENT_POWER_PERCENT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        MAX_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    private Channel<Integer> getCurrentPower() {
        return this.channel(ChannelId.CURRENT_POWER_KW);
    }

    private Channel<Integer> getCurrentPowerPercent() {
        return this.channel(ChannelId.CURRENT_POWER_PERCENT);
    }

    private Channel<Integer> getMaxPower() {
        return this.channel(ChannelId.MAX_POWER);
    }


    public AnalogueHeater() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.activationOrModifiedRoutine(config);
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    /**
     * This method is an internal method, that will run all basic operations/setups either if the component was activated or modified.
     * @param config the config of the component (either on activation or modification)
     * @throws OpenemsError.OpenemsNamedException if a component couldn't be found
     * @throws ConfigurationException if a component could be found but was not the correct instance of XYZ.
     */
    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (config.analogueType()) {
            case PWM:
                this.heaterComponent = new AnalogueHeaterPWM(this.cpm, config.analogueId(), this.maxPowerKw, config.controlType(), config.defaultMinPower());
                break;
            case AIO:
                this.heaterComponent = new AnalogueHeaterAIO(this.cpm, config.analogueId(), this.maxPowerKw, config.controlType(), config.defaultMinPower());
                break;
            case RELAY:
                this.heaterComponent = new AnalogueHeaterRelay(this.cpm, config.analogueId(), this.maxPowerKw, config.controlType(), config.defaultMinPower());
                break;
            case LUCID_CONTROL:
                this.heaterComponent = new AnalogueHeaterLucidControl(this.cpm, config.analogueId(), this.maxPowerKw, config.controlType(), config.defaultMinPower());
                break;
        }
        if (this.timer != null) {
            this.timer.removeComponent();
        }
        this.timer = new TimerHandlerImpl(config.id(), this.cpm);
        this.timer.addOneIdentifier(ENABLE_IDENTIFIER, config.timerId(), config.maxTimeEnableSignal());
        this.timer.addOneIdentifier(POWER_IDENTIFIER, config.timerId(), config.maxTimePowerSignal());
        this.overwritePower = config.defaultRunPower();
        this.getDefaultActivePowerChannel().setNextValue(config.defaultRunPower());
        this.getDefaultMinPowerChannel().setNextValue(config.defaultMinPower());
        this.isAutoRun = config.autoRun();
        this.maxPowerKw = config.maxPower();
        this.getMaxPower().setNextValue(config.maxPower());
        this.type = config.controlType();
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {

        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            //Check for New Default Values
            this.getDefaultActivePowerChannel().getNextWriteValueAndReset().ifPresent(entry -> this.updateConfig(entry, true));
            this.getDefaultMinPowerChannel().getNextWriteValueAndReset().ifPresent(entry -> this.updateConfig(entry, false));
        } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                if (this.shouldHeat()) {
                    this.isActive = true;
                    this.heaterComponent.startHeating(this.powerToApply());
                } else {
                    this.isActive = false;
                    this.getEnableSignalChannel().getNextWriteValueAndReset();
                    this.getEnableSignalChannel().setNextValue(false);
                    this.heaterComponent.stopHeating();
                }
                this.updatePowerChannel(this.heaterComponent.getCurrentPowerApplied());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply heating / stop heating! Reason: " + e.getMessage());
            }
        }
    }

    /**
     * Updates the Config if either: {@link Heater.ChannelId#SET_DEFAULT_ACTIVE_POWER_VALUE} or {@link Heater.ChannelId#SET_DEFAULT_MINIMUM_POWER_VALUE}
     * is updated.
     * @param power which powerValue should be written to an identifier of the property
     * @param activePower if the value comes from the activePowerValueChannel (true) or the MinimumPowerValueChannel(false)
     */
    private void updateConfig(Integer power, boolean activePower) {
        Configuration c;

        try {

            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String propertyName = activePower ? "defaultRunPower" : "inactiveValue";
            int setPointValue = (int) properties.get(propertyName);

            if ((setPointValue != power)) {
                properties.put(propertyName, power);
                c.update(properties);
            }
        } catch (IOException e) {
            this.log.warn("Couldn't update ChannelProperty, reason: " + e.getMessage());
        }
    }

    /**
     * Writes the CurrentPower Applied to the {@link ChannelId#CURRENT_POWER_KW} and {@link ChannelId#CURRENT_POWER_PERCENT}
     * channel. It asks/reads the value from it's {@link #heaterComponent}.
     * @param currentPowerApplied the currentPower Applied in Percent
     */
    private void updatePowerChannel(int currentPowerApplied) {
        this.getCurrentPowerPercent().setNextValue(currentPowerApplied);
        int currentlyAppliedPowerInKw = (this.maxPowerKw * currentPowerApplied) / 100;
        this.getCurrentPower().setNextValue(currentlyAppliedPowerInKw);
    }

    /**
     * Gets the correct channel and value, defined by this {@link ControlType}.
     * It Either gets a default RunPower or the setPoint, depending if the PowerValue was written into the SetPointChannel.
     * ({@link #getSetPointPowerChannel()} for ControlType KW or {@link #getSetPointPowerPercentChannel()} for ControlType Percent)
     * @return the power that will be applied.
     */
    private int powerToApply() {
        Channel<?> channelToGetPowerValueFrom = this.getSetPointPowerPercentChannel();
        boolean needToCheckTime = true;
        switch (this.type) {
            case PERCENT:
                channelToGetPowerValueFrom = this.getSetPointPowerPercentChannel();
                break;
            case KW:
                channelToGetPowerValueFrom = this.getSetPointPowerChannel();
                break;
        }
        if (channelToGetPowerValueFrom.value().isDefined()) {
            this.overwritePower = (Integer) channelToGetPowerValueFrom.value().get();
            this.timer.resetTimer(POWER_IDENTIFIER);
            needToCheckTime = false;
            channelToGetPowerValueFrom.setNextValue(null);
        }
        if (needToCheckTime && this.timer.checkTimeIsUp(POWER_IDENTIFIER) == false) {
            this.overwritePower = this.config.defaultRunPower();
        }

        return this.overwritePower;
    }

    /**
     * Should this device Heat?
     * Either -> it is set to AutoRun -> always true
     * or if an EnableSignal was Set
     * else if this Component was Active before and the Time is not up, to reset the component
     * @return true if this device should run at {@link #overwritePower}.
     */
    private boolean shouldHeat() {
        if (this.isAutoRun) {
            return true;
        }
        Optional<Boolean> enableValue = this.getEnableSignalChannel().getNextWriteValueAndReset();
        if (enableValue.isPresent()) {
            this.timer.resetTimer(ENABLE_IDENTIFIER);
            this.getEnableSignalChannel().setNextValue(enableValue.get());
            return enableValue.get();
        } else {
            return this.isActive && this.timer.checkTimeIsUp(ENABLE_IDENTIFIER) == false;
        }
    }


    //-------------------------//
    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        return 0;
    }

    @Override
    public int getMaximumThermalOutput() {
        return 0;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {

    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public void requestMaximumPower() {

    }

    @Override
    public void setIdle() {

    }
    //-----------------//
}
