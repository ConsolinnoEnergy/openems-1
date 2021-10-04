package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

/**
 * A Configurable Thermometer. It allows the User to set up 2 Static Temperature Values.
 * On Enable -> Write the Active Temperature, otherwise the inactive Temperature.
 * Those Active and Inactive Temperatures can be set and updated via {@link ChannelId#SET_DEFAULT_ACTIVE_TEMPERATURE}
 * and {@link ChannelId#SET_DEFAULT_INACTIVE_TEMPERATURE}.
 */
public interface ThermometerConfigurable extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Enable Signal.
         * Set the EnableSignal.
         * <ul>
         * <li>Interface: ThermometerConfigurable
         * <li>Type: Boolean
         * <li>Unit: none
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * Active Temperature Value.
         * The Value the Thermometer will write into it's {@link Thermometer.ChannelId#TEMPERATURE} if EnableSignal is true.
         * <ul>
         * <li>Interface: ThermometerConfigurable
         * <li>Type: Integer
         * <li>Unit: DeciDegreeCelsius
         * </ul>
         */
        ACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        /**
         * Inactive Temperature Value.
         * The Value the Thermometer will write into it's {@link Thermometer.ChannelId#TEMPERATURE} if not Enabled.
         * (And the Component should use an InactiveTemperature)
         * <ul>
         * <li>Interface: ThermometerConfigurable
         * <li>Type: Integer
         * <li>Unit: DeciDegreeCelsius
         * </ul>
         */
        INACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        /**
         * Set Active Temperature.
         * Set up a new Default active Temperature. This will be updated in the Config.
         * <ul>
         * <li>Interface: ThermometerConfigurable
         * <li>Type: Integer
         * <li>Unit: DeciDegreeCelsius
         * </ul>
         */
        SET_DEFAULT_ACTIVE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        /**
         * Set Inactive Temperature.
         * Set up a new Default active Temperature. This will be updated in the Config.
         * <ul>
         * <li>Interface: ThermometerConfigurable
         * <li>Type: Integer
         * <li>Unit: DeciDegreeCelsius
         * </ul>
         */
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
        this._getActiveTemperatureChannel().setNextValue(virtualActiveTemperature);
    }

    /**
     * Can be used by controller. Set the InactiveTemperature.
     *
     * @param virtualInactiveTemperature the new virtualInactiveTemperature
     */
    default void setInactiveTemperature(int virtualInactiveTemperature) {
        this._getInactiveTemperatureChannel().setNextValue(virtualInactiveTemperature);
    }

    /**
     * Internal Method, to get the ActiveTemperatureValue.
     *
     * @return the Channel
     */
    default Channel<Integer> _getActiveTemperatureChannel() {
        return this.channel(ChannelId.ACTIVE_TEMPERATURE);
    }

    /**
     * Internal Method, to get the ActiveTemperatureValue.
     *
     * @return the Channel
     */
    default Channel<Integer> _getInactiveTemperatureChannel() {
        return this.channel(ChannelId.INACTIVE_TEMPERATURE);
    }

    /**
     * Get the Channel to set the Default Active Temperature (SetNextWriteValue).
     *
     * @return the channel
     */
    default WriteChannel<Integer> _getDefaultActiveTemperatureChannel() {
        return this.channel(ChannelId.SET_DEFAULT_ACTIVE_TEMPERATURE);
    }

    /**
     * Get the Channel to set the Default Inactive Temperature (SetNextWriteValue).
     *
     * @return the channel
     */
    default WriteChannel<Integer> _getDefaultInactiveTemperatureChannel() {
        return this.channel(ChannelId.SET_DEFAULT_INACTIVE_TEMPERATURE);
    }

    /**
     * False is ignored since the Thermometer always resets this EnableSignalValue and only true is needed.
     * Additionally. Do not write false to disable it. If other Components/Controller want to enable -> Overwrite potential.
     * @param enable enableSignal usually true
     */
    default void setEnableSignal(boolean enable) throws OpenemsError.OpenemsNamedException {
        if (enable) {
            this.getEnableSignalChannel().setNextWriteValueFromObject(true);
        }
    }

    /**
     * Get the EnableSignalChannel.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getEnableSignalChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

}
