package io.openems.edge.controller.optimizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.optimizer.api.Optimizer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

import java.util.ArrayList;
import java.util.List;


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.controller.optimizer", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class OptimizerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Optimizer, EventHandler {


    @Reference
    ComponentManager cpm;
    private String jsonString = "null";
    private String bridgeId;
    private JsonArray jsonArray;
    private JsonPatchWorker jsonPatchWorker;
    private static final int DEFAULT = 65535;
    private static final int CHANNEL_INDEX = 0;
    private static final int VALUE_INDEX = 1;
    private static final int PING_TIME_DELTA = 5;
    private static final int RECONNECT_SECOND_TIME_DELTA = 15;
    private boolean stopOnError;
    private boolean stop;
    private String lastUpdate;
    private int minTime = DEFAULT;
    private int activeTask = DEFAULT;
    private int minDeltaTime = DEFAULT;
    private int logDisconnect = 1;
    private int lastMemberSeconds;
    private boolean reconnect = false;
    private final Logger log = LoggerFactory.getLogger(OptimizerImpl.class);
    private DateTime ping;
    private DateTime reconnectTime;
    private MqttBridge mqttBridge;
    private boolean fallback;
    private boolean previousFallback;
    private boolean rollOver = true;
    private DateTime savedDate = new DateTime();

    public OptimizerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Optimizer.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        this.jsonPatchWorker = new JsonPatchWorker(new ChannelAddress(config.id(), getJsonChannel().channelId().id()), new ChannelAddress(config.id(), getFallbackChannel().channelId().id()), this.cpm);
        this.stopOnError = config.stop();
        this.lastMemberSeconds = config.lastMemberTime();
        try {
            this.mqttBridge = this.cpm.getComponent(config.bridgeId());
        } catch (OpenemsError.OpenemsNamedException e) {
            // in case the bridge is not Configured when this is activated
            this.bridgeId = config.bridgeId();
            this.reconnectTime = new DateTime();
            this.log.error("Error in activate! The Mqtt Bridge does not exist");
        }

        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        this.stopOnError = config.stop();
        try {
            this.mqttBridge = this.cpm.getComponent(config.bridgeId());
        } catch (OpenemsError.OpenemsNamedException e) {
            // in case the bridge is not Configured when this is modified
            this.bridgeId = config.bridgeId();
            this.reconnectTime = new DateTime();
            this.log.error("Error in activate! The Mqtt Bridge does not exist");
        }

        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();

    }

    @Override
    public void handleEvent(Event event) {
        this.pingMqtt(this.checkPingTime());
        try {
            this.jsonPatchWorker.work(this.jsonString);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in Handle Event. OpenemsNamedException! jsonString is not Correct for Json Patch worker");
        }

        JsonObject json;
        if (this.jsonString.equals("null")) {
            this.lastUpdate = this.jsonString;
            this.jsonString = getJsonString();
            if (this.jsonString != null && this.jsonString.equals("null") == false) {
                json = new Gson().fromJson(this.jsonString, JsonObject.class);
                this.jsonArray = json.getAsJsonArray("time");
            }
        }
        //check for updated Json
        String updateString = getJsonString();
        if (updateString != null && updateString.equals("null") == false && this.jsonString.equals(updateString) == false) {

            if (this.lastUpdate != null && updateString.equals(this.lastUpdate) == false) {
                this.jsonString = updateString;
                this.fallback = false;
            }
            if (this.lastUpdate == null) {
                this.jsonString = updateString;
            }
            this.lastUpdate = updateString;
            json = new Gson().fromJson(this.jsonString, JsonObject.class);
            this.jsonArray = json.getAsJsonArray("time");
        }
        //Switches to Fallback schedule
        if (this.fallback) {
            this.jsonString = getFallbackString();
            json = new Gson().fromJson(this.jsonString, JsonObject.class);
            this.jsonArray = json.getAsJsonArray("time");
            this.previousFallback = this.fallback;
        }
        // Switches back from fallback
        if (!this.fallback && this.previousFallback) {
            this.jsonString = getJsonString();
            json = new Gson().fromJson(this.jsonString, JsonObject.class);
            this.jsonArray = json.getAsJsonArray("time");
            this.previousFallback = false;
        }


        if (this.jsonArray != null) {
            // get Closest Timestamp that is still in the past

            Integer[] timeArray = new Integer[this.jsonArray.size()];
            int timestamp;
            for (int i = 0; i < this.jsonArray.size(); i++) {
                DateTime now = DateTime.now(DateTimeZone.UTC);
                //add a 0 digit because 08 would be written as 8 and not 08
                if (now.minuteOfHour().get() < 10) {
                    timestamp = Integer.parseInt(now.hourOfDay().getAsString() + "0" + now.minuteOfHour().getAsString());
                } else {
                    timestamp = Integer.parseInt(now.hourOfDay().getAsString() + now.minuteOfHour().getAsString());
                }
                if (now.hourOfDay().get() == 0 && this.rollOver) {
                    this.rollOver = false;
                    timestamp = Integer.parseInt("0" + timestamp);
                    this.minTime = 0;
                    this.minDeltaTime = DEFAULT;
                } else if (now.hourOfDay().get() != 0) {
                    this.rollOver = true;
                }
                timeArray[i] = Integer.parseInt(this.jsonArray.get(i).getAsJsonObject().entrySet().iterator().next().getKey());
                if (this.minTime != DEFAULT) {
                    this.minDeltaTime = timestamp - this.minTime;
                }
                int deltaTime = timestamp - timeArray[i];
                if (deltaTime >= 0 && deltaTime <= this.minDeltaTime) {
                    this.minTime = timeArray[i];
                    this.activeTask = i;
                }
                if (this.fallback) {
                    this.minTime = Integer.parseInt("000");
                    this.activeTask = 0;
                }
            }
            if (this.minTime != DEFAULT) {
                if (this.jsonArray.size() > this.activeTask) {
                    DateTime lastMemberCountDown;
                    if (this.activeTask == this.jsonArray.size() - 1) {
                        lastMemberCountDown = new DateTime().plusSeconds(this.lastMemberSeconds);
                    } else {
                        lastMemberCountDown = null;
                    }
                    JsonElement activeJsonTask = this.jsonArray.get(this.activeTask);
                    if ((this.stopOnError && this.stop && new DateTime().getMinuteOfHour() % 2 == 0)
                            || (lastMemberCountDown != null
                            && new DateTime().getMinuteOfDay() == lastMemberCountDown.getMinuteOfDay())) {
                        this.fallback = true;
                    } else if (!this.stop) {
                        this.fallback = false;
                    }
                    List<List<String>> channelIdAndValues = this.getChannelIdsAndValues(activeJsonTask, this.minTime);
                    if (channelIdAndValues != null) {
                        this.executeActiveTask(channelIdAndValues);
                    }

                }
            }
        }
        if ((this.stopOnError && this.stop && this.jsonArray == null)) {
            this.fallback = true;
            this.getHasScheduleChannel().setNextValue(false);
        } else if (!this.stop) {
            this.fallback = false;
            this.getHasScheduleChannel().setNextValue(true);
        }
        this.getHasScheduleChannel().setNextValue(this.jsonArray != null && !this.fallback);

    }


    /**
     * Checks if its time to Ping the Mqtt Server based on the PING_TIME_DELTA value.
     *
     * @return true if the time is up or this optimizer was just created
     */
    private boolean checkPingTime() {
        if (this.ping != null) {
            if (getStatus().equals("Error")) {
                return this.checkReconnectTime();
            }
            return (new DateTime().getMinuteOfDay() - this.ping.getMinuteOfDay() < 0
                    || new DateTime().getMinuteOfDay() - this.ping.getMinuteOfDay() >= PING_TIME_DELTA
            );
        }
        return true;

    }

    /**
     * Checks if its time to retry a connection to the MqttBridge.
     *
     * @return true if the time is up
     */
    private boolean checkReconnectTime() {
        if (this.reconnectTime != null) {
            return (new DateTime().getSecondOfDay() - this.reconnectTime.getSecondOfDay() < 0
                    || new DateTime().getSecondOfDay() - this.reconnectTime.getSecondOfDay() >= RECONNECT_SECOND_TIME_DELTA
            );
        } else {
            return true;
        }
    }

    /**
     * Pings the Mqtt Server and sets the Connection Status in the Status Channel.
     *
     * @param ping Boolean -> if the Server should be pinged (should use CheckPingTime())
     */
    private void pingMqtt(Boolean ping) {
        if (ping || this.reconnect) {
            this.ping = new DateTime();
            if (this.mqttBridge != null && this.mqttBridge.isConnected()) {
                if (getStatus().equals("Online") == false) {
                    this.log.info("Connection established to Mqtt Server after " + this.logDisconnect + " attempt/-s");
                    this.logDisconnect = 1;
                    this.stop = false;
                    getStatusChannel().setNextValue("Online");
                }
            } else {
                this.logDisconnect++;
                if (this.getStatus().equals("Error") == false) {
                    this.log.warn("Connection Lost to Mqtt Server");
                    this.stop = true;
                    getStatusChannel().setNextValue("Error");
                }
            }
            if (this.reconnect) {
                this.reconnect = false;
            }
        }

        /* Try to establish a connection to the MqttBridge in case the optimizer was unable to at the point of activation/modification. */

        if (this.mqttBridge == null && this.checkReconnectTime()) {
            try {
                this.log.info("Trying to reconnect to Bridge");
                this.mqttBridge = this.cpm.getComponent(this.bridgeId);
                this.reconnect = true;
                this.log.info("Success");
            } catch (OpenemsError.OpenemsNamedException e) {
                this.reconnectTime = new DateTime();
                this.log.error("Failed to reconnect to Bridge");
            }

        }
    }

    /**
     * Writes all the values into the channels, specified in the current Task in the Json.
     *
     * @param channelIdAndValues The return of the getChannelIdsAndValues method
     */
    @SuppressWarnings("unchecked")
    private void executeActiveTask(List<List<String>> channelIdAndValues) {
        List<String> channel = channelIdAndValues.get(CHANNEL_INDEX);
        List<String> values = channelIdAndValues.get(VALUE_INDEX);

        for (int i = 0; i < channel.size(); i++) {
            try {
                ChannelAddress channelAddress = ChannelAddress.fromString(channel.get(i).replace("\"", ""));
                Channel<?> writeChannel;
                writeChannel = this.cpm.getChannel(channelAddress);
                if (writeChannel instanceof WriteChannel<?>) {
                    OpenemsType type = writeChannel.getType();
                    if (values.get(i).equals("null") == false) {
                        String value = values.get(i).replace("\"", "");
                        switch (type) {
                            case BOOLEAN:
                                //enable_signal have to be treated differently
                                if (channelAddress.toString().contains("EnableSignal")) {
                                    boolean setEnable = true;
                                    if (values.size() == i + 1) {
                                        setEnable = !values.get(i).startsWith("0");
                                    }
                                    if (setEnable && value.equals("true") || value.equals("1")) {
                                        ((WriteChannel<Boolean>) writeChannel).setNextWriteValue(true);
                                    }
                                } else {
                                    if (value.equals("true") || value.equals("1")) {
                                        ((WriteChannel<Boolean>) writeChannel).setNextWriteValue(true);
                                    } else {
                                        ((WriteChannel<Boolean>) writeChannel).setNextWriteValue(false);
                                    }
                                }
                                break;
                            case DOUBLE:
                                ((WriteChannel<Double>) writeChannel).setNextWriteValue(Double.parseDouble(value));
                                break;
                            case FLOAT:
                                ((WriteChannel<Float>) writeChannel).setNextWriteValue(Float.parseFloat(value));
                                break;
                            case INTEGER:
                                ((WriteChannel<Integer>) writeChannel).setNextWriteValue(Integer.parseInt(value));
                                break;
                            case LONG:
                                ((WriteChannel<Long>) writeChannel).setNextWriteValue(Long.parseLong(value));
                                break;
                            case SHORT:
                                ((WriteChannel<Short>) writeChannel).setNextWriteValue(Short.parseShort(value));
                                break;
                            default:
                                this.log.info(channelAddress + " " + value + "This value could not be used, because no supported Datatype has been specified.");
                                break;
                        }

                    }
                } else {
                    this.log.error("Error in execute Task. This is not a WriteChannel.");
                }
            } catch (Exception ignored) {
                this.log.error("Error in execute Task. Something went wrong with the ChannelIDs or Values."
                        + " This Channel either does not exist or the Value is incompatible."
                        + "This Channel will now be deleted.");
                this.jsonPatchWorker.deleteChannel(channel.get(i));
            }
        }

    }

    /**
     * Reads Json and saves the values in a List of String Lists.
     *
     * @param activeJsonTask The member of the Json that is the currently active Task
     * @param minTime        the Timestamp of the active task
     * @return A list of ChannelIds and Values. Both lists are the same length
     */
    private List<List<String>> getChannelIdsAndValues(JsonElement activeJsonTask, int minTime) {
        List<String> components = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        List<String> value = new ArrayList<>();
        String timeStamp = minTime + "";
        //0030 is previously converted to 30 therefore timeStamp.length == 2 -> convert to 030 to get the timestamp in JsonTask
        if (timeStamp.length() == 2) {
            timeStamp = "0" + timeStamp;
        }
        if (timeStamp.equals("0")) {
            timeStamp = "000";
        }
        if (!activeJsonTask.getAsJsonObject().has(timeStamp)) {
            return null;
        }
        int activeSize = activeJsonTask.getAsJsonObject().get(timeStamp).getAsJsonArray().size();
        for (int i = 0; i < activeSize; i++) {
            /*
            Since its possible to have more Channel_Value pairs then Components there has to be a component list
            where the channel Strings can be added on,so the amount of ChannelIds and Values are the same at the end
             */
            int channelCount = activeJsonTask.getAsJsonObject().get(timeStamp)
                    .getAsJsonArray().get(i).getAsJsonObject().get("channel_value").getAsJsonArray().size();
            //Adds the Component to the Component list and adds a / for convenience
            components.add(activeJsonTask.getAsJsonObject().get(timeStamp).getAsJsonArray().get(i)
                    .getAsJsonObject().get("component") + "/");
            /*
            Now adds all ChannelIds of a Component to the ChannelId list (Above Component + ChannelId) writes the
            appropriate Value in the value list. This guarantees that the lists are the same size and in the same order
             */
            for (int n = 0; n < channelCount; n++) {
                channelIds.add(components.get(i) + activeJsonTask.getAsJsonObject().get(timeStamp)
                        .getAsJsonArray().get(i).getAsJsonObject().get("channel_value").getAsJsonArray().get(n)
                        .getAsJsonObject().get("channel"));
                value.add(activeJsonTask.getAsJsonObject().get(timeStamp)
                        .getAsJsonArray().get(i).getAsJsonObject().get("channel_value").getAsJsonArray().get(n)
                        .getAsJsonObject().get("value").toString());
            }
        }
        List<List<String>> output = new ArrayList<>();

        output.add(CHANNEL_INDEX, channelIds);
        output.add(VALUE_INDEX, value);
        return output;
    }

    /**
     * Tell the JsonPatchWorker to add the schedule given by the Translator.
     *
     * @param schedule return of the Translator
     */
    @Override
    public void handleNewSchedule(List<List<String>> schedule) {
        DateTime current = new DateTime();
        if (current.getDayOfMonth() != this.savedDate.getDayOfMonth()) {
            this.savedDate = current;
            this.jsonPatchWorker.deleteOldSchedules();
            this.minTime = DEFAULT;
            this.activeTask = DEFAULT;
            this.minDeltaTime = DEFAULT;
            this.lastUpdate = null;
        }
        this.jsonPatchWorker.addSchedule(schedule);
    }

    /**
     * Delete Channel if a Translator deactivates.
     *
     * @param channelId The Channel that has to be deleted from the Json
     */
    @Override
    public void deleteChannel(String channelId) {
        this.jsonPatchWorker.deleteChannel(channelId);
    }

    /**
     * Tell the JsonPatchWorker to add the fallback given by the Translator into the Fallback schedule.
     *
     * @param fallbackSchedule The Fallback Schedule part
     */
    @Override
    public void addFallbackSchedule(List<List<String>> fallbackSchedule) {
        this.jsonPatchWorker.addFallback(fallbackSchedule);
    }

    @Override
    public String debugLog() {
        return getStatus();
    }
}
