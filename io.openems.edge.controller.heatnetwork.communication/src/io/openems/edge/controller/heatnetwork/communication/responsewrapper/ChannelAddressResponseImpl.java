package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

public class ChannelAddressResponseImpl extends AbstractResponseWrapper implements ChannelAddressResponse {

    private final ComponentManager cpm;
    private final ChannelAddress channelAddress;

    public ChannelAddressResponseImpl(String methodOrChannelAddress, String activeValue, String passiveValue, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        super(activeValue, passiveValue);
        this.cpm = cpm;
        this.channelAddress = ChannelAddress.fromString(methodOrChannelAddress);
    }

    @Override
    public void activateResponse() throws OpenemsError.OpenemsNamedException {
       this.response(super.activeValue);
    }

    @Override
    public void passiveResponse() throws OpenemsError.OpenemsNamedException {
        this.response(super.passiveValue);
    }

    private void response(String value) throws OpenemsError.OpenemsNamedException {
        Channel<?> channel = this.cpm.getChannel(this.channelAddress);
        if (value.equals("null")) {
            value = null;
        }
        if (channel instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channel).setNextWriteValueFromObject(value);
        } else {
            channel.setNextValue(value);
        }
    }
}
