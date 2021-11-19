package io.openems.edge.controller.filewriter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String CONFIG_SPLIT = ":";
    private static final int CONFIG_SPLIT_LENGTH = 2;
    private static final String LEAFLET_INVENTORY_FILE_LIST_KEY_VALUE_CONNECTOR = "=";
    private static final int CONFIG_KEY_ENTRY_POSITION = 0;
    private static final int CONFIG_VALUE_ENTRY_POSITION = 1;
    private OpenemsComponent otherComponent;
    List<Channel<?>> otherComponentChannelList = new ArrayList<>();
    List<String> channelIds = new ArrayList<>();
    private String filePath;
    private boolean fileExists;
    private boolean writtenToPath = false;
    private boolean configSuccess = false;
    private FileWriter fileWriter;
    private FileReader fileReader;
    String oldContent = "";
    String newContent = "";
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
            this.fileExists = this.fileExist();
            this.initFileWriter();
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
                if (!this.fileExists) {
                    this.createFileFromScratch();
                } else {
                    this.createNewOutput();
                    this.writeNewOutputToFile();
                }

                //look through file if key exists (for each line)
                // if yes -> get substring until = and change value
                // if no -> append at the end.
            } else {
                //checkForValueChange -> set written to Path to false
            }
        }
    }

    /**
     * Cehcks if the csvFile exists.
     *
     * @return true if file is not existing.
     */
    private boolean fileExist() {
        File f = new File(this.filePath);
        return f.exists();
    }


    /**
     * Initialize a new FileWriter for a CSV File.
     *
     * @throws IOException if something is happening.
     */
    public void initFileWriter() throws IOException {

        this.fileWriter = new FileWriter(this.filePath);
        this.fileReader = new FileReader(this.filePath);
    }

}


