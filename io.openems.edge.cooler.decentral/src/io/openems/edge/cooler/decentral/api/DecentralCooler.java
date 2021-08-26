package io.openems.edge.cooler.decentral.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.Cooler;

public interface DecentralCooler extends Cooler {

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
    default Channel<Boolean> getNeedCoolChannel() {
        return this.channel(ChannelId.NEED_COOL);
    }

    default boolean getNeedCool() {
        Boolean needCool = (Boolean) getCurrentChannelValue(this.getNeedCoolChannel());
        if (needCool == null) {
            needCool = (Boolean) getNextChannelValue(this.getNeedCoolChannel());
        }
        return needCool != null ? needCool : false;
    }
    //--------------------------------------------//

    //---------------NEED_COOL_ENABLE_SIGNAL--------------------//
    default WriteChannel<Boolean> getNeedCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_COOL_ENABLE_SIGNAL);
    }
    //REWORK at Bastis branch --> DecentralCooler uses Channel directly
    default boolean getNeedCoolEnableSignal() {
        Boolean needCoolEnableSignal = (Boolean) getCurrentChannelValue(this.getNeedCoolEnableSignalChannel());
        if (needCoolEnableSignal == null) {
            needCoolEnableSignal = (Boolean) getNextChannelValue(this.getNeedCoolEnableSignalChannel());
        }
        return needCoolEnableSignal != null ? needCoolEnableSignal : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_COOL--------------------//
    default Channel<Boolean> getNeedMoreCoolChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL);
    }

    default boolean getNeedMoreCool() {
        Boolean needMoreCool = (Boolean) getCurrentChannelValue(this.getNeedMoreCoolChannel());
        if (needMoreCool == null) {
            needMoreCool = (Boolean) getNextChannelValue(this.getNeedMoreCoolChannel());
        }
        return needMoreCool != null ? needMoreCool : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_COOL_ENABLE_SIGNAL--------------------//

    default Channel<Boolean> getNeedMoreCoolEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_COOL);
    }

    default boolean getNeedMoreCoolEnableSignal() {
        Boolean needMoreCool = (Boolean) getCurrentChannelValue(this.getNeedMoreCoolChannel());
        if (needMoreCool == null) {
            needMoreCool = (Boolean) getNextChannelValue(this.getNeedMoreCoolChannel());
        }
        return needMoreCool != null ? needMoreCool : false;
    }
    //--------------------------------------------------------------//


    //-----------------FORCE_COOL---------------------------//

    default WriteChannel<Boolean> getForceCoolChannel() {
        return this.channel(ChannelId.FORCE_COOL);
    }

    default void setForceCooling(boolean forceCooling) {
        this.getForceCoolChannel().setNextValue(forceCooling);
    }

    default boolean getIsForceCooling() {
        Boolean forceCooling = (Boolean) this.getCurrentChannelValue(this.getForceCoolChannel());
        if (forceCooling == null) {
            forceCooling = (Boolean) this.getNextChannelValue(this.getForceCoolChannel());
        }
        return forceCooling != null ? forceCooling : false;
    }

    //---------------------------------------------------//


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

}
