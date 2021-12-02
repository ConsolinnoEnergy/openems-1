package io.openems.edge.meter.virtual.asymmetric.subtract;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
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
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class VirtualAsymmetricSubtractMeter extends AbstractOpenemsComponent
        implements VirtualMeter, AsymmetricMeter, SymmetricMeter, OpenemsComponent, ModbusSlave, EventHandler {

    @Reference
    protected ComponentManager cpm;

    private Config config;

    private OpenemsComponent minuend;
    private final List<OpenemsComponent> subtrahends = new ArrayList<>();

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
        } catch (Exception e) {
            this.log.error("Unable to find minuend with id " + config.minuend_id() + "! Check config!");
        }
        super.activate(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public boolean addToSum() {
        return false;
    }

    @Override
    public MeterType getMeterType() {
        return MeterType.PRODUCTION;
    }

    @Override
    public String debugLog() {
        return "L:" + this.getActivePower().orElse(0) * -1;
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return new ModbusSlaveTable( //
                OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
                AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
        );
    }


    @Override
    public void handleEvent(Event event) {
        if (this.minuend != null && this.subtrahends != null) {
            this.channelManager.handle(this.minuend, this.subtrahends);
        } else {
            try {
                this.minuend = this.cpm.getComponent(this.config.minuend_id());
                Arrays.stream(this.config.subtrahends_ids()).forEach(id -> {
                    try {
                        AsymmetricMeter put = this.cpm.getComponent(id);
                        if (this.subtrahends != null && !this.subtrahends.contains(put)) {
                            this.subtrahends.add(put);
                        }
                    } catch (Exception e) {
                        this.log.error("Unable to find subtrahend with id " + id + "! Check config!");
                    }
                });
            } catch (Exception e) {
                this.log.error("Unable to find minuend with id " + this.config.minuend_id() + "! Check config!");
            }
        }
    }
}
