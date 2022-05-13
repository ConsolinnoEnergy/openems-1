package io.openems.edge.utility.virtualchannel.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.VirtualChannel;
import io.openems.edge.utility.api.VirtualChannelModbus;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A Generic ModbusChannel Component to Map Miscellaneous Modbus Channel to Virtual Channel and Vice Versa for Controlling.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "VirtualChannelModbusImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class VirtualChannelModbusImpl extends AbstractGenericModbusComponent implements OpenemsComponent, VirtualChannel,
        VirtualChannelModbus, EventHandler {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }


    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    Config config;

    public VirtualChannelModbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                VirtualChannel.ChannelId.values(),
                VirtualChannelModbus.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, IOException, OpenemsException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsException, IOException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
        this.config = config;

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && this.config.configurationDone()) {
            switch (event.getTopic()) {
                case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                    handleChannelUpdate(this.getLongChannel(), this._hasReadLong());
                    handleChannelUpdate(this.getBooleanChannel(), this._hasReadBoolean());
                    handleChannelUpdate(this.getDoubleChannel(), this._hasReadDouble());
                    handleChannelUpdate(this.getStringChannel(), this._hasReadString());
                    break;

                case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                    this.handleChannelWriteFromOriginalToModbus(this._getWriteBooleanChannel(), this.getBooleanChannel());
                    this.handleChannelWriteFromOriginalToModbus(this._getWriteLongChannel(), this.getLongChannel());
                    this.handleChannelWriteFromOriginalToModbus(this._getWriteDoubleChannel(), this.getDoubleChannel());
                    this.handleChannelWriteFromOriginalToModbus(this._getWriteStringChannel(), this.getStringChannel());
                    break;
            }
        }
    }

}

