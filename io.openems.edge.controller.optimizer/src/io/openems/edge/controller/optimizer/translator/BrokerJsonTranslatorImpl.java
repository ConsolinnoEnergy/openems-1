package io.openems.edge.controller.optimizer.translator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.Schedule;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.optimizer.Index;
import io.openems.edge.controller.optimizer.api.Optimizer;
import org.joda.time.DateTime;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.controller.optimizer.translator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class BrokerJsonTranslatorImpl extends AbstractOpenemsComponent implements OpenemsComponent, Schedule, EventHandler {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    private String componentId;
    private String channelId;
    private boolean enable;
    private String enableChannel;
    private String fallback;
    private Optimizer optimizer;
    private static final int RELOAD_SCHEDULE_TIME = 15;
    private DateTime now = new DateTime();
    private String lastSchedule;
    private String fallbackString;
    private boolean configurationDone;

    public BrokerJsonTranslatorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Schedule.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, IOException {
        this.componentId = config.componentId();
        this.enable = config.enableSignal();
        this.enableChannel = config.enableChannel();
        this.channelId = config.componentChannel();
        this.lastSchedule = null;
        String optimizerId = config.optimizerId();
        this.optimizer = cpm.getComponent(optimizerId);
        this.optimizer.handleNewSchedule(createSchedule());
        this.fallback = config.fallback();
        this.configurationDone = config.configurationDone();
        List<List<String>> fallbackSchedule;
        fallbackSchedule = createSchedule();
        optimizer.addFallbackSchedule(fallbackSchedule);

        super.activate(context, config.id(), config.alias(), config.enabled());
        update(cm.getConfiguration(this.servicePid(), "?"), "channelIdList", new ArrayList<>(cpm.getComponent(this.componentId).channels()), config.channelIdList().length);

    }

    /**
     * Reads the Schedule send by the Broker and puts the important information into a form the optimizer is able to work with.
     *
     * @return List of Lists. [componentId , [schedule] , channelId , (optional)EnableChannelID]
     */
    private List<List<String>> createSchedule() {
        List<String> enableChannel = new ArrayList<>();
        List<String> component = new ArrayList<>();
        List<String> channel = new ArrayList<>();
        component.add(this.componentId);
        channel.add(this.channelId);
        List<String> schedule = new ArrayList<>();
        if (this.getSchedule().getNextValue().isDefined()) {
            schedule.add(this.getSchedule().getNextValue().get());
        }
        List<List<String>> outputList = new ArrayList<>();
        outputList.add(Index.COMPONENT_INDEX.getNumVal(), component);
        outputList.add(Index.SCHEDULE_INDEX.getNumVal(), schedule);
        outputList.add(Index.CHANNEL_INDEX.getNumVal(), channel);
        if (this.enable) {
            enableChannel.add(this.enableChannel);
            outputList.add(Index.ENABLE_CHANNEL_INDEX.getNumVal(), enableChannel);
        }
        return outputList;
    }

    @Deactivate
    public void deactivate() {
        optimizer.deleteChannel("\"" + componentId + "\"" + "/" + "\"" + channelId + "\"");
        if (this.enable) {
            optimizer.deleteChannel("\"" + componentId + "\"" + "/" + "\"" + enableChannel + "\"");
        }
        super.deactivate();

    }

    @Override
    public void handleEvent(Event event) {
        if (this.configurationDone) {
            List<List<String>> newSchedule;
            newSchedule = createSchedule();
            if (lastSchedule == null) {
                if (newSchedule.isEmpty() == false) {
                    optimizer.handleNewSchedule(newSchedule);
                    this.lastSchedule = newSchedule.toString();
                }
            } else if (newSchedule.isEmpty() == false && lastSchedule.equals(newSchedule.toString()) == false) {
                optimizer.handleNewSchedule(newSchedule);
                this.lastSchedule = newSchedule.toString();

                //Periodically tells the Optimizer to reload the Schedule even if it is the same one.
                //Mainly used to update after the Day changes at 00:00
            } else if (now.plusMinutes(RELOAD_SCHEDULE_TIME).getMinuteOfDay() >= new DateTime().getMinuteOfDay()) {
                optimizer.handleNewSchedule(newSchedule);
                now = new DateTime();
            }
            if (fallbackString == null) {
                if (this.fallback != null) {
                    List<List<String>> fallbackSchedule;
                    fallbackSchedule = createFallbackSchedule();
                    optimizer.addFallbackSchedule(fallbackSchedule);
                    fallbackString = fallbackSchedule.toString();
                }
            }
        }
    }

    /**
     * Creates Schedule based on the Fallback Value. The Date is unimportant, but the Scheduler needs something.
     *
     * @return List of Lists. [componentId , [schedule] , channelId , (optional)EnableChannelID]
     */
    private List<List<String>> createFallbackSchedule() {
        List<String> enableChannel = new ArrayList<>();
        List<String> component = new ArrayList<>();
        List<String> channel = new ArrayList<>();
        component.add(this.componentId);
        channel.add(this.channelId);
        List<String> schedule = new ArrayList<>();
        String scheduleString = "{\"timestamps\":[\"" + new DateTime() + "\"],\"values\":[" + this.fallback + "],\"unit\":\"Fallback\",\"expiration\":null}";
        schedule.add(scheduleString);
        List<List<String>> outputList = new ArrayList<>();
        outputList.add(Index.COMPONENT_INDEX.getNumVal(), component);
        outputList.add(Index.SCHEDULE_INDEX.getNumVal(), schedule);
        outputList.add(Index.CHANNEL_INDEX.getNumVal(), channel);
        if (this.enable) {
            enableChannel.add(this.enableChannel);
            outputList.add(Index.ENABLE_CHANNEL_INDEX.getNumVal(), enableChannel);
        }
        return outputList;
    }

    /**
     * Update method available for Components using MQTT.
     *
     * @param config        config of the Component, will be updated automatically.
     * @param configTarget  target, where to put ChannelIds. Usually something like "ChannelIds".
     * @param channelsGiven Channels of the Component, collected by this.channels, filtered by "_Property"
     * @param length        length of the configTarget entries. If Length doesn't match ChannelSize --> Update.
     */
    public void update(Configuration config, String configTarget, List<Channel<?>> channelsGiven, int length) {
        List<Channel<?>> channels =
                channelsGiven.stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        if (length != channels.size()) {
            this.updateConfig(config, configTarget, channels);
        }
    }


    /**
     * Update Config and if successful you can initialize the MqttComponent.
     *
     * @param config       Configuration of the OpenemsComponent
     * @param configTarget usually from Parent-->Config.
     * @param channels     usually from Parent --> Channels.
     */

    private void updateConfig(Configuration config, String configTarget, List<Channel<?>> channels) {
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());

        try {
            Dictionary<String, Object> properties = config.getProperties();
            properties.put(configTarget, propertyInput(Arrays.toString(channelIdArray)));
            config.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Workaround for OSGi Arrays to String --> Otherwise it won't be correct.
     *
     * @param types OpenemsTypes etc
     * @return String Array which will be put to new Config
     */
    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        types = types.replace(" ", "");
        return types.split(",");
    }


}