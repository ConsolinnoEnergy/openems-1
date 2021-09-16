package io.openems.edge.heater.decentral.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Heater;

public interface DecentralHeater extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Signal from this heater to the heat network controller to activate the heat network. The heater wants to get
         * heat from the heat network (or not).
         * 
         * <ul>
         *     <li> Interface: DecentralHeater
         *     <li> Type: Boolean
         * </ul>
         */
        REQUEST_HEAT_NETWORK_ACTIVATION(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Signal from the heat network controller to this heater that the heat network is ready. The heater can now 
         * proceed to get heat from the heat network.
         * The heat network controller should write into this channel. This heater will only read from this channel.
         *
         * <ul>
         *     <li> Interface: DecentralHeater
         *     <li> Type: Boolean
         * </ul>
         */
        HEAT_NETWORK_READY_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        /**
         * ToDo: Ich habe keine Ahnung was dieser channel genau macht. Ich hab keinen Controller gefunden der den Wert
         *  verwendet. Wird nicht gebraucht?
         *
         * <ul>
         *     <li> Interface: DecentralHeater
         *     <li> Type: Boolean
         * </ul>
         */
        NEED_MORE_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        
        /* Wird im Moment nicht benutzt. Löschen?
        NEED_MORE_HEAT_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        */

        /**
         * Channel to set the ’force heat’ setting. Is set at startup from config entry.
         * ’force heat’ means the heater does not wait for a response from the heat network controller (= ’true’ in 
         * channel ’HeatNetworkReadySignal’) when it wants to get heat from the heat network.
         *
         * <ul>
         *     <li> Interface: DecentralHeater
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

    //---------------REQUEST_HEAT_NETWORK_ACTIVATION--------------------//
    /**
     * Gets the Channel for {@link ChannelId#REQUEST_HEAT_NETWORK_ACTIVATION}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getRequestHeatNetworkActivationChannel() {
        return this.channel(ChannelId.REQUEST_HEAT_NETWORK_ACTIVATION);
    }

    /**
     * Gets the signal from this heater to the heat network controller to activate the heat network. The heater wants to
     * get heat from the heat network (or not).
     * See {@link ChannelId#REQUEST_HEAT_NETWORK_ACTIVATION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getRequestHeatNetworkActivation() {
        return this.getRequestHeatNetworkActivationChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#REQUEST_HEAT_NETWORK_ACTIVATION} Channel.
     *
     * @param value the next value
     */
    default void _setRequestHeatNetworkActivation(Boolean value) {
        this.getRequestHeatNetworkActivationChannel().setNextValue(value);
    }

    /*
    Wird nicht gebraucht. Benutze ’getRequestHeatNetworkActivation().orElse(false)’ stattdessen, macht genau das gleiche.
    ’nextValue’ muss nicht geprüft werden. DecentralHeater ist ’TOPIC_CYCLE_AFTER_CONTROLLERS’. Alle Controller sind
    nach ’SwitchProcessImage’, also alles was von DecentralHeater in ’nextValue’ geschrieben wird ist wenn die Controller
    dran sind schon in ’value’

    default boolean getNeedHeat() {
        Boolean needHeat = this.getRequestHeatNetworkActivation().get();
        if (needHeat == null) {
            needHeat = this.getRequestHeatNetworkActivationChannel().getNextValue().get();
        }
        return needHeat != null ? needHeat : false;
    }
    */
    //--------------------------------------------//


    //---------------HEAT_NETWORK_READY_SIGNAL--------------------//
    /**
     * Gets the Channel for {@link ChannelId#HEAT_NETWORK_READY_SIGNAL}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHeatNetworkReadySignalChannel() {
        return this.channel(ChannelId.HEAT_NETWORK_READY_SIGNAL);
    }

    /**
     * Set the signal from the heat network controller to this heater that the heat network is ready. The heater can now
     * proceed to get heat from the heat network.
     * See {@link ChannelId#HEAT_NETWORK_READY_SIGNAL}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHeatNetworkReadySignal(Boolean value) throws OpenemsNamedException {
        this.getHeatNetworkReadySignalChannel().setNextWriteValue(value);
    }

    /*
    Wird nicht gebraucht.
    //REWORK at Bastis branch --> DecentralHeater uses Channel directly
    default boolean getNeedHeatEnableSignal() {
        Boolean needHeatEnableSignal = (Boolean) getCurrentChannelValue(this.getHeatNetworkReadySignalChannel());
        if (needHeatEnableSignal == null) {
            needHeatEnableSignal = (Boolean) getNextChannelValue(this.getHeatNetworkReadySignalChannel());
        }
        return needHeatEnableSignal != null ? needHeatEnableSignal : false;
    }
    */
    //--------------------------------------------//


    //---------------NEED_MORE_HEAT--------------------//
    /**
     * ToDo: Wird nicht gebraucht?
     * Gets the Channel for {@link ChannelId#NEED_MORE_HEAT}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getNeedMoreHeatChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT);
    }

    /*
    Wird nicht gebraucht?
    Andere getter und setter mach ich wenn geklärt ist ob der channel gebraucht wird.

    default boolean getNeedMoreHeat() {
        Boolean needMoreHeat = (Boolean) getCurrentChannelValue(this.getNeedMoreHeatChannel());
        if (needMoreHeat == null) {
            needMoreHeat = (Boolean) getNextChannelValue(this.getNeedMoreHeatChannel());
        }
        return needMoreHeat != null ? needMoreHeat : false;
    }
    */
    //--------------------------------------------//

    
    /* Wird im Moment nicht benutzt. Löschen?
    
    //---------------NEED_MORE_HEAT_ENABLE_SIGNAL--------------------//

    default Channel<Boolean> getNeedMoreHeatEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT_ENABLE_SIGNAL);
    }

    default boolean getNeedMoreHeatEnableSignal() {
        Boolean needMoreHeat = (Boolean) getCurrentChannelValue(this.getNeedMoreHeatChannel());
        if (needMoreHeat == null) {
            needMoreHeat = (Boolean) getNextChannelValue(this.getNeedMoreHeatChannel());
        }
        return needMoreHeat != null ? needMoreHeat : false;
    }
    //--------------------------------------------------------------//
    */
    

    //-----------------FORCE_HEAT---------------------------//
    /**
     * Gets the Channel for {@link ChannelId#FORCE_HEAT}.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> getForceHeatChannel() {
        return this.channel(ChannelId.FORCE_HEAT);
    }

    /**
     * Get the ’force heat’ setting. Is set at startup from config entry and can be changed any time with the
     * setForceHeat() method.
     * ’force heat’ means the heater does not wait for a response from the heat network controller (= ’true’ in
     * channel ’HeatNetworkReadySignal’) when it wants to get heat from the heat network.
     * See {@link ChannelId#FORCE_HEAT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getForceHeat() {
        return this.getForceHeatChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#FORCE_HEAT} Channel.
     *
     * @param value the next value
     */
    default void _setForceHeat(Boolean value) {
        this.getForceHeatChannel().setNextValue(value);
    }

    /**
     * Set the ’force heat’ setting. Is set at startup from config entry and can be changed any time with this method.
     * ’force heat’ means the heater does not wait for a response from the heat network controller (= ’true’ in
     * channel ’HeatNetworkReadySignal’) when it wants to get heat from the heat network.
     * See {@link ChannelId#FORCE_HEAT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setForceHeat(Boolean value) throws OpenemsNamedException {
        this.getForceHeatChannel().setNextWriteValue(value);
    }

    /*
    Wird nicht gebraucht. -> löschen
    default boolean getIsForceHeating() {
        Boolean forceHeating = (Boolean) this.getCurrentChannelValue(this.getForceHeatChannel());
        if (forceHeating == null) {
            forceHeating = (Boolean) this.getNextChannelValue(this.getForceHeatChannel());
        }
        return forceHeating != null ? forceHeating : false;
    }
    */

    //---------------------------------------------------//


    /* Vermutlich nicht gebraucht. -> löschen
    default Object getCurrentChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    default Object getNextChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        } else {
            return null;
        }
    }
    */

}
