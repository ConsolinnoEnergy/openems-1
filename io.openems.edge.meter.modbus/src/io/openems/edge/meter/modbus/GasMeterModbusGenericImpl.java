package io.openems.edge.meter.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.GasMeter;
import io.openems.edge.meter.api.GasMeterModbusGeneric;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.api.MeterModbusGeneric;
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
 * The GasMeter Generic Modbus Implementation.
 * It is a Generic Modbus Component, that can Map it's Channels to ModbusAddresses.
 * Depends on the way you configure them.
 */
@Designate(ocd = GasMeterModbusGenericConfig.class, factory = true)
@Component(name = "Meter.Modbus.Meter.Gas.Generic", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class GasMeterModbusGenericImpl extends AbstractGenericModbusComponent implements OpenemsComponent, Meter, GasMeter, GasMeterModbusGeneric, MeterModbusGeneric, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }


    public GasMeterModbusGenericImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Meter.ChannelId.values(),
                GasMeter.ChannelId.values(),
                GasMeterModbusGeneric.ChannelId.values(),
                MeterModbusGeneric.ChannelId.values());
    }

    private GasMeterModbusGenericConfig config;

    @Activate
    void activate(ComponentContext context, GasMeterModbusGenericConfig config) throws ConfigurationException, OpenemsException, IOException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), config.channelIds().length);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, GasMeterModbusGenericConfig config) throws OpenemsException, IOException, ConfigurationException {
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        this.config = config;
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE) && this.isEnabled()) {
            handleChannelUpdate(this.getTimestampChannel(), this._hasTimeStamp());
            handleChannelUpdate(this.getPowerChannel(), this._hasReadingPower());
            handleChannelUpdate(this.getTotalConsumedEnergyCubicMeterChannel(), this._hasReadEnergy());
            handleChannelUpdate(this.getReturnTemp(), this._hasReturnTemp());
            handleChannelUpdate(this.getPercolationChannel(), this._hasPercolation());
            handleChannelUpdate(this.getFlowTempChannel(), this._hasFlowTemp());
            handleChannelUpdate(this.getTotalConsumedEnergyCubicMeterChannel(), this._hasTotalConsumedEnergy());
        }
    }
}
