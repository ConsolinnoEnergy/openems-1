package io.openems.edge.utility.information;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import org.osgi.service.cm.ConfigurationAdmin;
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
import org.osgi.service.metatype.annotations.Designate;
import sun.misc.ObjectInputFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This component displays all Channels the other component has.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.Information.Component", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class InformationComponent extends AbstractOpenemsComponent implements OpenemsComponent {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    public InformationComponent() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, IOException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.displayComponent(config.otherComponentId(), config.allChannel().length);
    }

    private void displayComponent(String otherComponentId, int length) throws OpenemsError.OpenemsNamedException, IOException {
        List<Channel<?>> channels =
                this.cpm.getComponent(otherComponentId).channels().stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        List<String> channelToDisplay = new ArrayList<>();
        List<String> writeChannelId = new ArrayList<>();
        channels.sort(Comparator.comparing(a -> a.channelId().id()));
        channels.forEach(channel -> {
            String channelId = channel.channelId().id();
            channelToDisplay.add(channelId);
            if (channel instanceof WriteChannel<?>) {
                writeChannelId.add(channelId);
            }
        });
        Map<String, Object> propertyMap = new HashMap<>();
        if (!channelToDisplay.isEmpty() && channelToDisplay.size() != length) {
            propertyMap.put("allChannel", channelToDisplay);
            if (!writeChannelId.isEmpty()) {
                propertyMap.put("writeChannel", writeChannelId);
            }
            ConfigurationUpdate.updateConfig(this.cm, this.servicePid(), propertyMap);
        }

    }



    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

}
