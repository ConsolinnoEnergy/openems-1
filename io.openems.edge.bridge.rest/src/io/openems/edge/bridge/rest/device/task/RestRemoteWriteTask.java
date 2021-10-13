package io.openems.edge.bridge.rest.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.rest.api.RestWriteRequest;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;

/**
 * This Task collects the value one wants to write into a remote device. The value will be processed by the RestBridge.
 */
public class RestRemoteWriteTask extends AbstractRestRemoteDeviceTask implements RestWriteRequest {

    private final ChannelAddress valueAddress;
    private final ChannelAddress allowRequestAddress;


    public RestRemoteWriteTask(String remoteDeviceId, String realDeviceId,
                               String deviceChannel, ChannelAddress value,
                               ChannelAddress allowRequest, Logger log, ComponentManager cpm) {
        super(remoteDeviceId, realDeviceId, deviceChannel, log, cpm);

        this.valueAddress = value;
        this.allowRequestAddress = allowRequest;
    }

    /**
     * Creates the msg String for the REST POST Method.
     *
     * @return <p>PostMessage String if Value is defined.
     * If no Value is Defined Return NoValueDefined.
     * If the Value is not readyToWrite return "NotReadyToWrite".</p>
     */
    @Override
    public String getPostMessage() {
        if (this.allowedToWrite()) {
            Channel<String> valueChannel;
            try {
                valueChannel = this.cpm.getChannel(this.valueAddress);
            } catch (OpenemsError.OpenemsNamedException e) {
                return "ChannelNotAvailable";
            }
            if (valueChannel.value().isDefined()) {
                String msg = "{\"value\":";
                msg += valueChannel.value().get() + "}";
                return msg;
            }
            return "NoValueDefined";
        }
        return "NotReadyToWrite";
    }

    /**
     * If POST Method was successful, hasBeenset = true and print the success, otherwise print failure.
     *
     * @param success     successful POST call.
     * @param response Response of the REST Method.
     */
    @Override
    public void wasSuccess(boolean success, String response) {
        if (success) {
            this.logger.info("The Device corresponding to this device: " + super.getDeviceId() + "Was successfully set " + response);
        } else {
            this.logger.warn("Error while Posting Value, please try again! " + response);
        }

    }

    /**
     * Checks if the Component is Ready To Write/ Allowed to Write (Write false in Channel ALLOW_REQUEST).
     *
     * @return a boolean.
     */
    @Override
    public boolean allowedToWrite() {
        Channel<Boolean> allowedRequest;
        try {
            allowedRequest = this.cpm.getChannel(this.allowRequestAddress);
        } catch (OpenemsError.OpenemsNamedException e) {
            return false;
        }
        if (allowedRequest.value().isDefined()) {
            return allowedRequest.value().get();
        } else {
            return false;
        }
    }
}
