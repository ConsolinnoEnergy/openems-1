package io.openems.edge.bridge.rest.device.task;

import io.openems.edge.bridge.rest.api.RestRequest;
import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;

/**
 * The Abstract Rest Remote Device Task. This stores every common parameters and methods handled by the RestBridge.
 */
public abstract class AbstractRestRemoteDeviceTask implements RestRequest {

    private final String remoteDeviceId;
    private final String deviceChannel;
    private final String realDeviceId;
    protected final Logger logger;
    protected final ComponentManager cpm;


    AbstractRestRemoteDeviceTask(String remoteDeviceId, String realDeviceId, String deviceChannel, Logger log, ComponentManager cpm) {
        this.remoteDeviceId = remoteDeviceId;
        this.deviceChannel = deviceChannel;
        this.realDeviceId = realDeviceId;
        this.logger = log;
        this.cpm = cpm;
    }

    /**
     * Returns the Request. (ComponentId/ChannelId) For the Bridge.
     *
     * @return the ComponentId/ChannelId String.
     */
    @Override
    public String getRequest() {
        return this.realDeviceId + "/" + this.deviceChannel;
    }

    /**
     * Gets the Id of the Configured Component.
     *
     * @return the Id String.
     */
    @Override
    public String getDeviceId() {
        return this.remoteDeviceId;
    }
}