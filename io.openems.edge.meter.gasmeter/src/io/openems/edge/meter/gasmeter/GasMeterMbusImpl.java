package io.openems.edge.meter.gasmeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.GasMeter;
import io.openems.edge.meter.api.Meter;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "GasMeterMbus",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class GasMeterMbusImpl extends AbstractOpenemsMbusComponent implements OpenemsComponent, GasMeter {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMbus(BridgeMbus mbus) {
        super.setMbus(mbus);
    }


    private GasMeterType gasMeterType;

    public GasMeterMbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                GasMeter.ChannelId.values(),
                ChannelId.values());
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        DEVICE_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
        allocateAddressViaMeterType(config.meterType());
        super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                config.mbusBridgeId(), 0);
    }

    private void allocateAddressViaMeterType(String meterType) {
        switch (meterType) {
            case "Placeholder":
                this.gasMeterType = GasMeterType.PLACEHOLDER;
                break;

        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


    @Override
    protected void addChannelDataRecords() {
        this.channelDataRecordsList.add(new ChannelRecord(channel(GasMeter.ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER), this.gasMeterType.getTotalConsumptionEnergyAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(GasMeter.ChannelId.FLOW_TEMP), this.gasMeterType.getFlowTempAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(GasMeter.ChannelId.RETURN_TEMP), this.gasMeterType.getReturnTempAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(GasMeter.ChannelId.POWER), this.gasMeterType.getPowerAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(GasMeter.ChannelId.PERCOLATION), this.gasMeterType.getPercolationAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
        // Not needed so far.
    }
}