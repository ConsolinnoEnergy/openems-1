package io.openems.edge.meter.heatmeter.generic;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.HeatMeter;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.heatmeter.HeatMeterModel;
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
@Component(name = "HeatMeter.Mbus.Generic",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class HeatMeterMbusImpl extends AbstractOpenemsMbusComponent implements OpenemsComponent, HeatMeter, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMbus(BridgeMbus mbus) {
        super.setMbus(mbus);
    }

    HeatMeterModel heatMeterModel;

    private static final int NOT_DEFINED = -404;

    public HeatMeterMbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatMeter.ChannelId.values(),
                Meter.ChannelId.values(),
                ChannelId.values());
    }

    Config config;

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.getFlowRateToMbus().value().isDefined()) {
                this.getFlowRate().setNextValue(this.getFlowRateToMbus().value().get());
            }
            if (this.getPowerToMbus().value().isDefined()) {
                this.getReadingPowerChannel().setNextValue(this.getPowerToMbus().value().get());
                this.getMeterReadingChannel().setNextValue(this.getPowerToMbus().value());
            }
            if (this.getTotalConsumedEnergyToMbus().value().isDefined()) {
                this.getReadingEnergyChannel().setNextValue(this.getTotalConsumedEnergyToMbus().value().get());
            }
            if (this.getFlowTempToMbus().value().isDefined()) {
                this.getFlowTempChannel().setNextValue(this.getFlowTempToMbus().value().get());
            }
            if (this.getReturnTempToMbus().value().isDefined()) {
                this.getReturnTempChannel().setNextValue(this.getReturnTempToMbus().value().get());
            }
        }
    }

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        DEVICE_ID(Doc.of(OpenemsType.STRING) //
                .unit(Unit.NONE)), //
        READING_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
        FLOW_RATE_TO_MBUS(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR)),
        TOTAL_CONSUMED_ENERGY_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
        FLOW_TEMP_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        RETURN_TEMP_TO_MBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        FLOW_RATE(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR)),
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    Channel<Integer> getPowerToMbus() {
        return this.channel(ChannelId.READING_TO_MBUS);
    }

    Channel<Double> getFlowRateToMbus() {
        return this.channel(ChannelId.FLOW_RATE_TO_MBUS);
    }

    Channel<Integer> getTotalConsumedEnergyToMbus() {
        return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY_TO_MBUS);
    }

    Channel<Integer> getFlowTempToMbus() {
        return this.channel(ChannelId.FLOW_TEMP_TO_MBUS);
    }

    Channel<Integer> getReturnTempToMbus() {
        return this.channel(ChannelId.RETURN_TEMP_TO_MBUS);
    }

    Channel<Double> getFlowRate() {
        return this.channel(ChannelId.FLOW_RATE);
    }

    @Activate
    void activate(ComponentContext context, Config config) {

        this.config = config;

        if (config.usePollingInterval()) {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), config.pollingIntervalSeconds(), this.getErrorMessageChannel());     // If you want to use the polling interval, put the time as the last argument in super.activate().
        } else {
            super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                    config.mbusBridgeId(), 0, this.getErrorMessageChannel());  // If you don't want to use the polling interval, use super.activate() without the last argument.
        }
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected void addChannelDataRecords() {
        this.addToChannelDataRecordListIfDefined(channel(ChannelId.TOTAL_CONSUMED_ENERGY_TO_MBUS), this.config.totalConsumedEnergyAddress());
        this.addToChannelDataRecordListIfDefined(channel(ChannelId.FLOW_TEMP_TO_MBUS), this.config.flowTempAddress());
        this.addToChannelDataRecordListIfDefined(channel(ChannelId.RETURN_TEMP_TO_MBUS), this.config.returnTempAddress());
        this.addToChannelDataRecordListIfDefined(channel(ChannelId.READING_TO_MBUS), this.config.meterReading());
        this.addToChannelDataRecordListIfDefined(channel(ChannelId.FLOW_RATE_TO_MBUS), this.config.flowRateAddress());
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
        // Timestamp created by OpenEMS, not read from meter.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(Meter.ChannelId.TIMESTAMP_SECONDS), -1));
        // TimestampString is always on address -2, since it's an internal method. This channel needs to be
        // called after the TimestampSeconds Channel, as it takes it's value from that channel.
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(Meter.ChannelId.TIMESTAMP_STRING), -2));
    }

    private void addToChannelDataRecordListIfDefined(Channel<?> channel, int address) {
        if (address != NOT_DEFINED) {
            this.channelDataRecordsList.add(new ChannelRecord(channel, address));
        }
    }

    @Override
    public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
        // Not available yet for this controller.
    }

}
