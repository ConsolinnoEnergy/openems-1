package io.openems.edge.utility.conditionapplier.multiple;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.ConditionApplier;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The MultipleBooleanConditionApplier is an expansion of the {@link ConditionApplier} and allows the ability to check
 * via {@link CheckConditions} if the input of "n" BooleanChannel is, what the Condition Applier expects.
 * E.g. if the CheckConditions type is set to {@link CheckConditions#AND} ALL of the InputValues must match the expected Value (true or false)
 * otherwise the  CheckCondition Result will be false.
 * If the CheckConditions type is set to {@link CheckConditions#OR} ANY inputValue may match the expectedValue.
 * If none matches the expected Value -> result will be false.
 * If the Result of the CheckCondition is true -> the active Value will be written into the ChannelAddress provided via config
 * otherwise the optional inactiveValue will be written to the configured ChannelAddress.
 */

@Designate(ocd = ConfigMultipleConditionApplier.class, factory = true)
@Component(name = "MultipleBooleanConditionApplierImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class MultipleBooleanConditionApplierImpl extends AbstractOpenemsComponent implements OpenemsComponent, ConditionApplier, EventHandler {

    private static final String CONFIGURATION_SPLITTER = ":";
    private static final int SPLIT_LENGTH = 2;
    private static final int CHANNEL_ADDRESS_CONFIG_POSITION = 0;
    private static final int EXPECTED_VALUE_CONFIG_POSITION = 1;

    private final Logger log = LoggerFactory.getLogger(MultipleBooleanConditionApplierImpl.class);

    public MultipleBooleanConditionApplierImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ConditionApplier.ChannelId.values());
    }

    @Reference
    ConfigurationAdmin ca;

    @Reference
    ComponentManager cpm;

    private final Map<ChannelAddress, Boolean> conditionsToExpectedValue = new HashMap<>();
    private ConditionWrapper conditionWrapper;
    private CheckConditions checkConditions;

    private ConfigMultipleConditionApplier config;


    @Activate
    void activate(ComponentContext context, ConfigMultipleConditionApplier config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }

    /**
     * This method will be called either on activation or modification.
     * It applies standard parameters and the {@link ConditionWrapper} as well as the {@link ConditionChecker} will be created
     * depending on the configuration of the ConditionApplier.
     * It also applies a default active and inactive Value to it's channel, which can be updated via REST
     * -> and stored in config again
     *
     * @param config the config of the Component
     * @throws OpenemsError.OpenemsNamedException if a ChannelAddress is in a wrong format. NOTE:
     *                                            It CONTINUES to apply channelAddresses, even if one entry is wrong -> look at the info log which entry is wrong!
     */
    private void activateOrModifiedRoutine(ConfigMultipleConditionApplier config) throws OpenemsError.OpenemsNamedException {
        this.config = config;
        this.conditionsToExpectedValue.clear();
        Arrays.stream(config.channelAddresses()).forEach(entry -> {
            try {
                String[] splits = entry.split(CONFIGURATION_SPLITTER);
                if (splits.length != SPLIT_LENGTH) {
                    this.log.warn("Configuration String : " + entry + " is wrong!");
                }
                this.conditionsToExpectedValue.put(ChannelAddress.fromString(splits[CHANNEL_ADDRESS_CONFIG_POSITION]), Boolean.parseBoolean(splits[EXPECTED_VALUE_CONFIG_POSITION]));
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.info("Entry: " + entry + " is not a ChannelAddress, continue with other Config entries");
            }
        });


        this.conditionWrapper = new ConditionWrapper(config.useActiveValue(), config.activeValueIsChannel(), config.activeValue(),
                config.useInactiveValue(), config.inactiveValueIsChannel(),
                config.inactiveValue(), ChannelAddress.fromString(config.answerChannelAddress()));

        this.checkConditions = config.checkConditions();

        this._getDefaultActiveValueChannel().setNextValue(config.activeValue());
        this._getDefaultActiveValueChannel().nextProcessImage();
        this._getDefaultInactiveValueChannel().setNextValue(config.inactiveValue());
        this._getDefaultInactiveValueChannel().nextProcessImage();
    }

    @Modified
    void modified(ComponentContext context, ConfigMultipleConditionApplier config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
                boolean conditionMet = ConditionChecker.checkConditions(this.cpm, this.checkConditions, this.log, this.conditionsToExpectedValue);
                this.conditionWrapper.applyValueDependingOnConditionMet(conditionMet);
            } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                if (this._getDefaultActiveValueChannel().getNextWriteValue().isPresent()) {
                    this.updateConfig(true);
                } else if (this._getDefaultInactiveValueChannel().getNextWriteValue().isPresent()) {
                    this.updateConfig(false);
                }
            }
        }
    }

    /**
     * A Method to update the Config of this Component.
     *
     * @param activeValue is the ConfigEntry the Active value (true) or the inactive Value (false)
     */
    private void updateConfig(boolean activeValue) {


        try {
            Optional<String> channelValue = activeValue ?
                    this._getDefaultActiveValueChannel().getNextWriteValueAndReset() :
                    this._getDefaultInactiveValueChannel().getNextWriteValueAndReset();
            String propertyName = activeValue ? "activeValue" : "inactiveValue";
            String setPointValue = activeValue ? this.config.activeValue() : this.config.inactiveValue();

            if (channelValue.isPresent() && setPointValue.equals(channelValue.get()) == false) {
                ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), propertyName, channelValue);
            }
        } catch (IOException e) {
            this.log.warn("Couldn't update ChannelProperty, reason: " + e.getMessage());
        }
    }

    /**
     * The ConditionWrapper is a HelperClass, that helps the ConditionApplier to apply an active or inactive value, depending
     * on if the condition of the ConditionApplier was met.
     * The ConditionWrapper stores the active, inactive Value and the answerChannel, as well as the boolean if an (in)activeValue
     * should be applied at all.
     */
    private class ConditionWrapper {

        private final boolean useActiveValue;
        private final boolean activeValueIsChannel;
        private final String activeValue;
        private final boolean useInactiveValue;
        private final boolean inactiveValueIsChannel;
        private final String inactiveValue;
        private final ChannelAddress answerChannel;

        public ConditionWrapper(boolean useActiveValue, boolean activeValueIsChannel, String activeValue,
                                boolean useInactiveValue, boolean inactiveValueIsChannel,
                                String inactiveValue, ChannelAddress answerChannel) {
            this.useActiveValue = useActiveValue;
            this.activeValueIsChannel = activeValueIsChannel;
            this.activeValue = activeValue;
            this.useInactiveValue = useInactiveValue;
            this.inactiveValue = inactiveValue;
            this.inactiveValueIsChannel = inactiveValueIsChannel;
            this.answerChannel = answerChannel;
        }

        /**
         * Applies the active or inactive Value, determined by the boolean conditionMet.
         * Usually from the MultipleBooleanConditionApplier.
         *
         * @param conditionMet was the condition Met (true) -> writeActiveValue To channel
         */
        public void applyValueDependingOnConditionMet(boolean conditionMet) {
            if (conditionMet) {
                this.activeValueToChannel();
            } else {
                this.inactiveValueToChannel();
            }
        }

        /**
         * Writes the stored {@link #activeValue}, if it should be applied, to the stored channelAddress.
         */
        void activeValueToChannel() {
            if (this.useActiveValue) {
                String valueToWrite = this.activeValue;
                if (this.activeValueIsChannel) {
                    valueToWrite = this.getValueFromChannelAddress(this.activeValue);
                }
                this.writeValueToChannel(valueToWrite);
            }
        }

        /**
         * Get the Value From a Channel Address (if String is channelAddress).
         *
         * @param value the value -> either inactive or active value. In this case it should be a ChannelAddress
         * @return either the value
         */
        private String getValueFromChannelAddress(String value) {
            try {
                Channel<?> channel = cpm.getChannel(ChannelAddress.fromString(value));
                if (channel.value().isDefined()) {
                    return channel.value().get().toString();
                } else if (channel.getNextValue().isDefined()) {
                    return channel.getNextValue().get().toString();
                } else {
                    return "";
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn(id() + " Channel: " + value + " is not available");
                return "";
            }
        }

        /**
         * Writes the stored {@link #inactiveValue} to the ChannelAddress, if it should be applied at all.
         */
        void inactiveValueToChannel() {
            if (this.useInactiveValue) {
                String valueToWrite = this.activeValue;
                if (this.inactiveValueIsChannel) {
                    valueToWrite = this.getValueFromChannelAddress(this.activeValue);
                }
                this.writeValueToChannel(valueToWrite);
            }
        }

        /**
         * Writes a Value to the stored {@link #answerChannel}. Usually called internally by the {@link #inactiveValueToChannel()}
         * or {@link #activeValueToChannel()} method.
         *
         * @param value the value usually the {@link #activeValue} or {@link #inactiveValue}
         */
        @SuppressWarnings("checkstyle:RequireThis")
        void writeValueToChannel(String value) {
            if ((value == null || value.equals("")) == false) {
                try {
                    Channel<?> channelToApply = cpm.getChannel(this.answerChannel);
                    if (channelToApply instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) channelToApply).setNextWriteValueFromObject(value);
                    } else {
                        channelToApply.setNextValue(value);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    log.warn("Attention couldn't write to Channel: " + e.getMessage());
                }
            }

        }
    }
}
