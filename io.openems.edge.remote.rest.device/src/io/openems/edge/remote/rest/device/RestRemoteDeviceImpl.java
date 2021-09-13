package io.openems.edge.remote.rest.device;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.communication.remote.rest.api.RestBridge;
import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import io.openems.edge.remote.rest.device.task.RestRemoteReadTask;
import io.openems.edge.remote.rest.device.task.RestRemoteWriteTask;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Remote Device Communicating via REST.
 * One can configure a Channel to get Information from / write into.
 * Note: ATM Only Numeric Values are possible to read from!
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Rest.Remote.Device", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class RestRemoteDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteDevice, EventHandler {

    private final Logger log = LoggerFactory.getLogger(RestRemoteDeviceImpl.class);

    @Reference
    ComponentManager cpm;

    private RestBridge restBridge;

    private String restBridgeId;
    private RestRequest task;
    private boolean isRead;
    TimerHandler timerHandler;
    private static final String WAIT_TIME_IDENTIFIER = "WAIT_TIME_REST_REMOTE_DEVICE";

    private Config config;

    public RestRemoteDeviceImpl() {

        super(OpenemsComponent.ChannelId.values(),
                RestRemoteDevice.ChannelId.values());
    }

    /**
     * Activates the Component, get the Rest Bridge and add the RestRemoteDevice.
     *
     * @param context the context of the Component.
     * @param config  the Config.
     * @throws ConfigurationException             if the Rest Bridge is incorrect or the Connection gets an Error.
     * @throws OpenemsError.OpenemsNamedException if the Id is no available.
     */
    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {
            this.activationOrModifiedRoutine(config);
            this.getAllowRequestChannel().setNextValue(true);
        }
    }

    /**
     * This method will be called on activation or modified. This will configure the RestBridge and creates the Tasks for the RestBridge.
     *
     * @param config the config of this class
     * @throws OpenemsError.OpenemsNamedException will be thrown when de RestBridge couldn't be found.
     * @throws ConfigurationException             will be thrown if the configured BridgeId is not a RestBridge.
     */
    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.isEnabled()) {
            if (this.cpm.getComponent(config.restBridgeId()) instanceof RestBridge) {
                this.restBridge = this.cpm.getComponent(config.restBridgeId());
                this.task = this.createNewTask(config.deviceChannel(),
                        config.id(), config.realDeviceId(), config.deviceMode());
                this.restBridge.addRestRequest(super.id(), this.task);
            } else {
                throw new ConfigurationException(config.restBridgeId(), "Bridge Id Incorrect for : " + super.id() + "!");
            }
            this.createTimerHandler(config);
        }
    }

    private void createTimerHandler(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
        this.timerHandler.addOneIdentifier(WAIT_TIME_IDENTIFIER, config.timerId(), config.waitTime());
    }

    /**
     * This Method creates a RestRequest with it's given parameters coming from config. Usually called by the @Activate
     *
     * @param deviceChannel  usually from Config, declares the Channel of the actual Device you want to access.
     * @param remoteDeviceId usually from Config, is the Remote Device Id.
     * @param realDeviceId   usually from Config, is the Unique Id of the Device you want to access.
     * @param deviceMode     usually from Config, declares if you want to Read or Write.
     * @return RestRequest if creation of Instance was successful.
     * @throws ConfigurationException if TemperatureSensor is set to Write; or if an impossible Case occurs (deviceMode neither Read/Write)
     */
    private RestRequest createNewTask(String deviceChannel, String remoteDeviceId,
                                      String realDeviceId, String deviceMode) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        RestRequest task;
        if (deviceMode.equals("Write")) {

            this.getTypeSetChannel().setNextValue("Write");
            this.isRead = false;
            task = new RestRemoteWriteTask(remoteDeviceId, realDeviceId, deviceChannel,
                    ChannelAddress.fromString(super.id() + "/" + this.getWriteValueChannel().channelId().id()),
                    ChannelAddress.fromString(super.id() + "/" + this.getAllowRequestChannel().channelId().id()),
                    this.log, this.cpm);
            return task;

        } else if (deviceMode.equals("Read")) {
            this.getTypeSetChannel().setNextValue("Read");
            this.isRead = true;
            //String deviceId, String masterSlaveId, boolean master, String realTemperatureSensor, Channel<Integer> temperature
            task = new RestRemoteReadTask(remoteDeviceId, realDeviceId, deviceChannel,
                    ChannelAddress.fromString(super.id() + "/" + this.getReadValueChannel().channelId().id()), this.log, this.cpm);
            return task;
        }

        throw new ConfigurationException("Impossible Error", "Error shouldn't Occur because of Fix options");
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.restBridge.removeRestRemoteDevice(super.id());
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {
            this.activationOrModifiedRoutine(config);
        }

    }


    /**
     * SetsValue of Remote Device, if Remote Device TypeSet is set to "Write".
     *
     * @param value Value which will be Written to Device configured by the Remote Device.
     */
    @Override
    public void setValue(String value) {
        if (this.getTypeSetChannel().getNextValue().isDefined() == false) {
            this.log.warn("The Type of the Remote Device: " + super.id() + " is not available yet");
            return;
        }
        if (this.getTypeSetChannel().getNextValue().get().equals("Read")) {
            this.log.warn("Can't write into ReadTasks: " + super.id());
        }

        try {
            this.getWriteValueChannel().setNextWriteValue(value);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write the Value for : " + super.id());
        }

    }

    /**
     * Gets the Current Value of the Remote device depending if it's either read or write.
     *
     * @return the Value as a String.
     */
    @Override
    public String getValue() {
        if (this.getTypeSetChannel().value().get().equals("Write")) {
            if (this.getWriteValueChannel().value().isDefined()) {
                return this.getWriteValueChannel().value().get();
            } else {
                return "Write Value not available yet for " + super.id();
            }
        } else if (this.getReadValueChannel().value().isDefined()) {
            return this.getReadValueChannel().value().get();
        }
        return "Read Value not available yet";
    }

    /**
     * Get the Unique Id.
     *
     * @return the Id.
     */

    @Override
    public String getId() {
        return this.id();
    }

    /**
     * Check if this Device is a Write Remote Device.
     *
     * @return a boolean.
     */

    @Override
    public boolean isWrite() {
        return this.isRead == false;
    }

    /**
     * Checks if this Device is a Read Remote Device.
     *
     * @return a boolean.
     */

    @Override
    public boolean isRead() {
        return this.isRead;
    }

    /**
     * Checks/Asks if the Connection via Rest is ok.
     *
     * @return a boolean.
     */

    @Override
    public boolean connectionOk() {
        return this.restBridge.connectionOk();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof RestRemoteDeviceImpl) {
            RestRemoteDeviceImpl other = (RestRemoteDeviceImpl) o;
            return other.id().equals(this.id());
        }

        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            if (this.isEnabled()) {
                if (this.timerHandler.checkTimeIsUp(WAIT_TIME_IDENTIFIER)) {
                    try {
                        if (this.restBridge == null || this.restBridge.isEnabled() == false) {
                            OpenemsComponent component = this.cpm.getComponent(this.config.restBridgeId());
                            if (component instanceof RestBridge) {
                                this.restBridge = (RestBridge) component;
                            }
                        }
                        if (this.restBridge.getAllRequests() == null || this.restBridge.getAllRequests().containsKey(this.id()) == false) {
                            try {
                                if (this.task == null) {
                                    this.task = this.createNewTask(this.config.deviceChannel(),
                                            this.config.id(), this.config.realDeviceId(), this.config.deviceMode());
                                }
                                this.restBridge.addRestRequest(this.id(), this.task);
                            } catch (ConfigurationException e) {
                                this.log.info("Connection is not available, trying again later!");
                            }
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't find RestBridge! " + this.config.restBridgeId());
                    }
                    this.timerHandler.resetTimer(WAIT_TIME_IDENTIFIER);
                }
            }
        }
    }

    /**
     * Deactivates the component and removes it's task from the bridge.
     */
    @Deactivate
    public void deactivate() {
        this.restBridge.removeRestRemoteDevice(super.id());
        super.deactivate();
    }
}
