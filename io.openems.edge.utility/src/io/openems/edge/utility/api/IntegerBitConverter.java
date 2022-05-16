package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * This Nature provides a Bit output from an Integer input, or creates an Integer output from a Bit input.
 */
public interface IntegerBitConverter extends OpenemsComponent {

    int MAX_BITS = 32;

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * <ul>
         * IN OR OUTPUT. Integer input -> split to bits or
         * Bit to Integer output -> Get Bits and make an unsigned Integer value
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Integer
         * </ul>
         */
        INTEGER_VALUE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * Save the IntegerValue additionally to Long. If Integer becomes a negative value.
         * Just for debugging/additional information.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Long
         * </ul>
         */
        LONG_VALUE(Doc.of(OpenemsType.LONG)),
        /**
         * <ul>
         * The 1st Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_1(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 2nd Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 3rd Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_3(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 4th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_4(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 5th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_5(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 6th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_6(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 7th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_7(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 8th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_8(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 9th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_9(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 10th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_10(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 11th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_11(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 12th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_12(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 13th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_13(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 14th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_14(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 15th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_15(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 16th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_16(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 17th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_17(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 18th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_18(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 19th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_19(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 20th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_20(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 21st Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_21(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 22nd Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_22(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 23rd Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_23(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 24th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_24(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 25th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_25(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 26th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_26(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 27th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_27(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 28th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_28(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 29th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_29(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 30th Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_30(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 31st Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_31(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * <ul>
         * The 32nd Bit.
         * <li>Interface: IntegerToBitConverter
         * <li>Type: Boolean
         * </ul>
         */
        BIT_32(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Get the {@link ChannelId#INTEGER_VALUE}.
     *
     * @return the channel.
     */
    default WriteChannel<Integer> integerValueChannel() {
        return this.channel(ChannelId.INTEGER_VALUE);
    }


    /**
     * Get the {@link ChannelId#LONG_VALUE}.
     *
     * @return the channel.
     */
    default Channel<Long> longValueChannel() {
        return this.channel(ChannelId.LONG_VALUE);
    }

    /**
     * Get the {@link ChannelId#BIT_1}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit1Channel() {
        return this.channel(ChannelId.BIT_1);
    }

    /**
     * Get the {@link ChannelId#BIT_2}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit2Channel() {
        return this.channel(ChannelId.BIT_2);
    }

    /**
     * Get the {@link ChannelId#BIT_3}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit3Channel() {
        return this.channel(ChannelId.BIT_3);
    }

    /**
     * Get the {@link ChannelId#BIT_4}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit4Channel() {
        return this.channel(ChannelId.BIT_4);
    }

    /**
     * Get the {@link ChannelId#BIT_5}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit5Channel() {
        return this.channel(ChannelId.BIT_5);
    }

    /**
     * Get the {@link ChannelId#BIT_6}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit6Channel() {
        return this.channel(ChannelId.BIT_6);
    }

    /**
     * Get the {@link ChannelId#BIT_7}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit7Channel() {
        return this.channel(ChannelId.BIT_7);
    }

    /**
     * Get the {@link ChannelId#BIT_8}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit8Channel() {
        return this.channel(ChannelId.BIT_8);
    }

    /**
     * Get the {@link ChannelId#BIT_9}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit9Channel() {
        return this.channel(ChannelId.BIT_9);
    }

    /**
     * Get the {@link ChannelId#BIT_10}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit10Channel() {
        return this.channel(ChannelId.BIT_10);
    }

    /**
     * Get the {@link ChannelId#BIT_11}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit11Channel() {
        return this.channel(ChannelId.BIT_11);
    }

    /**
     * Get the {@link ChannelId#BIT_12}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit12Channel() {
        return this.channel(ChannelId.BIT_12);
    }

    /**
     * Get the {@link ChannelId#BIT_13}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit13Channel() {
        return this.channel(ChannelId.BIT_13);
    }

    /**
     * Get the {@link ChannelId#BIT_14}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit14Channel() {
        return this.channel(ChannelId.BIT_14);
    }

    /**
     * Get the {@link ChannelId#BIT_15}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit15Channel() {
        return this.channel(ChannelId.BIT_15);
    }

    /**
     * Get the {@link ChannelId#BIT_16}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit16Channel() {
        return this.channel(ChannelId.BIT_16);
    }

    /**
     * Get the {@link ChannelId#BIT_17}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit17Channel() {
        return this.channel(ChannelId.BIT_17);
    }

    /**
     * Get the {@link ChannelId#BIT_18}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit18Channel() {
        return this.channel(ChannelId.BIT_18);
    }

    /**
     * Get the {@link ChannelId#BIT_19}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit19Channel() {
        return this.channel(ChannelId.BIT_19);
    }

    /**
     * Get the {@link ChannelId#BIT_20}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit20Channel() {
        return this.channel(ChannelId.BIT_20);
    }

    /**
     * Get the {@link ChannelId#BIT_21}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit21Channel() {
        return this.channel(ChannelId.BIT_21);
    }

    /**
     * Get the {@link ChannelId#BIT_22}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit22Channel() {
        return this.channel(ChannelId.BIT_22);
    }

    /**
     * Get the {@link ChannelId#BIT_23}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit23Channel() {
        return this.channel(ChannelId.BIT_23);
    }

    /**
     * Get the {@link ChannelId#BIT_24}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit24Channel() {
        return this.channel(ChannelId.BIT_24);
    }

    /**
     * Get the {@link ChannelId#BIT_25}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit25Channel() {
        return this.channel(ChannelId.BIT_25);
    }

    /**
     * Get the {@link ChannelId#BIT_26}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit26Channel() {
        return this.channel(ChannelId.BIT_26);
    }

    /**
     * Get the {@link ChannelId#BIT_27}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit27Channel() {
        return this.channel(ChannelId.BIT_27);
    }

    /**
     * Get the {@link ChannelId#BIT_28}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit28Channel() {
        return this.channel(ChannelId.BIT_28);
    }

    /**
     * Get the {@link ChannelId#BIT_29}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit29Channel() {
        return this.channel(ChannelId.BIT_29);
    }

    /**
     * Get the {@link ChannelId#BIT_30}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit30Channel() {
        return this.channel(ChannelId.BIT_30);
    }

    /**
     * Get the {@link ChannelId#BIT_31}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit31Channel() {
        return this.channel(ChannelId.BIT_31);
    }

    /**
     * Get the {@link ChannelId#BIT_32}.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> bit32Channel() {
        return this.channel(ChannelId.BIT_32);
    }

    /**
     * Converts an integer to a Bit boolean Array.
     *
     * @param value the integer value
     * @return the boolean Array. Bit 1 at Position 0, Bit 32 at position 31.
     */
    static boolean[] getBitsFromInteger(int value) {

        return getBitsFromBitString(Integer.toBinaryString(value));
    }

    /**
     * Get a Bit boolean Array from a Bit String.
     *
     * @param value the Bit string.
     * @return the
     */
    static boolean[] getBitsFromBitString(String value) {
        boolean[] bits = new boolean[MAX_BITS];
        String valueAsBitString = String.format("%32s", value).replaceAll(" ", "0");
        char[] valueAsChars = valueAsBitString.toCharArray();
        for (int x = 0; x < MAX_BITS; x++) {
            bits[x] = (valueAsChars[(MAX_BITS - 1) - x]) == '1';
        }
        return bits;
    }

    /**
     * Return an integer from a Bit Map input.
     *
     * @param bitNumberToBooleanMap bit to value map (where 1 is least significant bit and 32 is most significant bit)
     * @return the unsigned integer value
     */
    static Integer getIntegerFromBits(Map<Integer, Boolean> bitNumberToBooleanMap) {
        char[] values = new char[MAX_BITS];
        for (int x = 0; x < MAX_BITS; x++) {
            values[x] = '0';
        }
        bitNumberToBooleanMap.forEach((key, value) -> {
            char valueToWrite = value ? '1' : '0';
            values[MAX_BITS - key] = valueToWrite;
        });
        return Integer.parseUnsignedInt(String.valueOf(values), 2);
    }

    /**
     * Writes a boolean to a bit Channel, depending on the bitPosition.
     *
     * @param bitPosition the bitPosition
     * @param value       the boolean value.
     */
    default void writeBoolToInternalChannel(int bitPosition, boolean value) {
        WriteChannel<Boolean> channel = this.getChannelFromBitPosition(bitPosition);

        if (channel != null) {
            channel.setNextValue(value);
        }

    }

    /**
     * Get the Channel corresponding to the bitPosition.
     *
     * @param bitPosition the bitPosition
     * @return the corresponding channel.
     */
    default WriteChannel<Boolean> getChannelFromBitPosition(int bitPosition) {
        WriteChannel<Boolean> channel;
        switch (bitPosition) {
            case 1:
                channel = this.bit1Channel();
                break;
            case 2:
                channel = this.bit2Channel();
                break;
            case 3:
                channel = this.bit3Channel();
                break;
            case 4:
                channel = this.bit4Channel();
                break;
            case 5:
                channel = this.bit5Channel();
                break;
            case 6:
                channel = this.bit6Channel();
                break;
            case 7:
                channel = this.bit7Channel();
                break;
            case 8:
                channel = this.bit8Channel();
                break;
            case 9:
                channel = this.bit9Channel();
                break;
            case 10:
                channel = this.bit10Channel();
                break;
            case 11:
                channel = this.bit11Channel();
                break;
            case 12:
                channel = this.bit12Channel();
                break;
            case 13:
                channel = this.bit13Channel();
                break;
            case 14:
                channel = this.bit14Channel();
                break;
            case 15:
                channel = this.bit15Channel();
                break;
            case 16:
                channel = this.bit16Channel();
                break;
            case 17:
                channel = this.bit17Channel();
                break;
            case 18:
                channel = this.bit18Channel();
                break;
            case 19:
                channel = this.bit19Channel();
                break;
            case 20:
                channel = this.bit20Channel();
                break;
            case 21:
                channel = this.bit21Channel();
                break;
            case 22:
                channel = this.bit22Channel();
                break;
            case 23:
                channel = this.bit23Channel();
                break;
            case 24:
                channel = this.bit24Channel();
                break;
            case 25:
                channel = this.bit25Channel();
                break;
            case 26:
                channel = this.bit26Channel();
                break;
            case 27:
                channel = this.bit27Channel();
                break;
            case 28:
                channel = this.bit28Channel();
                break;
            case 29:
                channel = this.bit29Channel();
                break;
            case 30:
                channel = this.bit30Channel();
                break;
            case 31:
                channel = this.bit31Channel();
                break;
            case 32:
                channel = this.bit32Channel();
                break;

            default:
                channel = null;
        }
        return channel;
    }

    /**
     * Internal method to generate an integerValue from existing bits.
     *
     * @return an Integer from BIT Channel.
     */
    default Integer _generateIntegerFromChannel() {
        Map<Integer, Boolean> bitPosToValueMap = new HashMap<>();
        for (int x = 1; x <= MAX_BITS; x++) {
            WriteChannel<Boolean> channel = this.getChannelFromBitPosition(x);
            Boolean value = false;
            if (channel != null) {
                value = channel.getNextWriteValueAndReset().orElse(false);
                channel.setNextValue(value);
            }
            bitPosToValueMap.put(x, value);
        }

        return getIntegerFromBits(bitPosToValueMap);
    }
}

