package io.openems.edge.meter.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.HeatMeter;
import io.openems.edge.meter.api.Meter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;


@Designate(ocd = GasMeterConfig.class, factory = true)
@Component(name = "Meter.Modbus.HeatMeter", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HeatMeterModbus extends AbstractMeter implements OpenemsComponent, HeatMeter, Meter {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private HeatMeterConfig config;
    private boolean configurationDone = true;


    public HeatMeterModbus() {
        super(OpenemsComponent.ChannelId.values(),
                HeatMeter.ChannelId.values(),
                Meter.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, HeatMeterConfig config) throws ConfigurationException, OpenemsException {
        this.config = config;
        if (super.update((Configuration) config, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length) && config.configurationDone()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId(), true, this.cpm, Arrays.asList(config.configurationList()));
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * WMZ_ENERGY_AMOUNT
     * WMZ_TEMP_SOURCE
     * WMZ_TEMP_SINK
     * WMZ_POWER
     */

}
