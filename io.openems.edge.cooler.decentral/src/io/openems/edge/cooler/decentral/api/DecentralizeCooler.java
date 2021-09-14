package io.openems.edge.cooler.decentral.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.api.Cooler;

/**
 * The DecentralizeCooler interface, extending the Cooler. It Provides the ability, equivalent to the Decentral Heater,
 * to cool a Storage, by asking for cooling and open /activate an HydraulicComponent depending on the temperature.
 */
public interface DecentralizeCooler extends Cooler {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        NEED_COOL(Doc.of(OpenemsType.BOOLEAN)),
        NEED_COOL_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        NEED_MORE_COOL(Doc.of(OpenemsType.BOOLEAN)),
        NEED_MORE_COOL_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

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
     * Get the NeedCool channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedCoolChannel() {
        return this.channel(ChannelId.NEED_COOL);
    }

    /**
     * Get the NeedCool ChannelValue.
     *
     * @return The Value or else false.
     */
    default boolean getNeedCool() {
        Boolean needCool = (Boolean) this.getCurrentChannelValue(this.getNeedCoolChannel());
        if (needCool == null) {
            needCool = (Boolean) this.getNextChannelValue(this.getNeedCoolChannel());
        }
        return needCool != null ? needCool : false;
    }
    //--------------------------------------------//

    //---------------NEED_COOL_ENABLE_SIGNAL--------------------//

    /**
     * Get the NeedCool Enable Signal Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Boolean> getNeedCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_COOL_ENABLE_SIGNAL);
    }

    //--------------------------------------------//

    //---------------NEED_MORE_COOL--------------------//

    /**
     * Get the NeedMoreCool Channel.
     *
     * @return the Channel.
     */
    default Channel<Boolean> getNeedMoreCoolChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL);
    }

    //--------------------------------------------//

    //---------------NEED_MORE_COOL_ENABLE_SIGNAL--------------------//

    /**
     * Gets the NeedMoreCool Enable Signal Channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> getNeedMoreCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL_ENABLE_SIGNAL);
    }

    //--------------------------------------------------------------//


    //-----------------FORCE_COOL---------------------------//

    /**
     * Get the ForceCoolChannel.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getForceCoolChannel() {
        return this.channel(ChannelId.FORCE_COOL);
    }

    /**
     * Gets Is Force Cooling value.
     *
     * @return the value.
     */
    default boolean getIsForceCooling() {
        Boolean forceCooling = (Boolean) this.getCurrentChannelValue(this.getForceCoolChannel());
        if (forceCooling == null) {
            forceCooling = (Boolean) this.getNextChannelValue(this.getForceCoolChannel());
        }
        return forceCooling != null ? forceCooling : false;
    }

    //---------------------------------------------------//

    /**
     * Gets the current Value of a given Channel. If defined.
     *
     * @param requestedChannel the requested Channel.
     * @return the value or null.
     */
    default Object getCurrentChannelValue(Channel<?> requestedChannel) {
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
    default Object getNextChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        } else {
            return null;
        }
    }

}
