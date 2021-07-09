package io.openems.edge.timer.api.test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timer.api.Timer;
import io.openems.edge.timer.api.TimerType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the Functionality of the Timer.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "TimerTest", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class TimerTest extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(TimerTest.class);
    private String timer;
    @Reference
    ComponentManager cpm;

    private final Map<String, Timer> identifierToTimerMap = new HashMap<>();


    public TimerTest() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.timer = config.timer();
        this.setupTimerAndIdentifier(config);
    }

    /**
     * Get the timer, and add identifier to the Timer.
     * Watch the logger and see if the Time is up for the configured identifier.
     *
     * @param config the config of this component.
     * @throws OpenemsError.OpenemsNamedException  if timer couldn't be found.
     */
    private void setupTimerAndIdentifier(Config config) throws OpenemsError.OpenemsNamedException {
        Timer timer = this.cpm.getComponent(config.timer());
        List<String> configEntries = Arrays.asList(config.identifier());
        configEntries.forEach(entry -> {
            String[] entries = entry.split(":");
            String identifier = entries[0];
            String value = entries[1];
            this.identifierToTimerMap.put(identifier, timer);
            timer.addIdentifierToTimer(super.id(), identifier, Integer.parseInt(value));
        });
    }


    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (this.timer.equals(config.timer()) == false) {
            this.setupTimerAndIdentifier(config);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.identifierToTimerMap.forEach((key, value) -> {
                if (value.checkIsTimeUp(super.id(), key)) {
                    this.log.info("TimeIsUp for : " + key);
                    value.reset(super.id(), key);
                    this.log.info("Reset: " + key);
                }
            });
        }
    }
}
