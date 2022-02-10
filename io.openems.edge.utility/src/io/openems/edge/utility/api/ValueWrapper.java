package io.openems.edge.utility.api;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.utility.calculator.AbstractCalculator;

/**
 * This class stores a Value or a ChannelAddress, depending on the {@link ValueOrChannel}.
 * This is determined within the constructor of this object.
 * Instances of this Class are created by the {@link AbstractCalculator}.
 * Children of the {@link AbstractCalculator} call this class to determine if the Value that is stored here is a special
 * Value (e.g. value should be used to divide the current calculation by this value or subtract this value).
 */
public class ValueWrapper {

    private final ValueOrChannel type;

    private String value;

    private ChannelAddress channelAddress;

    private final boolean isSpecialValue;

    public enum ValueOrChannel {
        VALUE, CHANNEL
    }

    public ValueWrapper(ChannelAddress address, boolean isSpecialValue) {
        this.type = ValueOrChannel.CHANNEL;
        this.channelAddress = address;
        this.isSpecialValue = isSpecialValue;
    }

    public ValueWrapper(String value, boolean isSpecialValue) {
        this.type = ValueOrChannel.VALUE;
        this.value = value;
        this.isSpecialValue = isSpecialValue;
    }

    /**
     * Get the Type of this ValueWrapper CHANNEL if the Value is stored within a ChannelAddress,
     * or VALUE when the Value is a static number.
     *
     * @return the {@link #type}
     */
    public ValueOrChannel getType() {
        return this.type;
    }

    /**
     * Get the static Value of this wrapper.
     *
     * @return {@link #value}
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Get the ChannelAddress of this wrapper, where the Value is stored for the calculation.
     *
     * @return {@link #channelAddress}
     */
    public ChannelAddress getChannelAddress() {
        return this.channelAddress;
    }

    /**
     * If the config had a special character in front of the value.
     * This Value will be treated in a special way.
     * The Special Character and handling of the value is done by the children of the {@link AbstractCalculator}.
     *
     * @return {@link #isSpecialValue}
     */
    public boolean isSpecialValue() {
        return this.isSpecialValue;
    }
}
