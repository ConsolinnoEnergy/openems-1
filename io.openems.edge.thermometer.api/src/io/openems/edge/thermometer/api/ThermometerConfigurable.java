package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

public interface ThermometerConfigurable extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        ACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        INACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        SET_DEFAULT_ACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        SET_DEFAULT_INACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE));


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
     * Can be used by controller. Set the ActiveTemperature.
     *
     * @param virtualActiveTemperature the new virtualActiveTemperature
     */
    default void setActiveTemperature(int virtualActiveTemperature) {
        this._getActiveTemperature().setNextValue(virtualActiveTemperature);
    }

    /**
     * Can be used by controller. Set the InactiveTemperature.
     * @param virtualInactiveTemperature the new virtualInactiveTemperature
     */
    default void setInactiveTemperature(int virtualInactiveTemperature) {
        this._getInactiveTemperature().setNextValue(virtualInactiveTemperature);
    }

    default Channel<Integer> _getActiveTemperature() {
        return this.channel(ChannelId.ACTIVE_TEMPERATURE);
    }

    default Channel<Integer> _getInactiveTemperature() {
        return this.channel(ChannelId.INACTIVE_TEMPERATURE);
    }

    default WriteChannel<Integer> _getDefaultActiveTemperatureChannel() {
        return this.channel(ChannelId.SET_DEFAULT_ACTIVE_TEMPERATURE);
    }

    default WriteChannel<Integer> _getDefaultInactiveTemperatureChannel() {
        return this.channel(ChannelId.SET_DEFAULT_INACTIVE_TEMPERATURE);
    }

    /**
     * False is ignored since the Thermometer always resets this EnableSignalValue and only true is needed.
     *
     * @param enable enableSignal usually true
     */
    default void setEnableSignal(boolean enable) throws OpenemsError.OpenemsNamedException {
        if (enable) {
            this.getEnableSignal().setNextWriteValueFromObject(true);
        }
    }

    default WriteChannel<Boolean> getEnableSignal() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

}
