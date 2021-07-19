package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface GasMeter extends Meter {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        PERCOLATION(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBICMETER_PER_SECOND)),
        TOTAL_CONSUMED_ENERGY_CUBIC_METER(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBIC_METER)),
        FLOW_TEMP(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
        RETURN_TEMP(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Gets the Percolation of the GasMeter.
     *
     * @return the Channel.
     */
    default Channel<Integer> getPercolationChannel() {
        return this.channel(ChannelId.PERCOLATION);
    }

    /**
     * Gets the Total Consumed Energy.
     *
     * @return the Channel
     */
    default Channel<Integer> getTotalConsumedEnergyCubicMeterChannel() {
        return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER);
    }

    /**
     * Gets the Flow Temperature Channel.
     *
     * @return the Channel
     */
    default Channel<Float> getFlowTempChannel() {
        return this.channel(ChannelId.FLOW_TEMP);
    }

    /**
     * Gets the return Temperature Channel.
     *
     * @return the Channel.
     */
    default Channel<Float> getReturnTemp() {
        return this.channel(ChannelId.RETURN_TEMP);
    }
}