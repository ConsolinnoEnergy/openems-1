package io.openems.edge.meter.mbus.electricity;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "MeterMbusElectricity",
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class ElectricityMeterMbusImpl extends AbstractOpenemsMbusComponent
        implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

    private MeterType meterType = MeterType.CONSUMPTION_METERED;
    int powerAddress, energyAddress;
    ElectricityMeterModel meterModel;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMbus(BridgeMbus mbus) {
        super.setMbus(mbus);
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

    public ElectricityMeterMbusImpl() {
        super(OpenemsComponent.ChannelId.values(), //
                SymmetricMeter.ChannelId.values(), //
                AsymmetricMeter.ChannelId.values(), //
                ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        if ( config.model().equals("none") ) {
            this.powerAddress=config.powerAddress();
            this.energyAddress=config.totalConsumedEnergyAddress();
        } else {
            //Select meter model from enum list. string in config.model has to be in ElectricityMeterModel list.
            this.meterModel=ElectricityMeterModel.valueOf(config.model());
            this.powerAddress=this.meterModel.getActivePowerAddress();
            this.energyAddress=this.meterModel.getTotalConsumptionEnergy1Address();
        }
        super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
                config.mbus_id());
    }

   @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public MeterType getMeterType() {
        return this.meterType;
    }


    @Override
    protected void addChannelDataRecords() {
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(SymmetricMeter.ChannelId.ACTIVE_POWER), this.powerAddress));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(SymmetricMeter.ChannelId.POSITIVE_ACTIVE_ENERGY_TOTAL), this.energyAddress));
    /*    this.channelDataRecordsList.add(new ChannelRecord(this.channel(SymmetricMeter.ChannelId.POSITIVE_ACTIVE_ENERGY_TARIF_TWO), this.meterModel.getTotalConsumptionEnergy2Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1), this.meterModel.getActivePowerL1Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2), this.meterModel.getActivePowerL2Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), this.meterModel.getActivePowerL3Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(SymmetricMeter.ChannelId.REACTIVE_POWER), this.meterModel.getReactivePowerAddress()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1), this.meterModel.getReactivePowerL1Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2), this.meterModel.getReactivePowerL2Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3), this.meterModel.getReactivePowerL3Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.VOLTAGE_L1), this.meterModel.getVoltageL1Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.VOLTAGE_L2), this.meterModel.getVoltageL2Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.VOLTAGE_L3), this.meterModel.getVoltageL3Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CURRENT_L1), this.meterModel.getAmperageL1Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CURRENT_L2), this.meterModel.getAmperageL2Address()));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CURRENT_L3), this.meterModel.getAmperageL3Address()));
     */   // Maybe add Voltage L1-L2, etc
    }



}
