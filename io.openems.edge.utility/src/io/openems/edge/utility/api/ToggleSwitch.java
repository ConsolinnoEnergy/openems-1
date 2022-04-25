package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Interface for the ToggleSwitch.
 * The {@link ToggleSwitch} receives a Signal Active (== Button is pressed and held down)
 * On release -> toggle to another state.
 * E.g. StartState == State A.
 * On {@link ChannelId#SIGNAL_ACTIVE} == true and followed by false switch state to State B
 * When this procedure is repeated it toggles the state back from State B to A.
 * The {@link io.openems.edge.utility.toogleswitch.ToggleState} (A == true/active B == false/inactive) will be stored in {@link ChannelId#TOGGLE_STATE}.
 * The current value (what is written to an output) is stored as a string in the {@link ChannelId#CURRENT_VALUE}.
 */
public interface ToggleSwitch extends OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * <ul>
         * <li> Description: Is the incoming signal active e.g. is the Button pressed. Value of this Channel == true -> on release toggle the current state
         * <li>Interface: {@link ToggleSwitch}
         * <li>Type: Boolean
         * </ul>
         */
        SIGNAL_ACTIVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * <li> Description: The current value of the state that is written to an output/stored internally.
         * <li>Interface: ToggleSwitch
         * <li>Type: String
         * </ul>
         */
        CURRENT_VALUE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * <ul>
         * <li> Description: The current ToggleState (true == State A, false == State B).
         * <li>Interface: ToggleSwitch
         * <li>Type: Boolean
         * </ul>
         */
        TOGGLE_STATE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the Channel of the ChannelId {@link ChannelId#TOGGLE_STATE}.
     *
     * @return the channel
     */
    default Channel<Boolean> getToggleStateChannel() {
        return this.channel(ChannelId.TOGGLE_STATE);
    }

    /**
     * Get the Channel of the ChannelId {@link ChannelId#CURRENT_VALUE}.
     *
     * @return the channel
     */
    default Channel<String> getCurrentValueChannel() {
        return this.channel(ChannelId.CURRENT_VALUE);
    }

    /**
     * Get the Channel of the ChannelId {@link ChannelId#SIGNAL_ACTIVE}.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getSignalActiveChannel() {
        return this.channel(ChannelId.SIGNAL_ACTIVE);
    }


}

