package io.openems.edge.cooler.analogue;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.Cooler;
import io.openems.edge.cooler.analogue.component.AnalogueCoolerAIO;
import io.openems.edge.cooler.analogue.component.AnalogueCoolerComponent;
import io.openems.edge.cooler.analogue.component.AnalogueCoolerLucidControl;
import io.openems.edge.cooler.analogue.component.AnalogueCoolerPWM;
import io.openems.edge.cooler.analogue.component.AnalogueCoolerRelay;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "AnalogueCooler", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class AnalogueCooler extends AbstractOpenemsComponent implements OpenemsComponent, Cooler, EventHandler {

    @Reference
    ConfigurationAdmin ca;
    @Reference
    ComponentManager cpm;


    private final Logger log = LoggerFactory.getLogger(AnalogueCooler.class);

    private TimerHandler timer;
    private static final String ENABLE_IDENTIFIER = "ENABLE_SIGNAL_IDENTIFIER";
    private static final String POWER_IDENTIFIER = "POWER_IDENTIFIER";
    private boolean isActive = false;
    private int overwritePower;
    private boolean isAutoRun = false;
    private int maxPowerKw;

    private Config config;

    private ControlType type = ControlType.PERCENT;


    private List<AnalogueCoolerComponent> coolerComponent = new ArrayList<>();


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        SET_DEFAULT_ACTIVE_POWER_VALUE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        SET_DEFAULT_MINIMUM_POWER_VALUE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        CURRENT_POWER_KW(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        CURRENT_POWER_PERCENT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        MAX_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT));

        private final Doc doc;

        private ChannelId(Doc doc) {
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


    public AnalogueCooler() {
        super(OpenemsComponent.ChannelId.values(),
                Cooler.ChannelId.values(),
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

    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {

        Arrays.stream(config.analogueId()).forEach(id -> {
            try {
                switch (config.analogueType()) {
                    case PWM:
                        this.coolerComponent.add(new AnalogueCoolerPWM(this.cpm, id, this.maxPowerKw, config.controlType(), config.defaultMinPower()));
                        break;
                    case AIO:
                        this.coolerComponent.add(new AnalogueCoolerAIO(this.cpm, id, this.maxPowerKw, config.controlType(), config.defaultMinPower()));
                        break;
                    case RELAY:
                        this.coolerComponent.add(new AnalogueCoolerRelay(this.cpm, id, this.maxPowerKw, config.controlType(), config.defaultMinPower()));
                        break;
                    case LUCID_CONTROL:
                        this.coolerComponent.add(new AnalogueCoolerLucidControl(this.cpm, id, this.maxPowerKw, config.controlType(), config.defaultMinPower()));
                        break;
                }
            } catch (Exception ignored) {
                this.log.error("Unable to get Components. Check Config!");
            }
        });


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
                if (this.shouldCool()) {
                    this.isActive = true;
                    this.coolerComponent.forEach(component -> {
                        try {
                            component.startCooling(this.powerToApply());
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.error("Unable to start cooling!");
                        }
                    });
                } else {
                    this.isActive = false;
                    this.coolerComponent.forEach(component -> {
                        try {
                            component.stopCooling();
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.error("Unable to stop cooling!");
                        }
                    });
                }
                this.updatePowerChannel(this.coolerComponent.stream().findAny().get().getCurrentPowerApplied());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply cooling / stop cooling! Reason: " + e.getMessage());
            }
        }
    }

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


    private void updatePowerChannel(int currentPowerApplied) {
        this.getCurrentPowerPercent().setNextValue(currentPowerApplied);
        this.getCurrentPower().setNextValue((this.maxPowerKw * currentPowerApplied) / 100);
    }

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

    private boolean shouldCool() {
        //Depending on EnableSignal etc etc
        if (this.isAutoRun) {
            return true;
        }
        Optional<Boolean> enableValue = this.getEnableSignalChannel().getNextWriteValue();
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
    private WriteChannel<Integer> getDefaultActivePowerChannel() {
        return this.channel(ChannelId.SET_DEFAULT_ACTIVE_POWER_VALUE);
    }

    private WriteChannel<Integer> getDefaultMinPowerChannel() {
        return this.channel(ChannelId.SET_DEFAULT_MINIMUM_POWER_VALUE);
    }

}
