package io.openems.edge.utility.conditionapplier;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The ConditionChecker is a helper class for the ConditionApplier.
 * It gets a Map of ChannelAddresses and the expected Boolean value. After that it checks for each key,value pair
 * If the condition is met. If the {@link CheckConditions} is {@link CheckConditions#AND} it checks each condition,
 * or until one condition is NOT met -> return false
 * otherwise if the {@link CheckConditions} is {@link CheckConditions#OR} check for ONE condition to be true ->
 * return true.
 */
public interface ConditionChecker {

    static boolean checkConditions(ComponentManager cpm, CheckConditions checkConditions, Logger log, Map<ChannelAddress, Boolean> values) {
        AtomicBoolean conditionOk = new AtomicBoolean(true);
        if (checkConditions.equals(CheckConditions.OR) || checkConditions.equals(CheckConditions.XOR)) {
            conditionOk.set(false);
        }
        AtomicBoolean xorConditionWasSetToTrue = new AtomicBoolean(false);

        values.forEach((key, value) -> {
            // run until one condition of key value is false or if CheckCondition OR and not true
            if ((conditionOk.get() && checkConditions.equals(CheckConditions.AND))
                    || (checkConditions.equals(CheckConditions.OR) && conditionOk.get() == false) || checkConditions.equals(CheckConditions.XOR)) {
                try {
                    Optional<?> channelValue = Optional.empty();
                    Channel<?> channel = cpm.getChannel(key);
                    Boolean booleanChannelValue = false;
                    if (channel instanceof WriteChannel<?>) {
                        channelValue = ((WriteChannel<?>) channel).getNextWriteValue();
                    }
                    if (channelValue.isPresent() == false) {
                        channelValue = channel.value().asOptional();
                    }
                    if (channelValue.isPresent()) {
                        try {
                            booleanChannelValue = TypeUtils.getAsType(OpenemsType.BOOLEAN, channelValue);
                        } catch (IllegalArgumentException e) {
                            String channelValueString = channelValue.get().toString();
                            if (ContainsOnlyNumbers.containsOnlyValidNumbers(channelValueString)) {
                                if (Double.parseDouble(channelValueString) >= 1) {
                                    booleanChannelValue = true;
                                }
                            }
                        } finally {
                            booleanChannelValue = booleanChannelValue != null && booleanChannelValue;
                            boolean conditionMet = booleanChannelValue == value;
                            if (conditionMet) {
                                if (checkConditions.equals(CheckConditions.OR)) {
                                    conditionOk.set(true);
                                } else if (checkConditions.equals(CheckConditions.XOR)) {
                                    if (xorConditionWasSetToTrue.get() == false) {
                                        xorConditionWasSetToTrue.set(true);
                                        conditionOk.set(true);
                                    } else {
                                        conditionOk.set(false);
                                    }
                                }
                            } else if (conditionMet == false && checkConditions.equals(CheckConditions.AND)) {
                                conditionOk.set(false);
                            }
                        }
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    log.warn("ChannelAddress not available: " + e.getMessage());
                }

            }
        });
        return conditionOk.get();
    }
}

