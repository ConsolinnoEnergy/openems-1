package io.openems.edge.kbr4f96.commands;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.kbr4f96.KbrBaseModuleImpl;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sends a Command to the KBR4F96.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.kbr4f96.kbrcommands", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KbrCommands extends AbstractOpenemsComponent implements OpenemsComponent {
    private final Logger log = LoggerFactory.getLogger(KbrCommands.class);
    @Reference
    protected ComponentManager cpm;

    protected KbrCommands() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
        try {
            ChannelAddress resetDevice = ChannelAddress.fromString(config.kbr() + "/CommandResetDevice");
            ChannelAddress resetMax = ChannelAddress.fromString(config.kbr() + "/CommandResetMaxValues");
            ChannelAddress resetMin = ChannelAddress.fromString(config.kbr() + "/CommandResetMinValues");
            ChannelAddress hT = ChannelAddress.fromString(config.kbr() + "/CommandSwapToHt");
            ChannelAddress nT = ChannelAddress.fromString(config.kbr() + "/CommandSwapToNt");
            ChannelAddress error = ChannelAddress.fromString(config.kbr() + "/CommandEraseFailStatus");
            this.cpm.getChannel(resetDevice).setNextValue(config.reset());
            this.cpm.getChannel(resetMax).setNextValue(config.max());
            this.cpm.getChannel(resetMin).setNextValue(config.min());
            if (config.hT()) {
                this.cpm.getChannel(hT).setNextValue(config.newHT());
            }
            if (config.nT()) {
                this.cpm.getChannel(nT).setNextValue(config.newNT());
            }
            this.cpm.getChannel(error).setNextValue(config.error());
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Cant execute Command.");

        }
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }
}
