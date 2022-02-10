package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature for ConditionApplier.
 * It allows Components or the User via REST
 * to set new Default Active and inactive Values.
 * Those will be stored in the config and written as the nextValue.
 * The Values will be applied to the e.g. {@link io.openems.edge.utility.conditionapplier.multiple.MultipleBooleanConditionApplierImpl}.
 */
public interface ConditionApplier extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Sets the default Active Value e.g. if conditions are met -> write this value to "answerChannel".
         *
         * <ul>
         * <li>Interface: ConditionApplier
         * <li>Type: String
         * </ul>
         */
        SET_DEFAULT_ACTIVE_VALUE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        /**
         * Sets the default Inactive Value e.g. if conditions are NOT met -> write this value to "answerChannel".
         *
         * <ul>
         * <li>Interface: ConditionApplier
         * <li>Type: String
         * </ul>
         */
        SET_DEFAULT_INACTIVE_VALUE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the {@link ChannelId#SET_DEFAULT_ACTIVE_VALUE} ChannelId. Usually called internally by a ConditionApplier
     * to check for new WriteValues.
     *
     * @return the Channel
     */
    default WriteChannel<String> _getDefaultActiveValueChannel() {
        return this.channel(ChannelId.SET_DEFAULT_ACTIVE_VALUE);
    }

    /**
     * Gets the {@link ChannelId#SET_DEFAULT_INACTIVE_VALUE} ChannelId. Usually called internally by a ConditionApplier
     * to check for new WriteValues.
     *
     * @return the Channel
     */
    default WriteChannel<String> _getDefaultInactiveValueChannel() {
        return this.channel(ChannelId.SET_DEFAULT_INACTIVE_VALUE);
    }


    /**
     * Sets  the new default Active Value of the ConditionApplier.
     *
     * @param activeValue the new ActiveValue as a String
     * @throws OpenemsError.OpenemsNamedException if setNextWriteValue fails.
     */

    default void setDefaultActiveValue(String activeValue) throws OpenemsError.OpenemsNamedException {
        this._getDefaultActiveValueChannel().setNextWriteValueFromObject(activeValue);
    }

    /**
     * Sets  the new default inactive Value of the ConditionApplier.
     *
     * @param inactiveValue the new inactiveValue as a String
     * @throws OpenemsError.OpenemsNamedException if setNextWriteValue fails.
     */

    default void setDefaultInactiveValue(String inactiveValue) throws OpenemsError.OpenemsNamedException {
        this._getDefaultInactiveValueChannel().setNextWriteValueFromObject(inactiveValue);
    }
}

