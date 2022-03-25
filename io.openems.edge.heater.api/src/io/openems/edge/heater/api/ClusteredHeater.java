package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

/**
 * A HeaterCluster. Other Classes and Controller can set a recommended HeatingPower.
 * Usually a Recommended HeatingPower is preferred, however Overwrite Heating Power can have a higher priority,
 * when it is set constantly and higher than the Recommended Heating Power.
 * As Long as the ClusteredHeater is Heating / EnableSignal is set, the Inherited Heater stays active.
 */
public interface ClusteredHeater extends Heater {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        CLUSTER_STATE(Doc.of(ClusterState.values()).accessMode(AccessMode.READ_ONLY)),
        OVERWRITE_SET_POINT_HEATING_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        RECOMMENDED_SET_POINT_HEATING_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the {@link ChannelId#OVERWRITE_SET_POINT_HEATING_POWER} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Float> getOverWriteSetPointHeatingPowerChannel() {
        return this.channel(ChannelId.OVERWRITE_SET_POINT_HEATING_POWER);
    }

    /**
     * Get the {@link ChannelId#RECOMMENDED_SET_POINT_HEATING_POWER} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Float> getRecommendedSetPointHeatingPower() {
        return this.channel(ChannelId.RECOMMENDED_SET_POINT_HEATING_POWER);
    }

    /**
     * Get the {@link ChannelId#CLUSTER_STATE} channel.
     *
     * @return the channel.
     */
    default Channel<Integer> getClusterStateChannel() {
        return this.channel(ChannelId.CLUSTER_STATE);
    }

}

