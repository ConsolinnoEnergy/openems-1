package io.openems.edge.heatsystem.components.valve.old.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;

/**
 * Class for testing Valve.
 */

@Designate(ocd = TestDeviceValveConfig.class, factory = true)
@Component(name = "TestDeviceForValve", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE})

public class TestDeviceForValve extends AbstractOpenemsComponent implements OpenemsComponent, TestDeviceValve, EventHandler {

    private final Logger log = LoggerFactory.getLogger(TestDeviceForValve.class);
    private boolean disturbance;
    private TestDeviceType deviceType;

    public TestDeviceForValve() {
        super(OpenemsComponent.ChannelId.values(),
                TestDeviceValve.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, TestDeviceValveConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.disturbance = config.enableDisturbance();
        this.deviceType = config.testForWhichValve();
    }

    @Modified
    void modified(ComponentContext context, TestDeviceValveConfig config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.disturbance = config.enableDisturbance();
        this.deviceType = config.testForWhichValve();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
            switch (this.deviceType) {
                case TWO_RELAYS:
                    Optional<Boolean> relayValue = this.getRelaysWriteChannel().getNextWriteValueAndReset();
                    Optional<Boolean> relay2Value = this.getRelaysWriteChannel2().getNextWriteValueAndReset();
                    boolean valveValueToSet;
                    if (relayValue.isPresent()) {
                        this.log.info("Set Relay " + super.id() + " to: " + relayValue.get());
                        if (this.disturbance) {
                            boolean hasDisturbance = new Random().nextInt(100) > 95;
                            boolean valveValue = hasDisturbance != relayValue.get();
                            this.getRelaysReadChannel().setNextValue(valveValue);
                            if (hasDisturbance) {
                                this.log.warn("Disturbance in Relay, value is now: " + valveValue);
                            }
                        } else {
                            this.getRelaysReadChannel().setNextValue(relayValue.get());
                        }
                    } else {
                        this.log.info("No WriteValue for Relay 1");
                    }

                    if (relay2Value.isPresent()) {
                        this.log.info("Set Relay 2" + super.id() + " to: " + relay2Value.get());
                        if (this.disturbance) {
                            boolean hasDisturbance = new Random().nextInt(100) > 95;
                            valveValueToSet = hasDisturbance != relay2Value.get();
                            this.getRelaysReadChannel2().setNextValue(valveValueToSet);
                            if (hasDisturbance) {
                                this.log.warn("Disturbance in Relay 2, value is now: " + valveValueToSet);
                            }
                        } else {
                            this.getRelaysReadChannel2().setNextValue(relay2Value.get());
                        }
                    } else {
                        this.log.info("No WriteValue for Relay 2");
                    }
                    break;
                case ONE_OUTPUT:
                    Optional<Integer> relayOutputValue = this.getPowerWriteChannel().getNextWriteValueAndReset();
                    if (relayOutputValue.isPresent()) {
                        if (this.disturbance) {
                            boolean hasDisturbance = new Random().nextInt(100) > 80;
                            if (hasDisturbance == false) {
                                this.getPowerReadChannel().setNextValue(relayOutputValue.get());
                            } else {
                                this.getPowerReadChannel().setNextValue(new Random().nextInt(1000));
                            }
                        }
                    }

            }
        }
    }


}
