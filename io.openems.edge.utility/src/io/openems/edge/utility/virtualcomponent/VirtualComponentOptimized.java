package io.openems.edge.utility.virtualcomponent;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;


/**
 * This component represents a virtual component.
 * This can be used to monitor an optimization before applying it at the real device.
 * Or alternatively use this to temporarily save the optimization part and remap the optimization to different "real" components.
 */

public interface VirtualComponentOptimized extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Input For Write Value.
         *
         * <ul>
         * <li>Interface: VirtualComponentOptimized
         * <li>Type: Long
         * </ul>
         */
        WRITE_OPTIMIZED_VALUE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> {
            ((LongWriteChannel) channel).onSetNextWrite(channel::setNextValue);
        })),

        /**
         * Input For Write Value.
         *
         * <ul>
         * <li>Interface: VirtualComponentOptimized
         * <li>Type: Double
         * </ul>
         */
        WRITE_OPTIMIZED_VALUE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                        channel -> {
                            ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                        })),

        /**
         * Input For Write Value.
         *
         * <ul>
         * <li>Interface: VirtualComponentOptimized
         * <li>Type: Boolean
         * </ul>
         */
        WRITE_OPTIMIZED_VALUE_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> {
                    ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),

        /**
         * Input For Write Value.
         *
         * <ul>
         * <li>Interface: VirtualComponentOptimized
         * <li>Type: String
         * </ul>
         */
        WRITE_OPTIMIZED_VALUE_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> {
                    ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),
        /**
         * EnableSignal to simulate an EnableSignal.
         * The EnableSignal will be set to next Value.
         * If the EnableSignal is not present, the Value will be written to false.
         * Keep that in mind when using a {@link io.openems.edge.utility.channeltransmitter.ChannelTransmitterImpl}.
         * You may want to use an alternative value.
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));



        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    default WriteChannel<Boolean> getEnableSignal(){
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }
}
