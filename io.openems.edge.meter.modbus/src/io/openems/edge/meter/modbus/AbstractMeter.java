package io.openems.edge.meter.modbus;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public abstract class AbstractMeter extends AbstractOpenemsModbusComponent implements OpenemsComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractMeter.class);

    protected AtomicReference<ConfigurationAdmin> cm = new AtomicReference<>();

    protected AtomicReference<ComponentManager> cpm = new AtomicReference<>();

    private static final String DEBUG_LOG_DIVIDER = "-----------";
    private static final String CONFIGURATION_SPLITTER = ":";
    private static final String TASK_TYPE_CONFIG = "taskType";
    private static final String PRIORITY_CONFIG = "priorities";
    private static final String WORD_TYPE_CONFIG = "wordType";
    private static final int CHANNEL_ID_POSITION = 0;
    private static final int ADDRESS_POSITION = 1;
    private static final int TASK_TYPE_POSITION = 2;
    private static final int WORD_TYPE_POSITION = 3;
    private static final int PRIORITY_POSITION = 4;
    private static final int EXPECTED_SIZE = 4;
    private static final int EXPECTED_SIZE_WITH_PRIORITY = 5;

    private final Map<String, Channel<?>> channelMap = new HashMap<>();
    private final List<ModbusConfigWrapper> modbusConfig = new ArrayList<>();


    private enum TaskType {
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

    private enum WordType {
        INT_16, INT_16_SIGNED, INT_32, INT_32_SIGNED, INT_64, INT_64_SIGNED,
        FLOAT_32;
        //FLOAT_64
    }

    private enum RegisterType {
        READ, WRITE
    }

    protected AbstractMeter(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
                               ConfigurationAdmin cm, String modbusId, ComponentManager cpm, List<String> channelToAddressList) throws OpenemsException {
        this.cpm.set(cpm);
        this.cm.set(cm);
        try {
            this.configureChannelConfiguration(channelToAddressList);
        } catch (ConfigurationException e) {
            return true;
        }
        return super.activate(context, id, alias, enabled, unitId, cm, "Modbus", modbusId);
    }

    protected boolean modified(ComponentContext context, String id, String alias, boolean enabled, int unitId,
                               ConfigurationAdmin cm, String modbusId, ComponentManager cpm, List<String> channelToAddressList) throws OpenemsException {
        this.cpm.set(cpm);
        this.cm.set(cm);
        try {
            this.configureChannelConfiguration(channelToAddressList);
        } catch (ConfigurationException e) {
            return true;
        }
        return super.modified(context, id, alias, enabled, unitId, cm, "Modbus", modbusId);
    }


    private void configureChannelConfiguration(List<String> channelToAddressList) throws ConfigurationException {
        ConfigurationException[] ex = {null};
        AtomicInteger index = new AtomicInteger(1);
        channelToAddressList.forEach(entry -> {
            if (ex[0] == null) {
                String[] split = entry.split(CONFIGURATION_SPLITTER);
                if (split.length == EXPECTED_SIZE || split.length == EXPECTED_SIZE_WITH_PRIORITY) {
                    String channelId = split[CHANNEL_ID_POSITION];
                    int address = Integer.parseInt(split[ADDRESS_POSITION]);
                    String taskType = split[TASK_TYPE_POSITION].trim().toUpperCase();
                    WordType wordType = null;
                    for (WordType type : WordType.values()) {
                        if (type.name().equals(split[WORD_TYPE_POSITION].trim().toUpperCase())) {
                            wordType = type;
                        }
                    }
                    if (wordType == null) {
                        ex[0] = new ConfigurationException("configureChannelConfiguration in " + super.id(), "Wrong WordType: " + split[WORD_TYPE_POSITION]);
                    }
                    Channel<?> channel = this.channelMap.get(channelId);
                    TaskType type = null;
                    Priority priority = null;
                    if (split.length == EXPECTED_SIZE_WITH_PRIORITY) {
                        String priorityString = split[PRIORITY_POSITION].trim().toUpperCase();
                        for (Priority prio : Priority.values()) {
                            if (prio.name().equals(priorityString)) {
                                priority = prio;
                            }
                        }
                    }
                    if (TaskType.contains(taskType) && channel != null) {
                        type = TaskType.valueOf(taskType);
                        if (priority != null) {
                            this.modbusConfig.add(new ModbusConfigWrapper(channel.channelId(), address, type, priority, wordType));
                        } else {
                            this.modbusConfig.add(new ModbusConfigWrapper(channel.channelId(), address, type, wordType));
                        }
                    } else {
                        ex[0] = new ConfigurationException("Configure Channel Configuration: " + super.id(), "Either Type is null or Channel not Available: For Entry: "
                                + index.get() + " Channel: " + (channel == null ? "null" : channel) + " Attempted TaskType: " + taskType);
                    }
                } else {
                    ex[0] = new ConfigurationException("Configure Channel Configuration: " + super.id(), "Expected Configuration Size of : "
                            + EXPECTED_SIZE + " But was: " + split.length);
                }
            }
            index.getAndIncrement();

        });
        if (ex[0] != null) {
            throw ex[0];
        }

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
            properties.put(TASK_TYPE_CONFIG, this.propertyInput(Arrays.toString(TaskType.values())));
            properties.put(PRIORITY_CONFIG, this.propertyInput(Arrays.toString(Priority.values())));
            properties.put(WORD_TYPE_CONFIG, this.propertyInput(Arrays.toString(WordType.values())));
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


    private class ModbusConfigWrapper {

        io.openems.edge.common.channel.ChannelId channelId;
        int modbusAddress;
        TaskType taskType;
        Priority priority;
        WordType wordType;


        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType, Priority priority, WordType wordType) {
            this.channelId = channelId;
            this.modbusAddress = modbusAddress;
            this.taskType = taskType;
            this.priority = priority;
            this.wordType = wordType;
        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType) {
            this(channelId, modbusAddress, taskType, Priority.LOW, WordType.INT_16);

        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType, WordType wordType) {
            this(channelId, modbusAddress, taskType, Priority.LOW, wordType);
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

        public Priority getPriority() {
            return this.priority;
        }

        public WordType getWordType() {
            return this.wordType;
        }
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this);
        OpenemsException[] ex = {null};
        this.modbusConfig.forEach(entry -> {
            try {
                if (ex[0] == null) {
                    switch (entry.getTaskType()) {

                        case READ_COIL:
                            protocol.addTask(
                                    new FC1ReadCoilsTask(entry.modbusAddress, entry.getPriority(),
                                            m(entry.getChannelId(), new CoilElement(entry.modbusAddress)))
                            );
                            break;
                        case READ_REGISTER:
                            this.addRegister(protocol, entry, RegisterType.READ);
                            break;
                        case WRITE_COIL:
                            protocol.addTask(new FC5WriteCoilTask(entry.getModbusAddress(),
                                    new CoilElement(entry.getModbusAddress())));
                            break;
                        case WRITE_REGISTER:
                            this.addRegister(protocol, entry, RegisterType.WRITE);
                            break;
                    }
                }
            } catch (OpenemsException e) {
                log.warn("Couldn't apply ModbusConfig for : " + entry.channelId);
                ex[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        return protocol;
    }

    private void addRegister(ModbusProtocol protocol, ModbusConfigWrapper wrapper, RegisterType registerType) throws OpenemsException {
        AbstractModbusElement<?> element = null;
        int address = wrapper.getModbusAddress();

        switch (wrapper.getWordType()) {

            case INT_16:
                element = new UnsignedWordElement(address);
                break;
            case INT_16_SIGNED:
                element = new SignedWordElement(address);
                break;
            case INT_32:
                element = new UnsignedDoublewordElement(address);
                break;
            case INT_32_SIGNED:
                element = new SignedDoublewordElement(address);
                break;
            case INT_64:
                element = new UnsignedQuadruplewordElement(address);
                break;
            case INT_64_SIGNED:
                element = new SignedQuadruplewordElement(address);
                break;
            case FLOAT_32:
                element = new FloatDoublewordElement(address);
                break;/* case FLOAT_64:
                element = new FloatQuadroubleWordElement(address);
                break;
                        */
        }

        if (registerType.equals(RegisterType.READ)) {
            protocol.addTask(new FC4ReadInputRegistersTask(wrapper.getModbusAddress(), wrapper.getPriority(),
                    m(wrapper.getChannelId(), element)));
        } else {
            protocol.addTask(new FC6WriteRegisterTask(wrapper.getModbusAddress(),
                    m(wrapper.getChannelId(), element)));
        }
    }

    @Override
    public String debugLog() {
        StringBuilder builder = new StringBuilder();
        builder.append(DEBUG_LOG_DIVIDER).append(super.id()).append(DEBUG_LOG_DIVIDER).append("\n");
        this.channels().stream().filter(channel -> channel.value().isDefined()).forEach(entry
                -> builder.append("Channel: ").append(entry.channelId()).append(" Value: ").append(entry.value()).append("\n"));
        builder.append(DEBUG_LOG_DIVIDER).append(DEBUG_LOG_DIVIDER).append("\n");
        return builder.toString();
    }

    /**
     * This will be called by extending classes. The Value of the channel will be written into the channel, depending on the unit.
     *
     * @param target the target channel (the original channel -> "correct" meter data
     * @param source the source channel (the channel that gets information via modbus -> adapted for "correct" meter data
     */
    protected void handleChannelUpdate(Channel<?> target, Channel<?> source) {
        Value<?> targetValue = source.value();
        Unit targetUnit = target.channelDoc().getUnit();
        Unit sourceUnit = source.channelDoc().getUnit();
        if (targetValue.isDefined()) {
            switch (source.channelDoc().getType()) {
                case BOOLEAN:
                case STRING:
                    target.setNextValue(targetValue.get());
                    break;
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    int scaleFactor = sourceUnit.getScaleFactor() - targetUnit.getScaleFactor();
                    target.setNextValue(((double)targetValue.get() * Math.pow(10, scaleFactor)));
                    break;
            }
        }
    }

}
