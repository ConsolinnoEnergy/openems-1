package io.openems.edge.meter.virtual.asymmetric.subtract;

import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.virtual.symmetric.subtract.VirtualSubtractMeter;

import java.util.List;
import java.util.function.BiConsumer;

public class AsymmetricChannelManager extends AbstractChannelListenerManager {

    private final VirtualAsymmetricSubtractMeter parent;

    public AsymmetricChannelManager(VirtualAsymmetricSubtractMeter parent) {
        this.parent = parent;
    }

    /**
     * Called on Component activate().
     *
     * @param minuend     the Minuend Component
     * @param subtrahends the Subtrahend Components
     */
    protected void activate(OpenemsComponent minuend, List<OpenemsComponent> subtrahends) {
        // Minuend
        if (minuend instanceof AsymmetricMeter) {
            // OK
        } else {
            throw new IllegalArgumentException("Minuend [" + minuend.id() + "] is neither a Meter nor a ESS");
        }

        // Subtrahends
        for (OpenemsComponent subtrahend : subtrahends) {
            if (subtrahend instanceof AsymmetricMeter) {
                // OK
            } else {
                throw new IllegalArgumentException("Subtrahend [" + subtrahend.id() + "] is neither a Meter nor a ESS");
            }
        }

        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.ACTIVE_POWER_L1);
        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.ACTIVE_POWER_L2);
        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.ACTIVE_POWER_L3);
        this.activateSubtractInteger(minuend, subtrahends, SymmetricMeter.ChannelId.ACTIVE_POWER);
        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.REACTIVE_POWER_L1);
        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.REACTIVE_POWER_L2);
        this.activateSubtractInteger(minuend, subtrahends, AsymmetricMeter.ChannelId.REACTIVE_POWER_L3);
        //this.parent._setActivePower(this.parent.getActivePowerL1().orElse(0) + this.parent.getActivePowerL2().orElse(0) + this.parent.getActivePowerL3().orElse(0));

    }

    private void activateSubtractInteger(OpenemsComponent minuend, List<OpenemsComponent> subtrahends,
                                         AsymmetricMeter.ChannelId meterChannelId) {
        final BiConsumer<Value<Integer>, Value<Integer>> callback = (oldValue, newValue) -> {
            Integer result = null;

            // Minuend
            if (minuend instanceof AsymmetricMeter) {
                IntegerReadChannel channel = ((AsymmetricMeter) minuend).channel(meterChannelId);
                result = channel.getNextValue().get();
            }

            // Subtrahends
            for (OpenemsComponent subtrahend : subtrahends) {
                if (subtrahend instanceof AsymmetricMeter) {
                    IntegerReadChannel channel = ((AsymmetricMeter) subtrahend).channel(meterChannelId);
                    result = TypeUtils.subtract(result, channel.getNextValue().get());
                }
            }

            IntegerReadChannel channel = this.parent.channel(meterChannelId);
            if (result != null) {
                channel.setNextValue(result * -1);
            }
        };

        // Minuend
        if (minuend instanceof AsymmetricMeter) {
            this.addOnChangeListener(minuend, meterChannelId, callback);
        }

        // Subtrahends
        for (OpenemsComponent subtrahend : subtrahends) {
            if (subtrahend instanceof AsymmetricMeter) {
                this.addOnChangeListener(subtrahend, meterChannelId, callback);
            }
        }
    }

    private void activateSubtractInteger(OpenemsComponent minuend, List<OpenemsComponent> subtrahends,
                                         SymmetricMeter.ChannelId meterChannelId) {
        final BiConsumer<Value<Integer>, Value<Integer>> callback = (oldValue, newValue) -> {
            Integer result = null;

            // Minuend
            if (minuend instanceof AsymmetricMeter) {
                IntegerReadChannel channel = ((AsymmetricMeter) minuend).channel(meterChannelId);
                result = channel.getNextValue().get();
            }

            // Subtrahends
            for (OpenemsComponent subtrahend : subtrahends) {
                if (subtrahend instanceof AsymmetricMeter) {
                    IntegerReadChannel channel = ((AsymmetricMeter) subtrahend).channel(meterChannelId);
                    result = TypeUtils.subtract(result, channel.getNextValue().get());
                }
            }

            IntegerReadChannel channel = this.parent.channel(meterChannelId);
            if (result != null) {
                channel.setNextValue(result * -1);
            }
        };

        // Minuend
        if (minuend instanceof AsymmetricMeter) {
            this.addOnChangeListener(minuend, meterChannelId, callback);
        }

        // Subtrahends
        for (OpenemsComponent subtrahend : subtrahends) {
            if (subtrahend instanceof AsymmetricMeter) {
                this.addOnChangeListener(subtrahend, meterChannelId, callback);
            }
        }
    }

    private void activateSubtractLong(OpenemsComponent minuend, List<OpenemsComponent> subtrahends,
                                      AsymmetricMeter.ChannelId meterChannelId) {
        final BiConsumer<Value<Long>, Value<Long>> callback = (oldValue, newValue) -> {
            Long result = null;

            // Minuend
            if (minuend instanceof AsymmetricMeter) {
                LongReadChannel channel = ((AsymmetricMeter) minuend).channel(meterChannelId);
                result = channel.getNextValue().get();
            }

            // Subtrahends
            for (OpenemsComponent subtrahend : subtrahends) {
                if (subtrahend instanceof AsymmetricMeter) {
                    LongReadChannel channel = ((AsymmetricMeter) subtrahend).channel(meterChannelId);
                    result = TypeUtils.subtract(result, channel.getNextValue().get());
                }
            }

            LongReadChannel channel = this.parent.channel(meterChannelId);
            channel.setNextValue(result);
        };

        // Minuend
        if (minuend instanceof AsymmetricMeter) {
            this.addOnChangeListener(minuend, meterChannelId, callback);

        }

        // Subtrahends
        for (OpenemsComponent subtrahend : subtrahends) {
            if (subtrahend instanceof AsymmetricMeter) {
                this.addOnChangeListener(subtrahend, meterChannelId, callback);

            }
        }
    }

}
