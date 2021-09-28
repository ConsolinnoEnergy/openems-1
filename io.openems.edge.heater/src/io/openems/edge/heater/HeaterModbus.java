package io.openems.edge.heater;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeaterModbus extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ENABLE_SIGNAL_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        ENABLE_SIGNAL_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_POWER_LEVEL_KW_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SET_POINT_POWER_LEVE_KW_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_POWER_LEVEL_PERCENT_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SET_POINT_POWER_LEVEL_PERCENT_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_TEMPERATURE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SET_POINT_TEMPERATURE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        READ_EFFECTIVE_POWER_LEVEL_PERCENT_LONG(Doc.of(OpenemsType.LONG)),
        READ_EFFECTIVE_POWER_LEVEL_PERCENT_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        READ_EFFECTIVE_POWER_LEVEL_KW_LONG(Doc.of(OpenemsType.LONG)),
        READ_EFFECTIVE_POWER_LEVEL_KW_DOUBLE(Doc.of(OpenemsType.DOUBLE)),


        FLOW_TEMPERATURE_LONG(Doc.of(OpenemsType.LONG)),
        FLOW_TEMPERATURE_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        RETURN_TEMPERATURE_LONG(Doc.of(OpenemsType.LONG)),
        RETURN_TEMPERATURE_DOUBLE(Doc.of(OpenemsType.DOUBLE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#ENABLE_SIGNAL_BOOLEAN}.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> _getEnableSignalBoolean() {
        return this.channel(ChannelId.ENABLE_SIGNAL_BOOLEAN);
    }

    /**
     * Gets the Channel for {@link ChannelId#ENABLE_SIGNAL_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _getEnableSignalLong() {
        return this.channel(ChannelId.ENABLE_SIGNAL_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_LEVEL_KW_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _getSetPointPowerLevelKwLong() {
        return this.channel(ChannelId.SET_POINT_POWER_LEVEL_KW_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ENABLE_SIGNAL_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> _getSetPointPowerLevelKwDouble() {
        return this.channel(ChannelId.SET_POINT_POWER_LEVE_KW_DOUBLE);
    }


    default WriteChannel<Long> _getSetPointPowerLevelPercentLong() {
        return this.channel(ChannelId.SET_POINT_POWER_LEVEL_PERCENT_LONG);
    }
    default WriteChannel<Double> _getSetPointPowerLevelPercentDouble() {
        return this.channel(ChannelId.SET_POINT_POWER_LEVEL_PERCENT_DOUBLE);
    }

    default WriteChannel<Long> _getSetPointTemperatureLong() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE_LONG);
    }
    default WriteChannel<Double> _getSetPointTemperatureDouble() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE_DOUBLE);
    }

    default Channel<Long> _getReadEffectivePowerLevelPercentLong() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER_LEVEL_PERCENT_LONG);
    }
    default Channel<Double> _getReadEffectivePowerLevelPercentDouble() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER_LEVEL_PERCENT_DOUBLE);
    }
    default Channel<?> _hasEffectivePowerPercent() {
        return HeaterModbus.getValueDefinedChannel(this._getReadEffectivePowerLevelPercentLong(), this._getReadEffectivePowerLevelPercentDouble());
    }

    default Channel<Long> _getReadEffectivePowerLevelKwLong() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER_LEVEL_KW_LONG);
    }
    default Channel<Double> _getReadEffectivePowerLevelKwDouble() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER_LEVEL_KW_DOUBLE);
    }
    default Channel<?> _hasEffectivePowerKw() {
        return HeaterModbus.getValueDefinedChannel(this._getReadEffectivePowerLevelKwLong(), this._getReadEffectivePowerLevelKwDouble());
    }

    default Channel<Long> _getFlowTempLong() {
        return this.channel(ChannelId.FLOW_TEMPERATURE_LONG);
    }
    default Channel<Double> _getFlowTempDouble() {
        return this.channel(ChannelId.FLOW_TEMPERATURE_DOUBLE);
    }
    default Channel<?> _hasFlowTemp() {
        return HeaterModbus.getValueDefinedChannel(this._getFlowTempLong(), this._getFlowTempDouble());
    }

    default Channel<Long> _getReturnTempLong() {
        return this.channel(ChannelId.RETURN_TEMPERATURE_LONG);
    }
    default Channel<Double> _getReturnTempDouble() {
        return this.channel(ChannelId.RETURN_TEMPERATURE_DOUBLE);
    }
    default Channel<?> _hasReturnTemp() {
        return HeaterModbus.getValueDefinedChannel(this._getReturnTempLong(), this._getReturnTempDouble());
    }

    static Channel<?> getValueDefinedChannel(Channel<?> firstChannel, Channel<?> secondChannel) {
        if (firstChannel.getNextValue().isDefined()) {
            return firstChannel;
        } else if (secondChannel.getNextValue().isDefined()) {
            return secondChannel;
        } else {
            return null;
        }
    }
}
