package io.openems.edge.utility.configSwap;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is used to swap Configurations by having one active Component and setting up configuration properties,
 * while the other non-active Components receive an inactive configuration with properties.
 * Active Components will be swapped after either a configured time is up or (optional) if an error occurred.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.Swap.Config.Entries", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class SwapConfigEntries extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin ca;

    private static final String IDENTIFIER_SWAP = "SWAP_CONFIG_ENTRY_SECONDS";
    private static final int INFINITE_COMPONENT = -1;
    private static final String CONFIG_SPLIT = ":";
    private static final int CONFIG_ENTRY_LENGTH_AFTER_SPLIT = 2;

    private List<String> componentStrings = new ArrayList<>();
    private boolean useSwapOnError;
    private String errorOccurredValue = "";
    private final List<ChannelAddress> errorAddresses = new ArrayList<>();

    private final Map<String, Object> activeValues = new HashMap<>();
    private final Map<String, Object> inactiveValues = new HashMap<>();

    //set by this component
    private DateTime dateTime;
    //set by this component
    private String activeComponent;

    private TimerHandler timerHandler;
    private int timeFrame = 0;
    private boolean swapSuccess = false;
    private boolean configSuccess = false;

    private Config config;



    private final Logger log = LoggerFactory.getLogger(SwapConfigEntries.class);

    public SwapConfigEntries() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }


    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);

    }

    /**
     * The SetUp Method for this component.
     * When called, clear up all lists and maps.
     * It is an option to swap components if an error occurred to the corresponding error channel.
     * (The config component entry position is mapped to the error-channel config entry position)
     * After that fill the active and inactive ConfigurationMap, followed by adding the active Component and the initial
     * DateTime. And set the initial DateTime.
     *
     *
     * @param config the component-config.
     */
    private void activationOrModifiedRoutine(Config config) {
        this.config = config;
        this.errorAddresses.clear();
        this.activeValues.clear();
        this.inactiveValues.clear();
        this.useSwapOnError = config.useSwapOnError();
        this.errorOccurredValue = config.errorOccurredValue();
        try {
            if (this.useSwapOnError && config.components().length != config.errorChannel().length) {
                throw new ConfigurationException("activationOrModifiedRoutine", "Components Length has to be equal to errorChannel Length!");
            }
            OpenemsError.OpenemsNamedException[] ex = {null};
            this.componentStrings = Arrays.asList(config.components());
            this.componentStrings.forEach(entry -> {
                if (ex[0] == null) {
                    try {
                        this.cpm.getComponent(entry);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        ex[0] = e;
                    }
                }
            });
            if (ex[0] == null) {
                Arrays.stream(config.errorChannel()).forEach(entry -> {
                    if (ex[0] == null) {
                        try {
                            this.errorAddresses.add(ChannelAddress.fromString(entry));
                        } catch (OpenemsError.OpenemsNamedException e) {
                            ex[0] = e;
                        }
                    }
                });
            }
            if (ex[0] != null) {
                throw ex[0];
            }
            ConfigurationException[] exC = {null};
            this.putKeyAndValuePairsToMap(this.activeValues, config.activeKeyValuePairs(), exC);
            if (exC[0] == null) {
                this.putKeyAndValuePairsToMap(this.inactiveValues, config.inactiveKeyValuePairs(), exC);
            }
            if (exC[0] != null) {
                throw exC[0];
            }

            boolean needToUpdateConfig = false;
            if (config.dateTime() == null || config.dateTime().equals("")) {
                this.dateTime = new DateTime();
                needToUpdateConfig = true;
            } else {
                this.dateTime = DateTime.parse(config.dateTime());
            }
            if (config.activeComponent() == null || config.activeComponent().equals("")) {
                this.activeComponent = this.componentStrings.get(0);
                needToUpdateConfig = true;
            } else {
                this.activeComponent = config.activeComponent();
            }
            this.timeFrame = config.swapInterval();

            if (this.timerHandler != null) {
                this.timerHandler.removeComponent();
            }
            this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
            timerHandler.addOneIdentifier(IDENTIFIER_SWAP, config.timerHandler(), config.swapInterval());

            if (needToUpdateConfig) {
                this.updateOwnConfig();
            } else {
                this.timerHandler.setInitialTime(this.dateTime, IDENTIFIER_SWAP);
            }

            this.configSuccess = true;
            this.swapSuccess = false;
        } catch (ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn(this.id() + ": Either Check ComponentsLength and ErrorChannel Length! "
                    + "Or Check Active/Passive Entries! " + e.getMessage());
        } catch (OpenemsError.OpenemsNamedException e) {
            this.configSuccess = false;
            this.log.warn(this.id() + " Couldn't find ChannelAddress! " + e.getError());
        } catch (IOException e) {
            this.configSuccess = false;
            this.log.warn("Couldn't update own Configuration!");
        }
    }

    /**
     * Updates Own config, by setting a new DateTime and a new ActiveComponent.
     * This happens either on initial set Up or after the Time is up.
     * @throws IOException if the update fails.
     */
    private void updateOwnConfig() throws IOException {
        Map<String, Object> update = new HashMap<>();
        update.put("dateTime", this.dateTime.toString());
        update.put("activeComponent", this.activeComponent);
        ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), update);
    }

    /**
     * Fills the key Value pairs for the active and inactive Configurations
     * @param valueMap the Map reference, where the key values will be put into.
     * @param keyValuePairs the Configuration of either the active or inactive configurations
     * @param exC exception Array to capture an error.
     */
    private void putKeyAndValuePairsToMap(Map<String, Object> valueMap, String[] keyValuePairs, ConfigurationException[] exC) {
        Arrays.stream(keyValuePairs).forEach(entry -> {
            if (exC[0] == null) {
                String[] entries = entry.split(CONFIG_SPLIT);
                if (entries.length == CONFIG_ENTRY_LENGTH_AFTER_SPLIT) {
                    valueMap.put(entries[0].trim(), entries[1].trim());
                } else {
                    exC[0] = new ConfigurationException("ActivationOrModified Routine", "Entry Length is not equal to : " + CONFIG_ENTRY_LENGTH_AFTER_SPLIT);
                }
            }
        });
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     *
     * The overwritten handleEvent method.
     * This component checks if the time is up to swap active components (or if an error occurred,
     * swap the active component as well).
     * When swapping the active component, always get the next entry of the {@link #componentStrings}.
     * When the end of the list is reached, start at the beginning.
     *
     * After getting a new active component, and also updating the dateTime, this component will be updated as well (update method of OSGi is called).
     * In the end, the new active Component receives an update with the {@link #activeValues}. This will be put into the Configuration.
     * Additionally, all the inactive Components receive {@link #inactiveValues}.
     *
     * @param event == the topic. Expected: {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE}.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.configSuccess) {
                if ((this.timeFrame != INFINITE_COMPONENT && this.timerHandler.checkTimeIsUp(IDENTIFIER_SWAP)) || this.errorOccurred()) {
                    this.dateTime = new DateTime();
                    int index = this.componentStrings.indexOf(activeComponent);
                    if (index >= this.componentStrings.size() - 1) {
                        index = 0;
                    } else {
                        index++;
                    }
                    this.activeComponent = this.componentStrings.get(index);
                    try {
                        this.updateOwnConfig();
                    } catch (IOException e) {
                        this.log.warn(this.id() + ": Couldn't change config, trying again!");
                    }
                }
                if (!this.swapSuccess) {
                    AtomicBoolean swapFail = new AtomicBoolean(false);
                    try {
                        ConfigurationUpdate.updateConfig(this.ca, this.cpm.getComponent(this.activeComponent).servicePid(), this.activeValues);
                    } catch (IOException | OpenemsError.OpenemsNamedException e) {
                        swapFail.set(true);
                    }
                    this.componentStrings.stream().filter(entry -> !entry.equals(this.activeComponent)).forEach(entry -> {
                        try {
                            ConfigurationUpdate.updateConfig(this.ca, this.cpm.getComponent(entry), this.inactiveValues);
                        } catch (IOException | OpenemsError.OpenemsNamedException e) {
                            swapFail.set(true);
                        }
                    });
                    this.swapSuccess = !swapFail.get();
                }

            } else {
                this.activationOrModifiedRoutine(this.config);
            }
        }
    }

    private boolean errorOccurred() {
        if (this.useSwapOnError) {
            int index = this.componentStrings.indexOf(this.activeComponent);
            Value<?> errorValue;
            try {
                errorValue = this.cpm.getChannel(this.errorAddresses.get(index)).value();
            } catch (OpenemsError.OpenemsNamedException e) {
                return true;
            }
            if (errorValue.isDefined()) {
                String compare = TypeUtils.getAsType(OpenemsType.STRING, errorValue.get());
                if (compare == null) {
                    return false;
                }
                return compare.equals(this.errorOccurredValue);
            }
        }
        return false;
    }
}
