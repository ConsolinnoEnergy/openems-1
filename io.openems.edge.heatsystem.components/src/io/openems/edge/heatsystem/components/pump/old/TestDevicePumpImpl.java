package io.openems.edge.heatsystem.components.pump.old;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.consolinno.pwm.api.Pwm;
import io.openems.edge.pwm.device.api.PwmPowerLevelChannel;
import io.openems.edge.relays.device.api.ActuatorRelaysChannel;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


/**
 *
 */

@Designate(ocd = TestDevicePumpConfig.class, factory = true)
@Component(name = "Test.Device.Pump", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class TestDevicePumpImpl extends AbstractOpenemsComponent implements OpenemsComponent, ActuatorRelaysChannel, PwmPowerLevelChannel, EventHandler {

    private Logger log = LoggerFactory.getLogger(TestDevicePumpImpl.class);
    PumpType pumpType;
    boolean useDisturbance;

    public TestDevicePumpImpl() {
        super(ChannelId.values(),
                ActuatorRelaysChannel.ChannelId.values(),
                PwmPowerLevelChannel.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, TestDevicePumpConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.pumpType = config.pumpType();
        this.useDisturbance = config.useDisturbance();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            switch (this.pumpType) {
                case RELAY:
                    this.handleRelay();
                    break;
                case PWM:
                    this.handlePwm();
                    break;
                case RELAY_AND_PWM:
                    this.handleRelay();
                    this.handlePwm();
                    break;
            }
        }
    }

    private void handleRelay() {
        Boolean value = this.getRelaysWriteChannel().getNextWriteValueAndReset().orElse(null);
        if (value != null) {
            if (this.disturbanceOccurred(50)) {
                this.getRelaysReadChannel().setNextValue(!value);
            } else {
                this.getRelaysReadChannel().setNextValue(value);
            }
        }
    }

    private boolean disturbanceOccurred(int probability) {
        if (this.useDisturbance) {
            return new Random().nextInt(100) < probability;
        } else {
            return false;
        }
    }

    private void handlePwm() {
        Float valueOfPowerValue = this.getPwmPowerLevelChannel().getNextWriteValueAndReset().orElse(null);
        boolean disturbance = this.disturbanceOccurred(30);
        if (valueOfPowerValue != null) {
                this.getPwmPowerLevelChannel().setNextValue(disturbance ? 100 - valueOfPowerValue : valueOfPowerValue);
        }

    }
}
