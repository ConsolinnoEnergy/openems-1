package io.openems.edge.bridge.modbus.api.generic;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.AbstractWordElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadrupleWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This Component allows a Simple implementation and use of Generic ModbusComponents.
 * Children can extend this Class and let the parent handle generic tasks.
 * Such as: Mapping and converting ChannelValues
 * Important: Extending Classes should have the following String[] Config entries (empty)
 * - taskType
 * - priorities
 * - wordType
 * - channelIds
 * ---
 * When Configuring the modbuschannel should be used since the children will map a modbuschannel to a "correct" channel.
 * E.g. if the generic ModbusMeter A gets it's reading by a DOUBLE -> the DOUBLE channel should be used for the reading.
 * If ModbusMeter B gets it's reading by a LONG -> the LONG channel should be used.
 * The Impl. of the Modbusmeter checks if a reading in the Long or Double is present, and sents it to this component.
 * The Correct Channel (Reading in the Meter) gets the value of the ModbusChannel multiplied by the configured scaleFactor.
 * In the End a generic Meter / HeatMeter etc... can be used by a e.g. a controller and does not have to distinct between a Generic and a concrete Modbus Meter.
 * Additionally add a String[] for config. Look up the package of modbus meter to see an example.
 */
public abstract class AbstractGenericModbusComponent extends AbstractOpenemsModbusComponent implements OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(AbstractGenericModbusComponent.class);

    protected AtomicReference<ConfigurationAdmin> cm = new AtomicReference<>();

    protected AtomicReference<ComponentManager> cpm = new AtomicReference<>();

    private static final String DEBUG_LOG_DIVIDER = "-----------";
    private static final String CONFIGURATION_SPLITTER = ":";
    private static final String TASK_TYPE_CONFIG = "taskType";
    private static final String PRIORITY_CONFIG = "priorities";
    private static final String WORD_TYPE_CONFIG = "wordType";
    private static final String WORD_ORDER_CONFIG = "wordOrder";
    private static final int CHANNEL_ID_POSITION = 0;
    private static final int ADDRESS_POSITION = 1;
    private static final int TASK_TYPE_POSITION = 2;
    private static final int WORD_TYPE_POSITION = 3;
    private static final int PRIORITY_OR_WORD_ORDER_POSITION = 4;
    //Either Length or ScaleFactor!
    private static final int LENGTH_POSITION = 5;
    private static final int EXPECTED_SIZE = 4;
    private static final int EXPECTED_SIZE_WITH_PRIORITY = 5;
    private static final int EXPECTED_SIZE_WITH_PRIORITY_AND_LENGTH = 6;

    private final Map<String, Channel<?>> channelMap = new HashMap<>();
    private final Map<io.openems.edge.common.channel.ChannelId, ModbusConfigWrapper> modbusConfig = new HashMap<>();


    /**
     * Declares what FC Task to use.
     */

    private enum TaskType {
        READ_COIL, READ_REGISTER, WRITE_COIL, WRITE_REGISTER;

        /**
         * Contains Method -> Check if the String matches an enum value.
         *
         * @param taskType the potential TaskType name
         * @return true if String matches a name
         */
        public static boolean contains(String taskType) {
            for (TaskType type : TaskType.values()) {
                if (type.name().equals(taskType)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The Word Type. Declares What type of WordElement to use.
     */
    private enum WordType {
        INT_16, INT_16_SIGNED, INT_32, INT_32_SIGNED, INT_64, INT_64_SIGNED,
        FLOAT_32, STRING, BOOLEAN, FLOAT_64
    }

    protected AbstractGenericModbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
                               ConfigurationAdmin cm, String modbusId, ComponentManager cpm, List<String> modbusConfiguration) throws OpenemsException, ConfigurationException {
        this.cpm.set(cpm);
        this.cm.set(cm);
        List<Channel<?>> channels =
                this.channels().stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        channels.forEach(entry -> {
            this.channelMap.put(entry.channelId().id(), entry);
        });
        this.configureChannelConfiguration(modbusConfiguration);
        return super.activate(context, id, alias, enabled, unitId, cm, "Modbus", modbusId);
    }

    protected boolean modified(ComponentContext context, String id, String alias, boolean enabled, int unitId,
                               ConfigurationAdmin cm, String modbusId, ComponentManager cpm, List<String> modbusConfiguration) throws OpenemsException, ConfigurationException {
        this.cpm.set(cpm);
        this.cm.set(cm);
        this.modbusConfig.clear();
        this.configureChannelConfiguration(modbusConfiguration);
        return super.modified(context, id, alias, enabled, unitId, cm, "Modbus", modbusId);
    }

    /**
     * Called on either activate or modified. It checks the configuration of the modbusChannel and applies it.
     * It creates the {@link #channelMap} that will be used by the {@link #defineModbusProtocol()} to create the ModbusTasks.
     *
     * @param modbusConfiguration the configuration of the child Config
     * @throws ConfigurationException if the ComponentConfiguration was wrong
     */
    private void configureChannelConfiguration(List<String> modbusConfiguration) throws ConfigurationException {
        ConfigurationException[] ex = {null};
        AtomicInteger index = new AtomicInteger(1);
        if (modbusConfiguration.size() > 0 && modbusConfiguration.get(0).equals("") == false) {
            modbusConfiguration.forEach(entry -> {
                if (ex[0] == null) {
                    String[] split = entry.split(CONFIGURATION_SPLITTER);
                    if (split.length >= EXPECTED_SIZE && split.length <= EXPECTED_SIZE_WITH_PRIORITY_AND_LENGTH) {
                        String channelId = split[CHANNEL_ID_POSITION];
                        int address = Integer.parseInt(split[ADDRESS_POSITION]);

                        WordType wordType = null;
                        for (WordType type : WordType.values()) {
                            if (type.name().equals(split[WORD_TYPE_POSITION].trim().toUpperCase())) {
                                wordType = type;
                                break;
                            }
                        }
                        if (wordType == null) {
                            ex[0] = new ConfigurationException("configureChannelConfiguration in " + super.id(), "Wrong WordType: " + split[WORD_TYPE_POSITION]);
                        }
                        Channel<?> channel = this.channelMap.get(channelId);
                        WordOrder wordOrder = null;
                        Priority priority = null;

                        int length = 0;
                        if (split.length >= EXPECTED_SIZE_WITH_PRIORITY) {
                            //only numbers == length <- when no Priority is set
                            if (split[PRIORITY_OR_WORD_ORDER_POSITION].matches("([+-][0-9]*)")) {
                                try {
                                    length = Integer.parseInt(split[LENGTH_POSITION]);
                                } catch (NumberFormatException e) {
                                    ex[0] = new ConfigurationException("configureChannelConfiguration in "
                                            + super.id(), "Wrong length entry: " + split[PRIORITY_OR_WORD_ORDER_POSITION]);
                                }
                            } else {
                                String priorityOrWordOrderString = split[PRIORITY_OR_WORD_ORDER_POSITION].trim().toUpperCase();
                                for (Priority prio : Priority.values()) {
                                    if (prio.name().equals(priorityOrWordOrderString)) {
                                        priority = prio;
                                        break;
                                    }
                                }
                                if (priority == null) {
                                    for (WordOrder wordOrder1 : WordOrder.values()) {
                                        if (wordOrder1.name().equals(priorityOrWordOrderString)) {
                                            wordOrder = wordOrder1;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (split.length == EXPECTED_SIZE_WITH_PRIORITY_AND_LENGTH) {
                            try {
                                length = Integer.parseInt(split[LENGTH_POSITION]);
                            } catch (NumberFormatException e) {
                                ex[0] = new ConfigurationException("configureChannelConfiguration in "
                                        + super.id(), "Wrong length entry: " + split[LENGTH_POSITION]);
                            }
                        }
                        String taskType = split[TASK_TYPE_POSITION].trim().toUpperCase();
                        TaskType type = null;
                        if (TaskType.contains(taskType) && channel != null) {
                            type = TaskType.valueOf(taskType);
                            if (type.equals(TaskType.WRITE_REGISTER)) {
                                this.modbusConfig.put(channel.channelId(), new ModbusConfigWrapper(channel.channelId(), address, type, wordOrder, wordType, length));
                            } else {
                                if (priority != null) {
                                    this.modbusConfig.put(channel.channelId(), new ModbusConfigWrapper(channel.channelId(), address, type, priority, wordType, length));
                                } else {
                                    this.modbusConfig.put(channel.channelId(), new ModbusConfigWrapper(channel.channelId(), address, type, wordType, length));
                                }
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

    }


    /**
     * Update method available for Components using MQTT.
     *
     * @param ca            The ConfigurationAdmin this class will need for updating the Configuration
     * @param configTarget  target, where to put ChannelIds. Usually something like "ChannelIds".
     * @param channelsGiven Channels of the Component, collected by this.channels, filtered by "_Property"
     * @param length        of the configTarget entries. If Length doesn't match ChannelSize --> Update.
     * @return true if it does not need to update
     */
    public boolean update(ConfigurationAdmin ca, String configTarget, List<Channel<?>> channelsGiven, int length) throws IOException {
        this.channelMap.clear();
        List<Channel<?>> channels =
                channelsGiven.stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        channels.forEach(entry -> {
            this.channelMap.put(entry.channelId().id(), entry);
        });
        if (length != channels.size()) {
            this.updateConfig(ca.getConfiguration(this.servicePid(), "?"), configTarget, channels);
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
        String[] channelIdWithUnitArray = new String[channels.size()];
        channels.forEach(channel -> channelIdWithUnitArray[counter.getAndIncrement()] = channel.channelId().id()
                + CONFIGURATION_SPLITTER + " Unit is " + CONFIGURATION_SPLITTER + channel.channelDoc().getUnit());

        try {
            Dictionary<String, Object> properties = config.getProperties();
            properties.put(configTarget, this.propertyInput(Arrays.toString(channelIdWithUnitArray)));
            properties.put(TASK_TYPE_CONFIG, this.propertyInput(Arrays.toString(TaskType.values())));
            properties.put(PRIORITY_CONFIG, this.propertyInput(Arrays.toString(Priority.values())));
            properties.put(WORD_TYPE_CONFIG, this.propertyInput(Arrays.toString(WordType.values())));
            properties.put(WORD_ORDER_CONFIG, this.propertyInput(Arrays.toString(WordOrder.values())));
            config.update(properties);

        } catch (IOException e) {
            this.log.warn("Couldn't update the Config: " + e.getMessage() + "\n because: " + e.getCause());
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

    /**
     * Instances of this classes are created when splitting the configuration.
     * It holds information of each Config entry for the ModbusProtocol.
     * It contains:
     * <ul>
     * <li> The ChannelId</li>
     * <li>ModbusRegister</li>
     * <li>TaskType</li>
     * <li>Priority</li>
     * <li>WordType</li>
     * <li>Length for String or ScaleFactor</li>
     * </ul>
     */
    private static class ModbusConfigWrapper {

        private final io.openems.edge.common.channel.ChannelId channelId;
        private final int modbusAddress;
        private final TaskType taskType;
        private Priority priority;
        private WordOrder wordOrder;
        private final WordType wordType;
        private final int length;


        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType, Priority priority, WordType wordType, int length) {
            this.channelId = channelId;
            this.modbusAddress = modbusAddress;
            this.taskType = taskType;
            this.priority = priority;
            this.wordType = wordType;
            this.length = length;
        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType) {
            this(channelId, modbusAddress, taskType, Priority.LOW, WordType.INT_16);

        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType, WordType wordType) {
            this(channelId, modbusAddress, taskType, Priority.LOW, wordType);
        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int modbusAddress, TaskType taskType, Priority priority, WordType wordType) {
            this(channelId, modbusAddress, taskType, priority, wordType, 1);
        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int address, TaskType type, WordType wordType, int length) {
            this(channelId, address, type, Priority.LOW, wordType, length);
        }

        public ModbusConfigWrapper(io.openems.edge.common.channel.ChannelId channelId, int address, TaskType type, WordOrder wordOrder, WordType wordType, int length) {
            this.channelId = channelId;
            this.modbusAddress = address;
            this.taskType = type;
            this.wordType = wordType;
            this.length = length;
            this.wordOrder = wordOrder;
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

        public WordOrder getWordOrder() {
            return this.wordOrder;
        }

        public int getStringLengthOrScaleFactor() {
            return this.length;
        }
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this);
        OpenemsException[] ex = {null};
        //previously configured ModbusConfig -> usually from child Configuration
        this.modbusConfig.forEach((key, entry) -> {
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
                            this.addReadRegister(protocol, entry);
                            break;
                        case WRITE_REGISTER:
                            this.addWriteRegister(protocol, entry);
                            break;
                        case WRITE_COIL:
                            protocol.addTask(new FC5WriteCoilTask(entry.getModbusAddress(),
                                    new CoilElement(entry.getModbusAddress())));
                            break;
                    }
                }
            } catch (OpenemsException e) {
                this.log.warn("Couldn't apply ModbusConfig for : " + entry.channelId);
                ex[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        return protocol;
    }

    /**
     * Adds WriteRegister to a ModbusProtocol.
     *
     * @param protocol the protocol, where Tasks should be added.
     * @param wrapper  a ModbusConfig Wrapper, usually from AbstractGenericModbusComponent
     * @throws OpenemsException if adding a task fails.
     */
    private void addWriteRegister(ModbusProtocol protocol, ModbusConfigWrapper wrapper) throws OpenemsException {
        AbstractWordElement<?, ?> element = null;
        AbstractModbusElement<?> element1 = null;

        int address = wrapper.getModbusAddress();
        switch (wrapper.getWordType()) {
            case INT_16:
                element = new UnsignedWordElement(address);
                protocol.addTask(new FC6WriteRegisterTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element)));
                break;
            case INT_16_SIGNED:
                element = new SignedWordElement(address);
                protocol.addTask(new FC6WriteRegisterTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element)));
                break;
            case INT_32:
                element1 = new UnsignedDoublewordElement(address).wordOrder(wrapper.getWordOrder());
                protocol.addTask(new FC16WriteRegistersTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element1)));
                break;
            case INT_32_SIGNED:
                element1 = new SignedDoublewordElement(address).wordOrder(wrapper.getWordOrder());
                protocol.addTask(new FC16WriteRegistersTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element1)));
                break;
            case FLOAT_32:
                element1 = new FloatDoublewordElement(address).wordOrder(wrapper.getWordOrder());
                protocol.addTask(new FC16WriteRegistersTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element1)));
                break;
            case INT_64:
                element1 = new UnsignedQuadruplewordElement(address).wordOrder(wrapper.getWordOrder());
                protocol.addTask(new FC16WriteRegistersTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element1)));
                break;
            case INT_64_SIGNED:
                element1 = new SignedQuadruplewordElement(address).wordOrder(wrapper.getWordOrder());
                protocol.addTask(new FC16WriteRegistersTask(wrapper.getModbusAddress(),
                        m(wrapper.getChannelId(), element1)));
                break;

        }
    }

    /**
     * Adds a Register (Read or Write) to the ModbusProtocol, distinct what {@link AbstractModbusElement} needs to be
     * used.
     *
     * @param protocol The ModbusProtocol usually from this {@link #defineModbusProtocol()}.
     * @param wrapper  the ModbusConfigWrapper, usually from the {@link #modbusConfig}
     * @throws OpenemsException if the addTask fails.
     */
    private void addReadRegister(ModbusProtocol protocol, ModbusConfigWrapper wrapper) throws OpenemsException {
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
                break;
            case FLOAT_64:
                element = new FloatQuadrupleWordElement(address);
                break;
            case STRING:
                element = new StringWordElement(address, wrapper.getStringLengthOrScaleFactor());
                break;
        }
        protocol.addTask(new FC4ReadInputRegistersTask(wrapper.getModbusAddress(), wrapper.getPriority(),
                m(wrapper.getChannelId(), element)));
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
        if (source != null) {
            if (this.modbusConfig.containsKey(source.channelId())) {
                Value<?> targetValue = source.getNextValue();
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
                            int scaleFactor = this.modbusConfig.get(source.channelId()).getStringLengthOrScaleFactor();
                            double targetSetValue = (Double) targetValue.get() * Math.pow(10, scaleFactor);
                            switch (target.getType()) {

                                case BOOLEAN:
                                    target.setNextValue(((int) targetSetValue) > 0);
                                    break;
                                case SHORT:
                                    target.setNextValue((short) targetSetValue);
                                    break;
                                case INTEGER:
                                    target.setNextValue((int) targetSetValue);
                                    break;
                                case LONG:
                                    target.setNextValue((long) targetSetValue);
                                    break;
                                case FLOAT:
                                    target.setNextValue((float) targetSetValue);
                                    break;
                                case DOUBLE:
                                case STRING:
                                    target.setNextValue(targetSetValue);
                                    break;
                            }

                            break;
                    }
                }
            }
        }
    }


    /**
     * Writes from the real Channel entry to the ModbusChannel.
     *
     * @param target the targetChannel to update the Value to
     * @param source the source Channel where the original Value is written
     */
    protected boolean handleChannelWriteFromOriginalToModbus(WriteChannel<?> target, WriteChannel<?> source) {

        if (this.modbusConfig.containsKey(target.channelId())) {
            int scaleFactor;
            double targetSetValue = 0.0d;
            Optional<?> targetValue = source.getNextWriteValueAndReset();
            if (targetValue.isPresent()) {
                try {
                    switch (source.channelDoc().getType()) {
                        case BOOLEAN:
                            target.setNextWriteValueFromObject(targetValue.get());
                            targetSetValue = (Boolean) targetValue.get() ?  1 : 0;
                            break;
                        case STRING:
                            target.setNextWriteValueFromObject(targetValue.get());
                            targetSetValue = Double.parseDouble((String)targetValue.get());
                            break;
                        case SHORT:
                            scaleFactor = this.modbusConfig.get(target.channelId()).getStringLengthOrScaleFactor();
                            targetSetValue = ((Short) targetValue.get()).doubleValue() * Math.pow(10, scaleFactor);
                            break;
                        case INTEGER:
                            scaleFactor = this.modbusConfig.get(target.channelId()).getStringLengthOrScaleFactor();
                            targetSetValue = ((Integer) targetValue.get()).doubleValue() * Math.pow(10, scaleFactor);
                            break;
                        case LONG:
                            scaleFactor = this.modbusConfig.get(target.channelId()).getStringLengthOrScaleFactor();
                            targetSetValue = ((Long) targetValue.get()).doubleValue() * Math.pow(10, scaleFactor);
                            break;
                        case FLOAT:
                            scaleFactor = this.modbusConfig.get(target.channelId()).getStringLengthOrScaleFactor();
                            targetSetValue = ((Float) targetValue.get()).doubleValue() * Math.pow(10, scaleFactor);
                            break;
                        case DOUBLE:
                        default:
                            scaleFactor = this.modbusConfig.get(target.channelId()).getStringLengthOrScaleFactor();
                            targetSetValue = (Double) targetValue.get() * Math.pow(10, scaleFactor);
                            break;
                    }
                    switch (target.getType()) {

                        case BOOLEAN:
                            target.setNextWriteValueFromObject(((int) targetSetValue) > 0);
                            break;
                        case SHORT:
                            target.setNextWriteValueFromObject((short) targetSetValue);
                            break;
                        case INTEGER:
                            target.setNextWriteValueFromObject((int) targetSetValue);
                            break;
                        case LONG:
                            target.setNextWriteValueFromObject((long) targetSetValue);
                            break;
                        case FLOAT:
                            target.setNextWriteValueFromObject((float) targetSetValue);
                            break;
                        case DOUBLE:
                        case STRING:
                            target.setNextWriteValueFromObject(targetSetValue);
                            break;
                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find target Channel, please check your configuration/Component/Code: " + e.getMessage());
                    return false;
                }
            } else {
                this.log.info("No TargetValue for nextWrite Available: " + target.channelId());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return this ModbusConfig
     *
     * @return the modbus Config.
     */
    protected Map<io.openems.edge.common.channel.ChannelId, ModbusConfigWrapper> getModbusConfig() {
        return this.modbusConfig;
    }
}
