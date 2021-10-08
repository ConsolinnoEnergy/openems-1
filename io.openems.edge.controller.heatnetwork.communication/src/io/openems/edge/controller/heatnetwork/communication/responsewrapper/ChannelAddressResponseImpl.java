package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

/**
 * ChannelAddressResponseImpl, implementing the Interface {@link ChannelAddressResponse}
 * Helps the CommunicationMaster, by saving the ChannelAddress from it's config and reacts to activation/deactivation,
 * with a corresponding value.
 */
public class ChannelAddressResponseImpl extends AbstractResponseWrapper implements ChannelAddressResponse {

    private final ComponentManager cpm;
    private final ChannelAddress channelAddress;

    public ChannelAddressResponseImpl(String methodOrChannelAddress, String activeValue, String passiveValue, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        super(activeValue, passiveValue);
        this.cpm = cpm;
        this.channelAddress = ChannelAddress.fromString(methodOrChannelAddress);
    }

    /**
     * Activates the Response, by setting the active Value. All the Information are coming from the
     * Config of the HeatNetworkMaster.
     *
     * @throws OpenemsError.OpenemsNamedException if channel won't be found.
     */
    @Override
    public void activateResponse() throws OpenemsError.OpenemsNamedException {
        this.response(super.activeValue);
    }

    /**
     * Deactivates the Response, by setting the active Value. All the Information are coming from the
     * Config of the HeatNetworkMaster.
     *
     * @throws OpenemsError.OpenemsNamedException if channel won't be found.
     */
    @Override
    public void passiveResponse() throws OpenemsError.OpenemsNamedException {
        this.response(super.passiveValue);
    }

    /**
     * Writes the given Value into the Channel. Called internally by {@link #activateResponse()} or {@link #passiveResponse()}
     * which are called by the  {@link io.openems.edge.controller.heatnetwork.communication.api.CommunicationMasterController}.
     *
     * @param value the value, that will be written into the Response Channel
     * @throws OpenemsError.OpenemsNamedException when Channel cannot be found / setNextWriteValueFromObject fails.
     */
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
