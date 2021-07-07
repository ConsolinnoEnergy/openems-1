package io.openems.edge.controller.heatnetwork.multipleheatercombined.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;
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
        /**
         * Has an Error occurred while executing the heatRoutine within the MultipleHeaterCombined.
         * <ul>
         *     <li>Interface: MultipleHeaterCombinedController
         *     <li>Type: Boolean
         * </ul>
         */
        ERROR(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Is the Controller Heating up any Heater.
         * <ul>
         *     <li> Interface: MultipleHeaterCombinedController
         *     <li> Type: Boolean
         * </ul>
         */
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

    /**
     * Get the Ok Channel.
     *
     * @return the channel
     */
    default Channel<Boolean> getOkChannel() {
        return this.channel(ChannelId.OK);
    }

    /**
     * Set the Ok Channel by a boolean value.
     *
     * @param ok is everything's ok in the heating routine or not.
     */

    default void setIsOk(boolean ok) {
        this.getOkChannel().setNextValue(ok);
        this.getHasErrorChannel().setNextValue(!ok);
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
     * Setter for the Error Channel.
     *
     * @param error true or false
     */
    default void setHasError(boolean error) {
        this.getHasErrorChannel().setNextValue(error);
        this.getOkChannel().setNextValue(!error);
    }

    /**
     * Has an error occurred within the Controller.
     *
     * @return a boolean.
     */
    default boolean errorInHeater() {
        if (this.getHasErrorChannel().value().isDefined()) {
            return this.getHasErrorChannel().value().get();
        } else if (this.getHasErrorChannel().getNextValue().isDefined()) {
            return this.getHasErrorChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    /**
     * Is Heating Channel.
     *
     * @return the Channel
     */
    default Channel<Boolean> getIsHeatingChannel() {
        return this.channel(ChannelId.IS_HEATING);
    }

    /**
     * Setter for the isHeating channel.
     *
     * @param isHeating true or false
     */
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
