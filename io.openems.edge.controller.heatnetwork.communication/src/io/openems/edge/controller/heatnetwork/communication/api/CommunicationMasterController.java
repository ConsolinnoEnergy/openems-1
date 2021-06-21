package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface CommunicationMasterController extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * ForceHeating. Sets The CallbackValue of each containing Requests to True.
         */
        FORCE_HEATING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Type of Execution on Fallback.
         */
        EXECUTION_ON_FALLBACK(Doc.of(FallbackHandling.values()).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((WriteChannel<?>) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Maximum allowed Request for the current Communication
         */
        MAXIMUM_REQUESTS(Doc.of(OpenemsType.INTEGER)),
        /**
         * Set the Maximum allowed Requests
         */
        SET_MAXIMUM_REQUESTS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Is Extra Heat Requests by Remote Components
         */
        CURRENT_REQUESTS(Doc.of(OpenemsType.INTEGER));
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
     * Getter Method for the ForceHeating Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Boolean> getForceHeatingChannel() {
        return this.channel(ChannelId.FORCE_HEATING);
    }

    /**
     * Sets the ForceHeating Channel.
     *
     * @param autoRun the value for ForceHeating channel
     */
    default void setForceHeating(boolean autoRun) {
        this.getForceHeatingChannel().setNextValue(autoRun);
    }

    /**
     * Gets the value of the ForceHeating Channel.
     *
     * @return the Value of ForceHeating
     */
    default boolean getForceHeating() {
        return this.getForceHeatingChannel().value().orElse(this.getForceHeatingChannel().getNextValue().orElse(false));
    }

    /**
     * Returns the ExecutionOnFallback Channel.
     *
     * @return the channel.
     */
    default WriteChannel<FallbackHandling> getExecutionOnFallbackChannel() {
        return this.channel(ChannelId.EXECUTION_ON_FALLBACK);
    }

    /**
     * Get the Value of the ExecutionOnFallback channel.
     *
     * @return the FallbackHandling.
     */
    default FallbackHandling getExecutionOnFallback() {
        return this.getExecutionOnFallbackChannel().value().asEnum();
    }

    /**
     * Setter for the  ExecutionOnFallback Channel.
     *
     * @param logic FallbackHandling value
     */
    default void setFallbackLogic(FallbackHandling logic) {
        this.getExecutionOnFallbackChannel().setNextValue(logic);
    }

    /**
     * Get the MaximumRequest Channel.
     *
     * @return the channel
     */
    default Channel<Integer> getMaximumRequestChannel() {
        return this.channel(ChannelId.MAXIMUM_REQUESTS);
    }

    /**
     * Setter Channel to change Maximum Request Channel if the CommunicationMaster allows it.
     *
     * @return the channel.
     */
    default WriteChannel<Integer> getSetMaximumRequestChannel() {
        return this.channel(ChannelId.SET_MAXIMUM_REQUESTS);
    }

    /**
     * The MaximumRequests channel Value.
     *
     * @return the value
     */
    default int getMaximumRequests() {
        return this.getMaximumRequestChannel().value().orElse(this.getMaximumRequestChannel().getNextValue().orElse(0));
    }

    /**
     * Internal Method to set the Maximum Allowed Requests.
     *
     * @param maximum the new maximum.
     */
    default void setMaximumRequests(int maximum) {
        this.getMaximumRequestChannel().setNextValue(maximum);
    }

    /**
     * Get the CurrentRequests Channel (The Amount of requests handled this turn).
     *
     * @return the channel
     */
    default Channel<Integer> getCurrentRequestsChannel() {
        return this.channel(ChannelId.CURRENT_REQUESTS);
    }

}
