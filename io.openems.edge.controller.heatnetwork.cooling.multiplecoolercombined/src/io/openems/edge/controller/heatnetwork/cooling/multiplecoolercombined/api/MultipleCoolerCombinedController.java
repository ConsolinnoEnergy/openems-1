package io.openems.edge.controller.heatnetwork.cooling.multiplecoolercombined.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The MultipleCoolerCombined controller manages Cooler by Temperature. Each heater gets an activation and deactivation
 * Thermometer as well as Temperature. When an allocated activationThermometer is <= the Activation Threshold an allocated Cooler
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
        IS_HEATING(Doc.of(OpenemsType.BOOLEAN));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    default Channel<Boolean> getOkChannel() {
        return this.channel(ChannelId.OK);
    }

    default void setIsOk(boolean ok) {
        this.getOkChannel().setNextValue(ok);
        this.getHasErrorChannel().setNextValue(!ok);
    }

    default boolean getIsOk() {
        if (this.getOkChannel().value().isDefined()) {
            return this.getOkChannel().value().get();
        } else if (this.getOkChannel().getNextValue().isDefined()) {
            return this.getOkChannel().getNextValue().get();
        } else {
            return true;
        }
    }

    default Channel<Boolean> getHasErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    default void setHasError(boolean error) {
        this.getHasErrorChannel().setNextValue(error);
        this.getOkChannel().setNextValue(!error);
    }

    default boolean errorInCooler() {
        if (this.getHasErrorChannel().value().isDefined()) {
            return this.getHasErrorChannel().value().get();
        } else if (this.getHasErrorChannel().getNextValue().isDefined()) {
            return this.getHasErrorChannel().getNextValue().get();
        } else {
            return false;
        }
    }


    default Channel<Boolean> getIsCoolingChannel() {
        return this.channel(ChannelId.IS_HEATING);
    }

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
