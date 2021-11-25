package io.openems.edge.controller.filewriter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Filewriter.InventoryList", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class LeafletInventoryListFileWriter extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    @Reference
    ConfigurationAdmin ca;

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(LeafletInventoryListFileWriter.class);

    private final Map<String, ChannelAddress> keyValuePair = new HashMap<>();
    //ChannelId -> Value
    private final Map<String, String> channelIdToValueMap = new HashMap<>();
    private static final String CONFIG_SPLIT = ":";
    private static final int CONFIG_SPLIT_LENGTH = 2;
    private static final String LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR = "=";
    private static final int CONFIG_KEY_ENTRY_POSITION = 0;
    private static final int CONFIG_VALUE_ENTRY_POSITION = 1;
    private OpenemsComponent otherComponent;
    List<Channel<?>> otherComponentChannelList = new ArrayList<>();
    List<String> channelIds = new ArrayList<>();
    private String filePath;
    private boolean writtenToPath = false;
    private boolean configSuccess = false;
    private FileWriter fileWriter;
    private FileReader fileReader;
    Config config;

    public LeafletInventoryListFileWriter() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException, IOException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException {
        this.config = config;
        this.otherComponent = this.cpm.getComponent(config.otherComponentId());
        this.keyValuePair.clear();
        this.channelIds.clear();
        this.channelIdToValueMap.clear();
        this.update();
        if (this.config.configurationDone()) {
            ConfigurationException[] ex = {null};
            Arrays.asList(this.config.keyValuePairs()).forEach(entry -> {
                if (ex[0] == null) {
                    if (!entry.contains(CONFIG_SPLIT)) {
                        ex[0] = new ConfigurationException("activationOrModifiedRoutine", "ConfigSplitter: \"" + CONFIG_SPLIT + "\" is missing between key and value at: " + entry);
                    } else {
                        String[] entries = entry.split(CONFIG_SPLIT);
                        if (entries.length == CONFIG_SPLIT_LENGTH) {
                            String key = entries[CONFIG_KEY_ENTRY_POSITION];
                            String value = entries[CONFIG_VALUE_ENTRY_POSITION];
                            if (this.keyValuePair.containsKey(key)) {
                                ex[0] = new ConfigurationException("activationOrModifiedRoutine",
                                        "Double configured keys:  " + key);
                            } else {
                                if (this.channelIds.contains(value)) {
                                    this.keyValuePair.put(key, new ChannelAddress(this.otherComponent.id(), value));
                                } else {
                                    ex[0] = new ConfigurationException("activationOrModifiedRoutine",
                                            "ChannelId is wrong for entry: " + entry + " ChannelId configured is: " + value);
                                }
                            }
                        } else {
                            ex[0] = new ConfigurationException("activationOrModifiedRoutine",
                                    "ConfigLength is wrong. Size is : " + entries.length + " but expected: " + CONFIG_SPLIT_LENGTH);

                        }
                    }
                }
            });
            if (ex[0] != null) {
                this.keyValuePair.clear();
                throw ex[0];
            }
            this.filePath = this.config.fileLocation();
            OpenemsError.OpenemsNamedException[] exception = {null};
            this.keyValuePair.forEach((key, value) -> {
                if (exception[0] == null) {
                    Channel<?> channel = null;

                    try {
                        channel = this.cpm.getChannel(value);

                        Value<?> channelValue = channel.value();
                        if (!channelValue.isDefined()) {
                            channelValue = channel.getNextValue();
                        }
                        this.channelIdToValueMap.put(channel.channelId().id(), channelValue.asString());

                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't find Channel: " + value);
                        this.channelIdToValueMap.clear();
                        exception[0] = e;
                    }
                }
            });
            if (exception[0] != null) {
                throw exception[0];
            }
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException, IOException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
        this.writtenToPath = false;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Update method available for Components using MQTT.
     */
    public void update() throws IOException {

        List<Channel<?>> channels =
                this.otherComponent.channels().stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        this.otherComponentChannelList = channels;
        this.otherComponentChannelList.forEach(entry -> {
            this.channelIds.add(entry.channelId().id());
        });
        if (this.config.channels().length != channels.size()) {
            this.updateConfig(this.ca.getConfiguration(this.servicePid(), "?"), channels);
        }
    }


    private void updateConfig(Configuration config, List<Channel<?>> channels) throws IOException {
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());


        Dictionary<String, Object> properties = config.getProperties();
        properties.put("channels", this.propertyInput(Arrays.toString(channelIdArray)));
        config.update(properties);
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

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.config.configurationDone()) {

            if (this.writtenToPath == false) {

                if (!this.fileExist()) {
                    this.writeNewOutputToFile(this.createOutputFromScratch());
                } else {
                    this.writeNewOutputToFile(this.createNewOutput());
                }
            } else {
                //checkForValueChange -> set written to Path to false
                this.otherComponentChannelList.forEach(entry -> {
                    if (this.channelIdToValueMap.containsKey(entry.channelId().id())) {
                        String currentChannelValue = entry.value().asString();
                        if (!entry.value().isDefined()) {
                            currentChannelValue = entry.getNextValue().asString();
                        }
                        String writtenValue = this.channelIdToValueMap.get(entry.channelId().id());
                        if (!writtenValue.equals(currentChannelValue)) {
                            this.channelIdToValueMap.replace(entry.channelId().id(), currentChannelValue);
                            this.writtenToPath = false;
                        }
                    }
                });
            }
        }
    }

    private void writeNewOutputToFile(String newOutput) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(this.filePath));
            writer.write(newOutput);
            this.writtenToPath = true;
        } catch (IOException e) {
            this.log.warn("Couldn't write into file! Reason: " + e.getCause());
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    this.log.warn("Couldn't  close or flush the writer");
                }
            }
        }
    }

    private String createNewOutput() {
        try {
            //Receive InputLine
            BufferedReader read = new BufferedReader(new FileReader(this.filePath));
            StringBuilder buffer = new StringBuilder();
            String line;
            List<String> keyValuePairNotAdded = new ArrayList<>(this.keyValuePair.keySet());
            //Check each Line
            while ((line = read.readLine()) != null) {
                //If Line could potentially be an easy line to read
                if (line.contains(LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR)) {
                    String[] splittedLines = line.split(LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR);
                    String keyValuePairKey = splittedLines[CONFIG_KEY_ENTRY_POSITION];
                    //check if the position of the key matches an existing configured key
                    if (this.keyValuePair.containsKey(keyValuePairKey)
                            && splittedLines.length == CONFIG_SPLIT_LENGTH) {
                        //since key matches -> will be checked -> remove from potentially missed keys
                        keyValuePairNotAdded.removeIf(entry -> entry.equals(keyValuePairKey));
                        String keyForAddress = this.keyValuePair.get(keyValuePairKey).getChannelId();
                        //get Value of corresponding ChannelId
                        if (this.channelIdToValueMap.containsKey(keyForAddress)) {
                            String value = this.channelIdToValueMap.get(keyForAddress);
                            if (!value.equals("")) {
                                splittedLines[CONFIG_VALUE_ENTRY_POSITION] = value;
                            }
                            //rearrange Line and append later
                            line = splittedLines[CONFIG_KEY_ENTRY_POSITION]
                                    + LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR
                                    + splittedLines[CONFIG_VALUE_ENTRY_POSITION];
                        }
                    }
                }
                buffer.append(line);
                buffer.append("\n");
            }
            read.close();
            //check for missing KeyValuePairs
            if (keyValuePairNotAdded.size() > 0) {
                keyValuePairNotAdded.forEach(entry -> {
                    String channelId = this.keyValuePair.get(entry).getChannelId();
                    String channelValue;
                    String channelValueString = "";
                    //get value of "Key" --> key stores ChannelAddress -> get the Value of the Channel
                    if (this.channelIdToValueMap.containsKey(channelId)) {
                        channelValue = this.channelIdToValueMap.get(channelId);
                        channelValueString = channelValue.trim();
                    }
                    buffer.append(entry)
                            .append(LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR)
                            .append(channelValueString)
                            .append("\n");
                });
            }
            return buffer.toString();
        } catch (IOException e) {
            this.log.warn("Couldn't read line of: " + this.id());
            return "";
        }
    }

    private String createOutputFromScratch() {
        StringBuilder builder = new StringBuilder();
        AtomicBoolean channelAddressWrong = new AtomicBoolean(false);

        this.keyValuePair.forEach((key, value) -> {

            if (!channelAddressWrong.get()) {
                String channelValueToWrite = "n.a.";
                try {
                    Channel<?> channel = this.cpm.getChannel(value);
                    Value<?> channelValue = channel.value();
                    if (!channelValue.isDefined()) {
                        channelValue = channel.getNextValue();
                    }
                    if (channelValue.isDefined()) {
                        channelValueToWrite = channelValue.get().toString();
                    }
                    if (this.channelIdToValueMap.containsKey(channel.channelId().id())) {
                        this.channelIdToValueMap.replace(channel.channelId().id(), channelValue.asString());
                    } else {
                        this.channelIdToValueMap.put(channel.channelId().id(), channelValue.asString());
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("ChannelAddress: " + value + " is not available!");
                }
                builder.append(key).append(LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR).append(channelValueToWrite).append("\n");
            }
        });
        if (channelAddressWrong.get()) {
            this.log.warn("ChannelAddress wrong! See log for reason");
            return "";
        }
        return builder.toString();
    }

    /**
     * Checks if the csvFile exists.
     *
     * @return true if file is not existing.
     */
    private boolean fileExist() {
        File f = new File(this.filePath);
        boolean fileExists = f.exists();
        if (!fileExists && !f.isDirectory()) {
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                boolean dirSuccess = f.getParentFile().mkdirs();
                this.log.info("Created Directories: " + dirSuccess);
            }
            try {
                boolean fileCreateSuccess = f.createNewFile();
                this.log.info("Created Files: " + fileCreateSuccess);
            } catch (IOException e) {
                this.log.warn("Couldn't create File! Reason: " + e.getMessage());
            }
        }
        return fileExists;
    }
}


