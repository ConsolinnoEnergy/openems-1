package io.openems.edge.meter.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeterModbusGeneric;
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

/**
 * The SymmetricMeter Generic Modbus Implementation.
 * It is a Generic Modbus Component, that can Map it's Channels to ModbusAddresses.
 * Depends on the way you configure them.
 */
@Designate(ocd = SymmetricMeterConfig.class, factory = true)
@Component(name = "Meter.Modbus.Meter.Electricity.Symmetric.Generic", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class ElectricityMeterSymmetricModbusGenericImpl extends AbstractElectricityMeter implements OpenemsComponent, SymmetricMeter,
        SymmetricMeterModbusGeneric, EventHandler {

    private SymmetricMeterConfig config;

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    public ElectricityMeterSymmetricModbusGenericImpl() {
        super(OpenemsComponent.ChannelId.values(),
                SymmetricMeter.ChannelId.values(),
                SymmetricMeterModbusGeneric.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, SymmetricMeterConfig config) throws ConfigurationException, OpenemsException, IOException {
        this.config = config;
        super.setMeterType(config.meterType());
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), config.channelIds().length);
    }

    @Modified
    void modified(ComponentContext context, SymmetricMeterConfig config) throws ConfigurationException, IOException, OpenemsException {
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
        super.setMeterType(config.meterType());
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE) && this.isEnabled()) {
            super.updateSymmetricMeterChannel();
        }
    }

}
