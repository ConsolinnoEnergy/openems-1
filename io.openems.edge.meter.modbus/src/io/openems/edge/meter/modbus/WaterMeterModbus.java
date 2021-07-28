package io.openems.edge.meter.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.api.MeterModbusGeneric;
import io.openems.edge.meter.api.WaterMeter;
import io.openems.edge.meter.api.WaterMeterModbusGeneric;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;


@Designate(ocd = GasMeterModbusGenericConfig.class, factory = true)
@Component(name = "Meter.Modbus.WaterMeter", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class WaterMeterModbus extends AbstractGenericModbusComponent implements OpenemsComponent, WaterMeter, Meter, MeterModbusGeneric, WaterMeterModbusGeneric, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private HeatMeterConfig config;


    public WaterMeterModbus() {
        super(OpenemsComponent.ChannelId.values(),
                WaterMeter.ChannelId.values(),
                WaterMeterModbusGeneric.ChannelId.values(),
                MeterModbusGeneric.ChannelId.values(),
                Meter.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, HeatMeterConfig config) throws ConfigurationException, OpenemsException {
        this.config = config;
        if (super.update((Configuration) config, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length) && config.configurationDone()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        }
    }

    @Modified
    void modified(ComponentContext context, HeatMeterConfig config) throws OpenemsException {
        if (super.update((Configuration) config, "channelIds", new ArrayList<>(this.channels()), config.channelIds().length) && config.configurationDone()) {
            super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        }
        this.config = config;

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            handleChannelUpdate(this.getTimestampChannel(), this._hasTimeStamp());
            handleChannelUpdate(this.getTotalConsumedWaterChannel(), this._hasReadWater());
        }
    }


}
