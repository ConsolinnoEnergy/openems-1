package io.openems.edge.weatherstation.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface WeatherStation extends OpenemsComponent {
     int OUTDOOR_TO_TEMPERATURE_SCALE = 10;
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Current outdoor temp
         * <ul>
         *      <li> Type: kwh
         * </ul>
         */
        CURRENT_OUTDOOR_TEMP(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }
    /**
     * Gets the Channel for {@link ChannelId#CURRENT_OUTDOOR_TEMP}.
     *
     * @return the Channel
     */
    default Channel<Float> getOutdoorTempChannel() {
        return this.channel(ChannelId.CURRENT_OUTDOOR_TEMP);
    }
}
