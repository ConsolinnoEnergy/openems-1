package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ThermometerModbus extends Thermometer {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * <ul> Temperature.
         * <li>Interface: Thermometer Modbus
         * <li>Type: Float
         * <li>Unit: DezidegreeCelsius
         * </ul>
         */

        TEMPERATURE_MODBUS_FLOAT(Doc.of(OpenemsType.FLOAT).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * <ul> Temperature.
         * <li>Interface: Thermometer Modbus
         * <li>Type: Integer
         * <li>Unit: DezidegreeCelsius
         * </ul>
         */

        TEMPERATURE_MODBUS_INTEGER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#TEMPERATURE_MODBUS_FLOAT}.
     *
     * @return the Channel
     */
    default Channel<Float> getTemperatureModbusFloat() {
        return this.channel(ChannelId.TEMPERATURE_MODBUS_FLOAT);
    }

    /**
     * Gets the Channel for {@link ChannelId#TEMPERATURE_MODBUS_INTEGER}.
     *
     * @return the Channel
     */
    default Channel<Integer> getTemperatureModbusInteger() {
        return this.channel(ChannelId.TEMPERATURE_MODBUS_INTEGER);
    }

    /**
     * Gets the Value for {@link ChannelId#TEMPERATURE_MODBUS_FLOAT}.
     *
     * @return the value
     */
    default float getTemperatureModbusFloatValue() {
        return this.getTemperatureModbusFloat().value().orElse(this.getTemperatureModbusFloat().getNextValue().orElse(-9001.f));
    }

    /**
     * Gets the Value for {@link ChannelId#TEMPERATURE_MODBUS_INTEGER}.
     *
     * @return the value
     */
    default int getTemperatureModbusIntValue() {
        return this.getTemperatureModbusInteger().value().orElse(this.getTemperatureModbusInteger().getNextValue().orElse(-9001));
    }
}

