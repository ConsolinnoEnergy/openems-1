package io.openems.edge.meter.virtual.asymmetric.subtract;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This VirtualMeter exists, so that eg. two real meters can have their delta can be used as a new meter.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "VirtualAsymmetricSubtractMeter", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class VirtualAsymmetricSubtractMeter extends AbstractOpenemsComponent
        implements VirtualMeter, AsymmetricMeter, SymmetricMeter, OpenemsComponent, ModbusSlave {

    @Reference
    protected ComponentManager cpm;
    private Config config;

   // @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private OpenemsComponent minuend;

    //@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    private List<OpenemsComponent> subtrahends = new ArrayList<>();

    private final AsymmetricChannelManager channelManager = new AsymmetricChannelManager(this);

    private final Logger log = LoggerFactory.getLogger(VirtualAsymmetricSubtractMeter.class);

    public VirtualAsymmetricSubtractMeter() {
        super(AsymmetricMeter.ChannelId.values(),
                SymmetricMeter.ChannelId.values(),
                OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {

        this.config = config;
        try {
            this.minuend = this.cpm.getComponent(config.minuend_id());
            Arrays.stream(config.subtrahends_ids()).forEach(id -> {
                try {
                    AsymmetricMeter put = this.cpm.getComponent(id);
                    if (!this.subtrahends.contains(put)) {
                        this.subtrahends.add(put);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to find subtrahend with id " + id + "! Check config!");
                }
            });
            this.channelManager.activate(this.minuend, this.subtrahends);
            super.activate(context, config.id(), config.alias(), config.enabled());
        } catch (Exception e) {
            this.log.error("Unable to find minuend with id " + config.minuend_id() + "! Check config!");
        }

    }

    @Deactivate
    protected void deactivate() {
        this.channelManager.deactivate();
        super.deactivate();
    }

    @Override
    public boolean addToSum() {
        return this.config.addToSum();
    }

    @Override
    public MeterType getMeterType() {
        return this.config.type();
    }

    @Override
    public String debugLog() {
        return "L:" + this.getActivePower().asString();
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return new ModbusSlaveTable( //
                OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
                AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
        );
    }
}
