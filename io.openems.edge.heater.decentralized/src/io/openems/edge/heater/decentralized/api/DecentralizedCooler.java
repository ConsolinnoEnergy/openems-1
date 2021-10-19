package io.openems.edge.heater.decentralized.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.api.Cooler;

/**
 * The DecentralizeCooler interface, extending the Cooler. It Provides the ability, equivalent to the Decentralized Heater,
 * to cool a Storage, by asking for cooling and open /activate an HydraulicComponent depending on the temperature.
 */
public interface DecentralizedCooler extends Cooler {

    // Default SetPointTemperature for DecentralizedCooler, if SetPoint is not defined.
    int DEFAULT_SET_POINT_TEMPERATURE = 200;

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Set by the DecentralizedCooler itself.
         * On EnableSignal ->{@link io.openems.edge.heater.api.Heater.ChannelId#ENABLE_SIGNAL} ask for Cooling and await
         * the {@link #NEED_COOL_ENABLE_SIGNAL} for logic Execution.
         *
         * <ul>
         *     <li> Interface: DecentralizedCooler
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_COOL(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * This will be set by e.g. a Centralized Edge System via REST.
         * If this is set to true the Logic of the DecentralizedCooler executes.
         * <ul>
         *     <li> Interface: DecentralizedCooler
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_COOL_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set by the DecentralizedCooler itself.
         * On Execution of Cooler Logic -> watch a Thermometer Temperature with the SetPoint of the Decentralized Heater.
         * If the Temperature exceeds the SetPoint -> Ask for more cooling.
         *
         * <ul>
         *     <li> Interface: DecentralizedCooler
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_MORE_COOL(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * This will be set by e.g. a Centralized Edge System via REST.
         * Monitor if the Centralized Edge System reacted to the request of {@link #NEED_MORE_COOL}
         *
         * <ul>
         *     <li> Interface: DecentralizedCooler
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_MORE_COOL_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * If this is set to true -> Execute Logic, no matter if a {@link #NEED_COOL_ENABLE_SIGNAL} was set or not.
         *
         * <ul>
         *     <li> Interface: DecentralizedCooler
         *     <li> Type: Boolean
         * </ul>
         */
        FORCE_COOL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
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

    //---------------NEED_COOL--------------------//

    /**
     * Get the Channel for  {@link ChannelId#NEED_COOL}.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedCoolChannel() {
        return this.channel(ChannelId.NEED_COOL);
    }

    /**
     * Get the Value of the {@link ChannelId#NEED_COOL} channel.
     *
     * @return true if NeedHeat is set to true.
     */
    default boolean getNeedCool() {
        Boolean needCool = (Boolean) this._getCurrentChannelValue(this.getNeedCoolChannel());
        if (needCool == null) {
            needCool = (Boolean) this._getNextChannelValue(this.getNeedCoolChannel());
        }
        return needCool != null ? needCool : false;
    }
    //--------------------------------------------//

    //---------------NEED_COOL_ENABLE_SIGNAL--------------------//

    /**
     * Get the Channel for {@link ChannelId#NEED_COOL_ENABLE_SIGNAL}.
     * Only for monitoring.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getNeedCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_COOL_ENABLE_SIGNAL);
    }

    //--------------------------------------------//

    //---------------NEED_MORE_COOL--------------------//

    /**
     * Get the Channel for {@link ChannelId#NEED_MORE_COOL}.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedMoreCoolChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL);
    }

    /**
     * Get the NeedMoreCool value.
     * Only for monitoring.
     *
     * @return a boolean.
     */
    default boolean getNeedMoreCool() {
        Boolean needMoreCool = (Boolean) this._getCurrentChannelValue(this.getNeedMoreCoolChannel());
        if (needMoreCool == null) {
            needMoreCool = (Boolean) this._getNextChannelValue(this.getNeedMoreCoolChannel());
        }
        return needMoreCool != null ? needMoreCool : false;
    }

    //--------------------------------------------//

    //---------------NEED_MORE_COOL_ENABLE_SIGNAL--------------------//

    /**
     * Get the Channel for {@link ChannelId#NEED_MORE_COOL_ENABLE_SIGNAL}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getNeedMoreCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL_ENABLE_SIGNAL);
    }

    /**
     * Get the NeedMoreCoolEnableSignal value.
     * Only for monitoring.
     *
     * @return a boolean.
     */
    default boolean getNeedMoreCoolEnableSignal() {
        Boolean needMoreCoolEnableSignal = (Boolean) this._getCurrentChannelValue(this.getNeedMoreCoolEnableSignalChannel());
        if (needMoreCoolEnableSignal == null) {
            needMoreCoolEnableSignal = (Boolean) this._getNextChannelValue(this.getNeedMoreCoolEnableSignalChannel());
        }
        return needMoreCoolEnableSignal != null ? needMoreCoolEnableSignal : false;
    }

    //--------------------------------------------------------------//


    //-----------------FORCE_COOL---------------------------//

    /**
     * Get the Channel for {@link ChannelId#FORCE_COOL}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getForceCoolChannel() {
        return this.channel(ChannelId.FORCE_COOL);
    }

    /**
     * Gets Is Force Cooling value.
     *
     * @return a boolean.
     */
    default boolean getIsForceCooling() {
        Boolean forceCooling = (Boolean) this._getCurrentChannelValue(this.getForceCoolChannel());
        if (forceCooling == null) {
            forceCooling = (Boolean) this._getNextChannelValue(this.getForceCoolChannel());
        }
        return forceCooling != null ? forceCooling : false;
    }

    /**
     * Sets the Value for Force cooling.
     *
     * @param forceCooling true to enable force cooling.
     */
    default void setForceCooling(boolean forceCooling) {
        this.getForceCoolChannel().setNextValue(forceCooling);
    }


    //---------------------------------------------------//

    /**
     * Gets the current Value of a given Channel. If defined.
     *
     * @param requestedChannel the requested Channel.
     * @return the value or null.
     */
    default Object _getCurrentChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    /**
     * Get ne next Value of a given Channel. If defined.
     *
     * @param requestedChannel the Channel to get the next Value from.
     * @return the channel Value or null.
     */
    default Object _getNextChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        } else {
            return null;
        }
    }

}
