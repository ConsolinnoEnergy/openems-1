package io.openems.edge.heatsystem.components.pump;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.pwm.api.Pwm;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This simple Pump can be configured and used by either a Pwm or a Relay or Both.
 * It works with Channels as well. You still need to configure if the Pump is controlled by a Realy, Pwm or Both.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Pump")
public class PumpImpl extends AbstractOpenemsComponent implements OpenemsComponent, Pump {

    private Relay relay;
    private Pwm pwm;

    private ChannelAddress relayChannelAddress;
    private ChannelAddress pwmChannelAddress;
    private boolean isRelay = false;
    private boolean isPwm = false;
    private ConfigurationType configurationType;

    @Reference
    ComponentManager cpm;

    public PumpImpl() {
        super(OpenemsComponent.ChannelId.values(), HeatsystemComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.allocateComponents(config.configType(), config.pump_Type(), config.pump_Relays(), config.pump_Pwm());
        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);
    }


    /**
     * Allocates the components.
     *
     * @param configurationType The Configuration Type -> either Channel or Device
     * @param pump_type         is the pump controlled via realys, pwm or both.
     * @param pump_relays       the unique id of the relays controlling the pump.
     * @param pump_pwm          unique id of the pwm controlling the pump.
     *
     *                          <p>Depending if it's a relays/pwm/both the Components will be fetched by the component-manager and allocated.
     *                          The relays and or pump will be off (just in case).
     */
    private void allocateComponents(ConfigurationType configurationType, String pump_type, String pump_relays, String pump_pwm) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.configurationType = configurationType;
        switch (pump_type) {
            case "Relay":
                isRelay = true;
                break;
            case "Pwm":
                isPwm = true;
                break;

            case "Both":
            default:
                isRelay = true;
                isPwm = true;
                break;
        }

        if (isRelay) {
            this.configureRelay(pump_relays);
            if (cpm.getComponent(pump_relays) instanceof Relay) {
                this.relay = cpm.getComponent(pump_relays);
                this.relay.getRelaysWriteChannel().setNextWriteValue(!this.relay.isCloser().getNextValue().get());
            } else {
                throw new ConfigurationException(pump_relays, "Allocated relays not a (configured) relays.");
            }
        }
        if (isPwm) {
            this.configurePump(pump_pwm);
            if (cpm.getComponent(pump_pwm) instanceof Pwm) {
                this.pwm = cpm.getComponent(pump_pwm);
                //reset pwm to 0; so pump is on activation off
                this.pwm.getWritePwmPowerLevelChannel().setNextWriteValue(0.f);
            } else {
                throw new ConfigurationException(pump_pwm, "Allocated Pwm, not a (configured) pwm-device.");
            }
        }

    }

    private void configurePump(String pump_pwm) {
        switch(this.configurationType){
            case CHANNEL:

                break;
            case DEVICE:
                break;
        }
    }

    private void configureRelay(String pump_relays) {

    }


    /**
     * Deactivates the pump.
     * if the relays is a closer --> false is written --> open
     * if the relays is an opener --> true is written --> open
     * --> no voltage.
     */
    @Deactivate
    public void deactivate() {
        super.deactivate();
        try {
            if (this.isRelay) {
                this.relay.getRelaysWriteChannel().setNextWriteValue(!this.relay.isCloser().getNextValue().get());
            }
            if (this.isPwm) {
                this.pwm.getWritePwmPowerLevelChannel().setNextWriteValue(0.f);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean readyToChange() {
        return true;
    }

    /**
     * Changes the powervalue by percentage.
     *
     * @param percentage to adjust the current powerlevel.
     * @return successful boolean
     * <p>
     * If the Pump is only a relays --> if negative --> controlyRelays false, else true
     * If it's in addition a pwm --> check if the powerlevel - percentage <= 0
     * --> pump is idle --> relays off and pwm is 0.f %
     * Otherwise it's calculating the new Power-level and writing
     * the old power-level in the LastPowerLevel Channel
     * </p>
     */
    @Override
    public boolean changeByPercentage(double percentage) {

        if (this.isRelay) {
            if (this.isPwm) {
                if ((this.getPowerLevelChannel().getNextValue().get() + percentage <= 0)) {
                    this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelChannel().getNextValue().get());
                    this.getPowerLevelChannel().setNextValue(0);
                    try {
                        this.pwm.getWritePwmPowerLevelChannel().setNextWriteValue(0.f);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        e.printStackTrace();
                        return false;
                    }
                    controlRelays(false);
                    return true;
                } else {
                    controlRelays(true);
                }
            } else if (percentage <= 0) {
                controlRelays(false);
            } else {
                controlRelays(true);
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            currentPowerLevel = this.getPowerLevelChannel().getNextValue().get();
            this.getLastPowerLevelChannel().setNextValue(currentPowerLevel);
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100
                    : currentPowerLevel < 0 ? 0 : currentPowerLevel;

            this.getPowerLevelChannel().setNextValue(currentPowerLevel);
            try {
                this.pwm.getWritePwmPowerLevelChannel().setNextWriteValue((float) currentPowerLevel);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void controlRelays(boolean activate) {
        try {
            if (this.relay.isCloser().value().get()) {
                this.relay.getRelaysWriteChannel().setNextWriteValue(activate);
            } else {
                this.relay.getRelaysWriteChannel().setNextWriteValue(!activate);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPowerLevel(double percent) {
        if (percent >= 0) {
            double changeByPercent = percent - getPowerLevelValue();
            this.changeByPercentage(changeByPercent);
        }
    }
}
