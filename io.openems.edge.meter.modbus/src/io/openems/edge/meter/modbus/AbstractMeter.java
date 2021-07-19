package io.openems.edge.meter.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public abstract class AbstractMeter extends AbstractOpenemsModbusComponent implements OpenemsComponent {


    protected ConfigurationAdmin cm;

    protected ComponentManager cpm;

    private static final String CONFIGURATION_SPLITTER = ":";
    private static final int CHANNEL_ID_POSITION = 0;
    private static final int ADDRESS_POSITION = 1;
    private static final int TASK_TYPE_POSITION = 2;
    //May increase in future when ChannelConverter is handled.
    private static final int EXCPECTED_SIZE = 3;

    private Map<String, Channel<?>> channelMap;
    private List<ModbusConfigWrapper> modbusConfig = new ArrayList<>();


    protected AbstractMeter(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
                               ConfigurationAdmin cm, String modbusReference, String modbusId, boolean configurationDone, ComponentManager cpm, List<String> channelToAddressList) throws OpenemsException {
        this.cpm = cpm;
        this.cm = cm;
        if (configurationDone) {
            this.configureChannelConfiguration(channelToAddressList);
            return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);
        }
        return true;
    }

    private void configureChannelConfiguration(List<String> channelToAddressList) {
        channelToAddressList.forEach(entry -> {
            String[] split = entry.split(CONFIGURATION_SPLITTER);
            if (split.length == EXCPECTED_SIZE) {
                String channelId = split[CHANNEL_ID_POSITION];
                int address = Integer.parseInt(split[ADDRESS_POSITION]);
                String taskType = split[TASK_TYPE_POSITION].trim().toUpperCase();

                Channel<?> channel = this.channelMap.get(channelId);
                TaskType type = null;
                if (TaskType.contains(taskType) && channel != null) {
                    type = TaskType.valueOf(taskType);
                    this.modbusConfig.add(new ModbusConfigWrapper(channel.channelId(), address, type));
                }

            } else {
                //TODO ERROR
            }

        });

    }


    /**
     * Update method available for Components using MQTT.
     *
     * @param config        config of the Component, will be updated automatically.
     * @param configTarget  target, where to put ChannelIds. Usually something like "ChannelIds".
     * @param channelsGiven Channels of the Component, collected by this.channels, filtered by "_Property"
     * @param length        of the configTarget entries. If Length doesn't match ChannelSize --> Update.
     * @return true if it does not need to update
     */
    public boolean update(Configuration config, String configTarget, List<Channel<?>> channelsGiven, int length) {
        List<Channel<?>> channels =
                channelsGiven.stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        channels.forEach(entry -> {
            this.channelMap.put(entry.channelId().id(), entry);
        });
        if (length != channels.size()) {
            this.updateConfig(config, configTarget, channels);
            return false;
        }
        return true;
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
            properties.put(configTarget, this.propertyInput(Arrays.toString(channelIdArray)));
            properties.put("TaskType", this.propertyInput(Arrays.toString(TaskType.values())));
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


    private static enum TaskType {
        READ_COIL, READ_REGISTER, WRITE_COIL, WRITE_REGISTER;

        public static boolean contains(String taskType) {
            for (TaskType type : TaskType.values()) {
                if (type.name().equals(taskType)) {
                    return true;
                }
            }
            return false;
        }
    }


    private class ModbusConfigWrapper {
        //Only UnsignedWordElement supported atm
        io.openems.edge.common.channel.ChannelId channelId;
        int modbusAddress;
        TaskType taskType;

        private ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId idForModbus, int modbusAddress, TaskType taskType) {
            this.channelId = idForModbus;
            this.modbusAddress = modbusAddress;
            this.taskType = taskType;
        }

        public io.openems.edge.common.channel.ChannelId getChannelId() {
            return this.channelId;
        }

        public int getModbusAddress() {
            return this.modbusAddress;
        }

        public TaskType getTaskType() {
            return this.taskType;
        }
    }
}
