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

    default Channel<Float> getTemperatureModbusFloat() {
        return this.channel(ChannelId.TEMPERATURE_MODBUS_FLOAT);
    }

    default Channel<Integer> getTemperatureModbusInteger() {
        return this.channel(ChannelId.TEMPERATURE_MODBUS_INTEGER);
    }


    default float getTemperatureModbusFloatValue() {
        if (this.getTemperatureModbusFloat().value().isDefined()) {
            return this.getTemperatureModbusFloat().value().get();
        } else if (this.getTemperatureModbusFloat().getNextValue().isDefined()) {
            return this.getTemperatureModbusFloat().getNextValue().get();
        } else {
            return -9001;
        }
    }

    default int getTemperatureModbusIntValue() {
        return this.getTemperatureModbusInteger().value().orElse(this.getTemperatureModbusInteger().getNextValue().orElse(-9001));
    }
}

