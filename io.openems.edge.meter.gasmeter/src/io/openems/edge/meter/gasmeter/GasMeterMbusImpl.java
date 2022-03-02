package io.openems.edge.meter.gasmeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "GasMeterMbus",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class GasMeterMbusImpl extends AbstractOpenemsMbusComponent implements OpenemsComponent, GasMeter, EventHandler {

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
                Meter.ChannelId.values(),
                ChannelId.values());
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        DEVICE_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //

        CUBIC_METER_TO_MBUS(Doc.of(OpenemsType.DOUBLE)),
        TIME_STAMP_STRING_TO_MBUS(Doc.of(OpenemsType.STRING))
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    Channel<Double> getCubicMeterToMbus(){
        return this.channel(ChannelId.CUBIC_METER_TO_MBUS);
    }
    Channel<String> getTimeToMBus(){
        return this.channel(ChannelId.TIME_STAMP_STRING_TO_MBUS);
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
      this.gasMeterType = config.meterType();
        super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                config.mbusBridgeId(), 0);
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


    @Override
    protected void addChannelDataRecords() {
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.CUBIC_METER_TO_MBUS), this.gasMeterType.getTotalConsumptionEnergyAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.TIME_STAMP_STRING_TO_MBUS), this.gasMeterType.getTime()));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));

    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
        // Not needed so far.
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if(this.getCubicMeterToMbus().value().isDefined()){
                this.getTotalConsumedEnergyCubicMeterChannel().setNextValue(this.getCubicMeterToMbus().value());
            }
            if(this.getTimeToMBus().value().isDefined()){
                this.getTimestampStringChannel().setNextValue(this.getTimeToMBus().value());
            }
        }
    }
}