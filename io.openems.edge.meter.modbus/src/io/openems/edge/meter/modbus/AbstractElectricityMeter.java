package io.openems.edge.meter.modbus;

import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeterModbusGeneric;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Generalized Abstract Electricity Meter, easier use of implementations of Symmetric and Asymmetric Meter.
 */

public abstract class AbstractElectricityMeter extends AbstractGenericModbusComponent implements SymmetricMeter, SymmetricMeterModbusGeneric {

    protected MeterType meterType = MeterType.GRID;

    protected AbstractElectricityMeter(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                       io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected void setMeterType(MeterType meterType) {
        this.meterType = meterType;
    }


    protected void updateSymmetricMeterChannel() {
        handleChannelUpdate(this.getFrequencyChannel(), this._hasFrequency());
        handleChannelUpdate(this.getMinActivePowerChannel(), this._hasMinActivePower());
        handleChannelUpdate(this.getMaxActivePowerChannel(), this._hasMaxActivePower());
        handleChannelUpdate(this.getActivePowerChannel(), this._hasActivePower());
        handleChannelUpdate(this.getReactivePowerChannel(), this._hasReactivePower());
        handleChannelUpdate(this.getActiveProductionEnergyChannel(), this._hasActiveProductionEnergy());
        handleChannelUpdate(this.getActiveConsumptionEnergyChannel(), this._hasActiveConsumptionEnergy());
        handleChannelUpdate(this.getVoltageChannel(), this._hasVoltage());
        handleChannelUpdate(this.getCurrentChannel(), this._hasCurrent());
    }

    @Override
    public MeterType getMeterType() {
        return this.meterType;
    }
}
