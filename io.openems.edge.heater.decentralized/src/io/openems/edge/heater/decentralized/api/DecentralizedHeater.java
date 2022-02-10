package io.openems.edge.heater.decentralized.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.api.Heater;


/**
 * The DecentralizeCooler interface, extending the Heater. It Provides the ability,
 * to heat up a Storage, by asking for heating and open /activate an HydraulicComponent depending on the temperature.
 */
public interface DecentralizedHeater extends Heater {

    // Default SetPointTemperature for DecentralizedHeater, if SetPoint is not defined.
    int DEFAULT_SET_POINT_TEMPERATURE = 500;

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Set by the DecentralizedHeater itself.
         * On EnableSignal ->{@link io.openems.edge.heater.api.Heater.ChannelId#ENABLE_SIGNAL} ask for Heating and await
         * the {@link #NEED_HEAT_ENABLE_SIGNAL} for logic Execution.
         *
         * <ul>
         *     <li> Interface: DecentralizedHeater
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * This will be set by e.g. a Centralized Edge System via REST.
         * If this is set to true the Logic of the DecentralizedHeater executes.
         * <ul>
         *     <li> Interface: DecentralizedHeater
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_HEAT_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set by the DecentralizedHeater itself.
         * On Execution of Heater Logic -> watch a Thermometer Temperature with the SetPoint of the Decentralized Heater.
         * If the Temperature falls below the SetPoint -> Ask for more Heating.
         *
         * <ul>
         *     <li> Interface: DecentralizedHeater
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_MORE_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * This will be set by e.g. a Centralized Edge System via REST.
         * Monitor if the Centralized Edge System reacted to the request of {@link #NEED_MORE_HEAT}
         *
         * <ul>
         *     <li> Interface: DecentralizedHeater
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_MORE_HEAT_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * If this is set to true -> Execute Logic, no matter if a {@link #NEED_HEAT_ENABLE_SIGNAL} was set or not.
         *
         * <ul>
         *     <li> Interface: DecentralizedHeater
         *     <li> Type: Boolean
         * </ul>
         */
        FORCE_HEAT(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    //---------------NEED_HEAT--------------------//

    /**
     * Get the Channel for {@link ChannelId#NEED_HEAT}.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedHeatChannel() {
        return this.channel(ChannelId.NEED_HEAT);
    }

    /**
     * Get the Value of the {@link ChannelId#NEED_HEAT} channel.
     *
     * @return true if NeedHeat is set to true.
     */
    default boolean getNeedHeat() {
        Boolean needHeat = (Boolean) this._getCurrentChannelValue(this.getNeedHeatChannel());
        if (needHeat == null) {
            needHeat = (Boolean) this._getNextChannelValue(this.getNeedHeatChannel());
        }
        return needHeat != null ? needHeat : false;
    }
    //--------------------------------------------//

    //---------------NEED_HEAT_ENABLE_SIGNAL--------------------//
    /**
     * Get the Channel for {@link ChannelId#NEED_HEAT_ENABLE_SIGNAL}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getNeedHeatEnableSignalChannel() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL);
    }

    /**
     * Get the Value of the {@link ChannelId#NEED_HEAT_ENABLE_SIGNAL} channel.
     * Only for monitoring.
     * @return true if NeedHeat is set to true.
     */
    default boolean getNeedHeatEnableSignal() {
        Boolean needHeatEnableSignal = (Boolean) this._getCurrentChannelValue(this.getNeedHeatEnableSignalChannel());
        if (needHeatEnableSignal == null) {
            needHeatEnableSignal = (Boolean) this._getNextChannelValue(this.getNeedHeatEnableSignalChannel());
        }
        return needHeatEnableSignal != null ? needHeatEnableSignal : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_HEAT--------------------//
    /**
     * Get the Channel for {@link ChannelId#NEED_MORE_HEAT}.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedMoreHeatChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT);
    }

    /**
     * Get the NeedMoreHeat value.
     * Only for monitoring.
     * @return a boolean.
     */
    default boolean getNeedMoreHeat() {
        Boolean needMoreHeat = (Boolean) this._getCurrentChannelValue(this.getNeedMoreHeatChannel());
        if (needMoreHeat == null) {
            needMoreHeat = (Boolean) this._getNextChannelValue(this.getNeedMoreHeatChannel());
        }
        return needMoreHeat != null ? needMoreHeat : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_HEAT_ENABLE_SIGNAL--------------------//
    /**
     * Get the Channel for {@link ChannelId#NEED_MORE_HEAT_ENABLE_SIGNAL}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getNeedMoreHeatEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT_ENABLE_SIGNAL);
    }
    /**
     * Get the NeedMoreHeatEnableSignal value.
     * Only for monitoring.
     * @return a boolean.
     */
    default boolean getNeedMoreHeatEnableSignal() {
        Boolean needMoreHeatEnableSignal = (Boolean) this._getCurrentChannelValue(this.getNeedMoreHeatEnableSignalChannel());
        if (needMoreHeatEnableSignal == null) {
            needMoreHeatEnableSignal = (Boolean) this._getNextChannelValue(this.getNeedMoreHeatEnableSignalChannel());
        }
        return needMoreHeatEnableSignal != null ? needMoreHeatEnableSignal : false;
    }
    //--------------------------------------------------------------//


    //-----------------FORCE_HEAT---------------------------//
    /**
     * Get the Channel for {@link ChannelId#FORCE_HEAT}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getForceHeatChannel() {
        return this.channel(ChannelId.FORCE_HEAT);
    }

    /**
     * Sets the Value for Force Heating.
     * @param forceHeating true to enable force heating.
     */
    default void setForceHeating(boolean forceHeating) {
        this.getForceHeatChannel().setNextValue(forceHeating);
    }

    /**
     * Get the ForceHeating value.
     * @return a boolean.
     */
    default boolean getIsForceHeating() {
        Boolean forceHeating = (Boolean) this._getCurrentChannelValue(this.getForceHeatChannel());
        if (forceHeating == null) {
            forceHeating = (Boolean) this._getNextChannelValue(this.getForceHeatChannel());
        }
        return forceHeating != null ? forceHeating : false;
    }

    //---------------------------------------------------//

    /**
     * Get the value of a requested Channel.
     * @param requestedChannel the Channel
     * @return the object value or null
     */
    default Object _getCurrentChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    /**
     * Get the next value of a requested Channel.
     * @param requestedChannel the Channel
     * @return the object value or null
     */
    default Object _getNextChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        } else {
            return null;
        }
    }

}
