package io.openems.edge.controller.heatnetwork.multipleheatercombined.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The MultipleCoolerCombined controller manages Cooler by Temperature. Each heater gets an activation and deactivation
 * Thermometer as well as Temperature. When an allocated activationThermometer is >= the Activation Threshold an allocated Cooler
 * activates till the deactivationThermometer reaches the Deactivation Temperature.
 * Disable the Cooler and wait till the Activation Temperature is reached again.
 */
public interface MultipleCoolerCombinedController extends OpenemsComponent {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Is the MultipleCoolerCombined Controller ok or did an error occur while Cooling.
         *
         * <ul>
         * <li>Interface: MultipleCoolerCombinedController
         * <li>Type: Boolean
         * </ul>
         */
        OK(Doc.of(OpenemsType.BOOLEAN)),
        ERROR(Doc.of(OpenemsType.BOOLEAN)),
        IS_COOLING(Doc.of(OpenemsType.BOOLEAN));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Get the OK Channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> getOkChannel() {
        return this.channel(ChannelId.OK);
    }

    /**
     * Sets the value of the Ok Channel.
     *
     * @param ok is everything ok or did an error occur.
     */
    default void setIsOk(boolean ok) {
        this.getOkChannel().setNextValue(ok);
        this.getHasErrorChannel().setNextValue(!ok);
    }

    /**
     * Get the channelValue of OK.
     *
     * @return the value or if none present true.
     */
    default boolean getIsOk() {
        return this.getOkChannel().value().orElse(this.getOkChannel().getNextValue().orElse(true));
    }

    /**
     * Get the Error Channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> getHasErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    /**
     * Sets the Error value for the Error channel.
     *
     * @param error has a Cooler an error.
     */
    default void setHasError(boolean error) {
        this.getHasErrorChannel().setNextValue(error);
        this.getOkChannel().setNextValue(!error);
    }

    /**
     * Get the value of the Error channel.
     *
     * @return the value or else false.
     */
    default boolean errorInCooler() {
        return this.getHasErrorChannel().value().orElse(this.getHasErrorChannel().getNextValue().orElse(false));
    }

    /**
     * Get the IsCooling channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> getIsCoolingChannel() {
        return this.channel(ChannelId.IS_COOLING);
    }

    /**
     * Sets the value for the IsCooling channel.
     *
     * @param isCooling the value.
     */
    default void setIsCooling(boolean isCooling) {
        this.getIsCoolingChannel().setNextValue(isCooling);
    }

    /**
     * Getter for the isCooling channel.
     *
     * @return a boolean.
     */
    default boolean isCooling() {
        if (this.getIsCoolingChannel().value().isDefined()) {
            return this.getIsCoolingChannel().value().get();
        } else if (this.getIsCoolingChannel().getNextValue().isDefined()) {
            return this.getIsCoolingChannel().getNextValue().get();
        } else {
            return false;
        }
    }


}
