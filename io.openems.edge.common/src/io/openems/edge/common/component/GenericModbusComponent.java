package io.openems.edge.common.component;

import io.openems.edge.common.channel.Channel;

public interface GenericModbusComponent extends OpenemsComponent {

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
