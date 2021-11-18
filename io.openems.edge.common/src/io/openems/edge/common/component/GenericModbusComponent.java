package io.openems.edge.common.component;

import io.openems.edge.common.channel.Channel;

public interface GenericModbusComponent extends OpenemsComponent {

    /**
     * This Checks if a Value is defined between the two Channel.
     * Usually one of the two Channels is configured by an Implementation of a GenericModbus Component and after that,
     * mapped to the "real" Channel e.g.  MeterModbusGeneric.ChannelId#FLOW_TEMP_DOUBLE} and
     *  MeterModbusGeneric.ChannelId#FLOW_TEMP_LONG} will be mapped to {HeatMeter.ChannelId#FLOW_TEMP}
     *
     * @param firstChannel  the first channel
     * @param secondChannel the second channel.
     * @return the Channel storing the value
     */

    static Channel<?> getValueDefinedChannel(Channel<?> firstChannel, Channel<?> secondChannel) {
        if (firstChannel.getNextValue().isDefined()) {
            return firstChannel;
        } else if (secondChannel.getNextValue().isDefined()) {
            return secondChannel;
        } else {
            return null;
        }
    }
}
