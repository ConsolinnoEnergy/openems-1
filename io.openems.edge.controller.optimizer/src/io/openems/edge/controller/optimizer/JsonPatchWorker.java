package io.openems.edge.controller.optimizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This Patch worker patches the json together that all the different translators give him.
 * Has to be called from Optimizer.
 **/
class JsonPatchWorker {
    private final WriteChannel<String> jsonChannel;
    private String jsonString;
    private final JsonObject jsonObject = new JsonObject();
    private final WriteChannel<String> fallbackChannel;
    private String fallbackString;
    private final JsonObject fallbackObject = new JsonObject();
    private int taskPosition = 0;
    private List<String> lastTimestampArray;
    private final Logger log = LoggerFactory.getLogger(JsonPatchWorker.class);

    JsonPatchWorker(WriteChannel<String> jsonChannel, WriteChannel<String> fallbackChannel) {
        this.jsonChannel = jsonChannel;
        this.fallbackChannel = fallbackChannel;
    }

    /**
     * Writes new Schedule for the optimizer, if there were changes made to it.
     * @param jsonString The current schedule of the Optimizer.
     * @throws OpenemsError.OpenemsNamedException This should never happen.
     */
    void work(String jsonString) throws OpenemsError.OpenemsNamedException {
        if (this.jsonString != null && this.jsonString.equals(jsonString) == false) {
            this.jsonChannel.setNextWriteValue(this.jsonString);
        }
    }

    /**
     * Adds schedule given by Optimizer to the current schedule.
     * Takes the return Array of the "actual translation" and adds the final conversion step.
     *
     * @param schedule Schedule from the Translator
     */
    void addSchedule(List<List<String>> schedule) {
        if (schedule.get(Index.SCHEDULE_INDEX.getNumVal()).isEmpty() == false) {
            JsonArray newArray = splitList(schedule);
            JsonArray copy = newArray.deepCopy();
            if (this.jsonObject.size() > 0) {
                addToExistingObject(copy, this.jsonObject);

            } else {
                this.jsonObject.add("time", copy);
            }

            this.jsonString = this.jsonObject.toString();

            try {
                this.jsonChannel.setNextWriteValue(this.jsonString);
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.log.error("Error in addSchedule. OpenemsNamedException!");
            }
        }
    }

    /**
     * Adds new Elements in the already existing schedule.
     *
     * @param current the new Task that has to be added
     * @param old     The old Schedule that has to be altered
     */
    private void addToExistingObject(JsonArray current, JsonObject old) {
        for (int i = 0; i < current.size(); i++) {

            boolean alreadyHere = false;
            int n;
            JsonElement now;
            now = current.get(i);
            String time = ((JsonObject) now).keySet().iterator().next();
            int oldSize = old.get("time").getAsJsonArray().size();
            for (n = 0; n < oldSize; n++) {
                this.taskPosition = 0;
                if (old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time) != null) {
                    if (checkForDuplicateEntry(old, now, n, time)) {
                        //Doesn't do anything if the Task already exists with the same value
                        alreadyHere = true;
                    } else if (checkComponentChannelAlreadyHere(old, now, n, time)) {
                        //Modifies Component if its already in the Schedule
                        old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time).getAsJsonArray().get(this.taskPosition)
                                .getAsJsonObject().remove("channel_value");
                        old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time).getAsJsonArray().get(this.taskPosition)
                                .getAsJsonObject().add("channel_value", now.getAsJsonObject().get(time).getAsJsonArray()
                                .get(0).getAsJsonObject().get("channel_value"));
                        alreadyHere = true;
                    } else if (!alreadyHere) {
                        //Adds Component in schedule if its not existent in any way
                        old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time).getAsJsonArray()
                                .add(now.getAsJsonObject().get(time).getAsJsonArray().get(0));
                    }

                }
            }
            if (old.toString().contains("\"" + time + "\"") == false) {
                old.get("time").getAsJsonArray().add(now);
                }
        }
        if (lastTimestampArray.size() < old.get("time").getAsJsonArray().size()) {
            int hitTime = -1;
            for (int k = 0; k < old.get("time").getAsJsonArray().size(); k++) {
                if (lastTimestampArray.toString().contains(old.get("time").getAsJsonArray().get(k).getAsJsonObject().keySet().iterator().next()) == false) {
                    hitTime = k;
                }
            }
            if (hitTime != -1) {
                old.get("time").getAsJsonArray().remove(hitTime);
            }
        }
    }

    /**
     * Checks if the Component Already exists in the Schedule and has to be modified instead of added.
     *
     * @param old  The entire Schedule
     * @param now  The new Task
     * @param n    The current counter of the loop
     * @param time The timestamp String
     * @return true if the Component already exists in the Schedule
     */
    private boolean checkComponentChannelAlreadyHere(JsonObject old, JsonElement now, int n, String time) {
        this.taskPosition = 0;
        if (old.get("time").getAsJsonArray().get(n) != null && old.get("time").getAsJsonArray().get(n).getAsJsonObject()
                .get(time) != null && 0 < old.get("time").getAsJsonArray().get(n).getAsJsonObject()
                .get(time).getAsJsonArray().size()) {
            for (int i = 0; i < old.get("time").getAsJsonArray().get(n).getAsJsonObject()
                    .get(time).getAsJsonArray().size(); i++) {
                if (checkThisTask(old, n, time, now)) {
                    return true;
                } else {
                    this.taskPosition++;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the task we are looking for is in the current taskPosition.
     *
     * @param old  The entire Schedule
     * @param n    The current counter of the loop
     * @param time The timestamp String
     * @param now  The new Task
     * @return true if the Component now is in the taskPosition of old
     */
    private boolean checkThisTask(JsonObject old, int n, String time, JsonElement now) {
        return (old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time).getAsJsonArray()
                .get(this.taskPosition).getAsJsonObject().get("channel_value").getAsJsonArray().get(0).getAsJsonObject()
                .get("channel").toString().equals(
                        now.getAsJsonObject().get(time).getAsJsonArray().get(0)
                                .getAsJsonObject().get("channel_value").getAsJsonArray().get(0).getAsJsonObject()
                                .get("channel").toString())
                && old.get("time").getAsJsonArray().get(n).getAsJsonObject().get(time).getAsJsonArray()
                .get(this.taskPosition).getAsJsonObject().get("component").toString().equals(
                        now.getAsJsonObject().get(time).getAsJsonArray().get(0)
                                .getAsJsonObject().get("component").toString()));
    }

    /**
     * Checks if the task we are looking for is in the current taskPosition.
     *
     * @param old       The entire Schedule
     * @param i         The current counter in the timestamp loop
     * @param n         The current counter in the position loop
     * @param time      The timestamp String
     * @param component The Component ID as String
     * @param channel   The Channel ID as String
     * @return true if the Component now is in the taskPosition of old
     */
    private boolean checkThisTask(JsonObject old, int i, int n, String time, String component, String channel) {
        return old.get("time").getAsJsonArray().get(i).getAsJsonObject()
                .get(time).getAsJsonArray().get(n).getAsJsonObject()
                .get("component").toString().equals(component) && (old.get("time").getAsJsonArray().get(i).getAsJsonObject().get(time).getAsJsonArray()
                .get(n).getAsJsonObject().get("channel_value").getAsJsonArray().get(0).getAsJsonObject()
                .get("channel").toString().equals(
                        channel));
    }

    /**
     * This Checks, if the List already contains this element.
     *
     * @param old  The entire Schedule
     * @param now  The new Task
     * @param n    The current counter of the loop
     * @param time The timestamp String
     * @return true if the task already Exists exactly in that form
     */
    private boolean checkForDuplicateEntry(JsonObject old, JsonElement now, int n, String time) {
        return old.get("time")
                .getAsJsonArray().get(n).getAsJsonObject().get(time).toString().contains(now.getAsJsonObject().get(time).getAsJsonArray().get(0)
                        .toString());

    }


    /**
     * Splits the lists given by the translator and puts them in the correct Form for the Optimizer.
     * This technically means that this is the "real" translation.
     *
     * @param schedule Schedule given by translator
     * @return The JsonArray in the correct form for the optimizer.
     */
    private JsonArray splitList(List<List<String>> schedule) {
        lastTimestampArray = new ArrayList<>();
        JsonElement component = new JsonParser().parse(schedule.get(Index.COMPONENT_INDEX.getNumVal()).get(0));
        String channel = schedule.get(Index.CHANNEL_INDEX.getNumVal()).get(0);
        String enableChannel = null;
        if (schedule.size() == 4) {
            enableChannel = schedule.get(Index.ENABLE_CHANNEL_INDEX.getNumVal()).get(0);
        }
        JsonArray timeStamps;
        JsonArray value;
        JsonElement channelInternal;
        JsonElement enableInternal = null;
        JsonElement enableValueInternal = null;
        JsonElement valueInternal;
        JsonArray channelValueOutput = new JsonArray();
        JsonObject channelValueInternal = new JsonObject();
        JsonObject enableChannelValueInternal = new JsonObject();
        JsonArray timeArray = new JsonArray();
        JsonObject timeInternal = new JsonObject();
        JsonObject outputInternal = new JsonObject();
        JsonArray outputArray = new JsonArray();
        JsonObject output;
        JsonObject json = new Gson().fromJson(schedule.get(Index.SCHEDULE_INDEX.getNumVal()).get(0), JsonObject.class);
        timeStamps = json.getAsJsonArray("timestamps");
        value = json.getAsJsonArray("values");
        DateTime now = new DateTime();
        for (int i = 0; i < timeStamps.size(); i++) {
            String time;
            //Check if timestamp is today
            if (DateTime.parse(timeStamps.get(i).getAsString()).dayOfMonth().getAsString().equals(now.dayOfMonth().getAsString())) {
                //Save time in right Format
                //Should the minute be less then 10 then the translator HAS to save it at 00 and not 0 because 10:00 would appear as 01:00 instead
                if (DateTime.parse(timeStamps.get(i).getAsString()).getMinuteOfHour() < 10) {
                    time = DateTime.parse(timeStamps.get(i).getAsString()).getHourOfDay() + "0" + DateTime.parse(timeStamps.get(i).getAsString()).getMinuteOfHour() + "";
                } else {
                    time = DateTime.parse(timeStamps.get(i).getAsString()).getHourOfDay() + "" + DateTime.parse(timeStamps.get(i).getAsString()).getMinuteOfHour() + "";
                }
                channelInternal = new JsonParser().parse(channel);
                if (i >= value.size()) {
                    valueInternal = new JsonParser().parse("null");
                } else {
                    valueInternal = new JsonParser().parse(value.get(i).toString());
                }
                if (enableChannel != null) {
                    enableInternal = new JsonParser().parse(enableChannel);
                    if (valueInternal.toString().equals("null") == false) {
                        enableValueInternal = new JsonParser().parse("\"true\"");
                    } else {
                        enableValueInternal = new JsonParser().parse("\"false\"");
                    }
                }

                //add component to the Array that is behind the Timestamp
                timeInternal.add("component", component);
                //add the Channel value pair
                channelValueInternal.add("channel", channelInternal);
                channelValueInternal.add("value", valueInternal);
                //add a second channel value pair
                if (enableChannel != null) {
                    //need extra Element for this
                    enableChannelValueInternal.add("channel", enableInternal);
                    enableChannelValueInternal.add("value", enableValueInternal);
                    channelValueOutput.add(enableChannelValueInternal);
                }
                channelValueOutput.add(channelValueInternal);
                //add the channel_value pair the the Array that is behind the Timestamp
                timeInternal.add("channel_value", channelValueOutput);
                timeArray.add(timeInternal);
                //add the timestamp and the array
                outputInternal.add(time, timeArray);

                //deepCopy the Correct output out of the temporary one
                output = outputInternal.deepCopy();
                outputArray.add(output.deepCopy());
                //Reset Old Arrays
                timeInternal.remove("component");
                channelValueInternal.remove("channel");
                channelValueInternal.remove("value");
                enableChannelValueInternal.remove("channel");
                enableChannelValueInternal.remove("value");
                channelValueOutput.remove(enableChannelValueInternal);
                channelValueOutput.remove(channelValueInternal);
                timeInternal.remove("channel_value");
                timeArray.remove(timeInternal);
                outputInternal.remove(time);
                lastTimestampArray.add(time);
            }
        }

        return outputArray;
    }

    /**
     * Should the Optimizer notice that a Channel that is noted in the Json is not existent,
     * or if the Translator deactivates,
     * it will be removed from the Json.
     *
     * @param errorChannelId String of the ChannelID (e.g. Relay0/WriteOnOff)
     */
    void deleteChannel(String errorChannelId) {
        String component = errorChannelId.substring(0, errorChannelId.indexOf("/"));
        String channel = errorChannelId.substring(errorChannelId.indexOf("/") + 1);
        for (int i = 0; i < lastTimestampArray.size(); i++) {
            String time = lastTimestampArray.get(i);
            int objectSize = this.jsonObject.get("time").getAsJsonArray().get(i).getAsJsonObject().get(time).getAsJsonArray().size();
            for (int n = 0; n < objectSize; n++) {
                if (checkThisTask(this.jsonObject, i, n, time, component, channel)) {
                    this.jsonObject.get("time").getAsJsonArray().get(i).getAsJsonObject().get(time).getAsJsonArray().remove(n);
                }

            }

        }
        this.jsonString = jsonObject.toString();
    }


    /**
     * Creates the Fallback Schedule for the Optimizer.
     * @param fallbackSchedulePart The Fallback Schedule parts of the translators
     */
    void addFallback(List<List<String>> fallbackSchedulePart) {
        if (fallbackSchedulePart.get(Index.SCHEDULE_INDEX.getNumVal()).isEmpty() == false) {
            JsonArray newFallback = new JsonArray();
            JsonObject fallbackObject = new JsonObject();
            JsonArray newArray = splitList(fallbackSchedulePart);
            JsonArray copy = newArray.deepCopy();
            String timestamp = copy.getAsJsonArray().get(0).getAsJsonObject().keySet().iterator().next();
            //Add Fallback to 00:00 so its always the value the Optimizer will take as the active value
            fallbackObject.add("000",copy.getAsJsonArray().get(0).getAsJsonObject().get(timestamp));
            newFallback.add(fallbackObject);
            if (this.fallbackObject.size() > 0) {
                addToExistingObject(newFallback, this.fallbackObject);

            } else {
                this.fallbackObject.add("time", newFallback);
            }

            this.fallbackString = this.fallbackObject.toString();

            try {
                this.fallbackChannel.setNextWriteValue(this.fallbackString);
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.log.error("Error in fallbackSchedule. OpenemsNamedException!");
            }
        }
    }
}
