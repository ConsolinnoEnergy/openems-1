package io.openems.edge.controller.heatnetwork.multipleheatercombined.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The MultipleHeaterCombined controller manages Heater by Temperature. Each heater gets an activation and deactivation
 * Thermometer as well as Temperature. When an allocated activationThermometer is <= the Activation Threshold an allocated Heater
 * activates till the deactivationThermometer reaches the Deactivation Temperature.
 * Disable the Heater and wait till the Activation Temperature is reached again.
 */
public interface MultipleHeaterCombinedController extends OpenemsComponent {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Is the MultipleHeaterCombined Controller ok or did an error occur while Heating.
         *
         * <ul>
         * <li>Interface: MultipleHeaterCombinedController
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

    default boolean errorInHeater() {
        if (this.getHasErrorChannel().value().isDefined()) {
            return this.getHasErrorChannel().value().get();
        } else if (this.getHasErrorChannel().getNextValue().isDefined()) {
            return this.getHasErrorChannel().getNextValue().get();
        } else {
            return false;
        }
    }


    default Channel<Boolean> getIsHeatingChannel() {
        return this.channel(ChannelId.IS_HEATING);
    }

    default void setIsHeating(boolean isHeating) {
        this.getIsHeatingChannel().setNextValue(isHeating);
    }

    /**
     * Getter for the isHeating channel.
     *
     * @return a boolean.
     */
    default boolean isHeating() {
        if (this.getIsHeatingChannel().value().isDefined()) {
            return this.getIsHeatingChannel().value().get();
        } else if (this.getIsHeatingChannel().getNextValue().isDefined()) {
            return this.getIsHeatingChannel().getNextValue().get();
        } else {
            return false;
        }
    }


}
