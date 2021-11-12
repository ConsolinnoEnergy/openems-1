package io.openems.edge.heater.chp.viessmann.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Chp;

/**
 * Channels for the Viessmann chp.
 */
public interface ChpViessmann extends Chp {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Module mode.
         * 0 Off
         * 1 Hand
         * 2 Auto
         * <ul>
         * <li>Type: Integer
         * </ul>
         */
        MODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * ModuleStatus.
         * 0 Off
         * 1 Ready
         * 2 Start
         * 3 Running
         * 4 Disturbance
         * <ul>
         * <li>Type: Integer
         * </ul>
         */
        STATUS(Doc.of(ModuleStatus.values())),

        /**
         * Operating mode type.
         * 0 Off
         * 1 Hand
         * 2 Grid substitute
         * 3 --
         * 4 100%
         * 5 Between 0-100%
         * <ul>
         * <li>Type: Integer
         * </ul>
         */
        OPERATING_MODE(Doc.of(OpenemsType.INTEGER)),

        /*
         * SetPoint Operation Mode.
         * Format: n (Int 16)
         * Signed Int
         */
        //SET_POINT_OPERATION_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), -> Heater EFFECTIVE_HEATING_POWER_PERCENT

        /**
         * ErrorBits. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
         */
        ERROR_BITS_1(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_2(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_3(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_4(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_5(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_6(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_7(Doc.of(OpenemsType.INTEGER)),
        ERROR_BITS_8(Doc.of(OpenemsType.INTEGER)),

        /**
         * Operating time of the chp.
         * <ul>
         * <li>Type: integer
         * <li>Unit: hours
         * </ul>
         */
        OPERATING_HOURS(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating time of the chp in minutes.
         * <ul>
         * <li>Type: integer
         * <li>Unit: minutes
         * </ul>
         */
        OPERATING_MINUTES(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE)),

        /**
         * How often was the chp started.
         * <ul>
         * <li>Type: integer
         * </ul>
         */
        START_COUNTER(Doc.of(OpenemsType.INTEGER)),

        /**
         * Interval of maintenance in hours. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: hours
         * </ul>
         */
        MAINTENANCE_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Module lock in hours. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: hours
         * </ul>
         */
        MODULE_LOCK(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Time when a warning should appear.
         * <ul>
         * <li>Type: integer
         * <li>Unit: hours
         * </ul>
         */
        WARNING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Time until the next maintenance should happen.
         * <ul>
         * <li>Type: integer
         * <li>Unit: hours
         * </ul>
         */
        NEXT_MAINTENANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Exhaust temperatures. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: decimal degree Celsius
         * </ul>
         */
        EXHAUST_A(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),   // Todo: check if unit is correct (degree vs decidegree).
        EXHAUST_B(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        EXHAUST_C(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        EXHAUST_D(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Temperature sensor values. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: decimal degree Celsius
         * </ul>
         */
        PT_100_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),    // Todo: check if unit is correct (degree vs decidegree).
        //PT_100_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)), -> Heater RETURN_TEMPERATURE
        //PT_100_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)), -> Heater FLOW_TEMPERATURE
        PT_100_4(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        PT_100_5(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),
        PT_100_6(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Voltage of the battery.
         * <ul>
         * <li>Type: integer
         * <li>Unit: deci Volt
         * </ul>
         */
        BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),

        /**
         * Oil pressure.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Bar
         * </ul>
         */
        OIL_PRESSURE(Doc.of(OpenemsType.INTEGER).unit(Unit.BAR)),

        /**
         * Value of comparison between Oxygen left in the Exhaust with Oxygen of it's Reference.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Volt * 10^-4
         * </ul>
         */
        LAMBDA_PROBE_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.TEN_THOUSANDTH_VOLT)),

        /**
         * Engine speed in rotations per minute.
         * <ul>
         * <li>Type: integer
         * <li>Unit: 1/min
         * </ul>
         */
        ROTATION_PER_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.ROTATION_PER_MINUTE)),

        /**
         * Temperature Controller. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: decimal degree Celsius
         * </ul>
         */
        TEMPERATURE_CONTROLLER(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),  // Todo: check if unit is correct (degree vs decidegree).

        /**
         * Temperature clearance. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: decimal degree Celsius
         * </ul>
         */
        TEMPERATURE_CLEARANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),   // Todo: check if unit is correct (degree vs decidegree).

        /**
         * Supply voltages L1-L3. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Volt
         * </ul>
         */
        SUPPLY_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        SUPPLY_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        SUPPLY_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

        /**
         * Generator voltages L1-L3. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Volt
         * </ul>
         */
        GENERATOR_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        GENERATOR_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
        GENERATOR_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

        /**
         * Generator current L1-L3. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Ampere
         * </ul>
         */
        GENERATOR_CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),
        GENERATOR_CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),
        GENERATOR_CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

        /**
         * Supply voltage total. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Volt
         * </ul>
         */
        SUPPLY_VOLTAGE_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

        /**
         * Generator voltage total. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Volt
         * </ul>
         */
        GENERATOR_VOLTAGE_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

        /**
         * Generator current total. Signed Int.
         * <ul>
         * <li>Type: integer
         * <li>Unit: Ampere
         * </ul>
         */
        GENERATOR_CURRENT_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

        /**
         * Supply AC frequency.
         * <ul>
         * <li>Type: float
         * <li>Unit: Hertz
         * </ul>
         */
        SUPPLY_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),

        /**
         * Generator AC frequency.
         * <ul>
         * <li>Type: float
         * <li>Unit: Hertz
         * </ul>
         */
        GENERATOR_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),

        /**
         * CosPhi of chp.
         * <ul>
         * <li>Type: integer
         * <li>Unit: milli degree
         * </ul>
         */
        ACTIVE_POWER_FACTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLI_DEGREE)),

        /**
         * Reserve.
         * <ul>
         * <li>Type: integer
         * <li>Unit: kilo Watt hours
         * </ul>
         */
        RESERVE(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#MODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getModuleModeChannel() {
        return this.channel(ChannelId.MODE);
    }

    /**
     * Get the module mode.
     * 0 Off
     * 1 Hand
     * 2 Auto
     * See {@link ChannelId#MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getModuleMode() {
        return this.getModuleModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS}.
     *
     * @return the Channel
     */
    default EnumReadChannel getModuleStatusChannel() {
        return this.channel(ChannelId.STATUS);
    }

    /**
     * Get the module status.
     * 0 Off
     * 1 Ready
     * 2 Start
     * 3 Running
     * 4 Disturbance
     * See {@link ChannelId#STATUS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getModuleStatus() {
        return this.getModuleStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#OPERATING_MODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOperatingModeChannel() {
        return this.channel(ChannelId.OPERATING_MODE);
    }

    /**
     * Get the operating mode type.
     * 0 Off
     * 1 Hand
     * 2 Grid substitute
     * 3 --
     * 4 100%
     * 5 Between 0-100%
     * See {@link ChannelId#OPERATING_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError1Channel() {
        return this.channel(ChannelId.ERROR_BITS_1);
    }

    /**
     * Get error bit 1. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError1() {
        return this.getError1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError2Channel() {
        return this.channel(ChannelId.ERROR_BITS_2);
    }

    /**
     * Get error bit 2. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError2() {
        return this.getError2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_3}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError3Channel() {
        return this.channel(ChannelId.ERROR_BITS_3);
    }

    /**
     * Get error bit 3. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_3}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError3() {
        return this.getError3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_4}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError4Channel() {
        return this.channel(ChannelId.ERROR_BITS_4);
    }

    /**
     * Get error bit 4. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_4}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError4() {
        return this.getError4Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_5}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError5Channel() {
        return this.channel(ChannelId.ERROR_BITS_5);
    }

    /**
     * Get error bit 5. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_5}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError5() {
        return this.getError5Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_6}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError6Channel() {
        return this.channel(ChannelId.ERROR_BITS_6);
    }

    /**
     * Get error bit 6. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_6}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError6() {
        return this.getError6Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_7}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError7Channel() {
        return this.channel(ChannelId.ERROR_BITS_7);
    }

    /**
     * Get error bit 7. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_7}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError7() {
        return this.getError7Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_BITS_8}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getError8Channel() {
        return this.channel(ChannelId.ERROR_BITS_8);
    }

    /**
     * Get error bit 8. Length 2 Byte --> Each bit acts as a flag --> Vitobloc Gateway
     * See {@link ChannelId#ERROR_BITS_8}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getError8() {
        return this.getError8Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#OPERATING_HOURS}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOperatingHoursChannel() {
        return this.channel(ChannelId.OPERATING_HOURS);
    }

    /**
     * Get the operating time of the chp in hours.
     * See {@link ChannelId#OPERATING_HOURS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingHours() {
        return this.getOperatingHoursChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#OPERATING_MINUTES}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOperatingMinutesChannel() {
        return this.channel(ChannelId.OPERATING_MINUTES);
    }

    /**
     * Get the operating time of the chp in minutes.
     * See {@link ChannelId#OPERATING_MINUTES}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingMinutes() {
        return this.getOperatingMinutesChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#START_COUNTER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStartCounterChannel() {
        return this.channel(ChannelId.START_COUNTER);
    }

    /**
     * How often was the chp started.
     * See {@link ChannelId#START_COUNTER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStartCounter() {
        return this.getStartCounterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#MAINTENANCE_INTERVAL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMaintenanceIntervalChannel() {
        return this.channel(ChannelId.MAINTENANCE_INTERVAL);
    }

    /**
     * Get the interval of maintenance in hours.
     * See {@link ChannelId#MAINTENANCE_INTERVAL}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMaintenanceInterval() {
        return this.getMaintenanceIntervalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#MODULE_LOCK}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getModuleLockChannel() {
        return this.channel(ChannelId.MODULE_LOCK);
    }

    /**
     * Get the module lock in hours.
     * See {@link ChannelId#MODULE_LOCK}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getModuleLock() {
        return this.getModuleLockChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#WARNING_TIME}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getWarningTimeChannel() {
        return this.channel(ChannelId.WARNING_TIME);
    }

    /**
     * Get the time when a warning should appear.
     * See {@link ChannelId#WARNING_TIME}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getWarningTime() {
        return this.getWarningTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#NEXT_MAINTENANCE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getNextMaintenanceChannel() {
        return this.channel(ChannelId.NEXT_MAINTENANCE);
    }

    /**
     * Get the time until the next maintenance should happen, in hours.
     * See {@link ChannelId#NEXT_MAINTENANCE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getNextMaintenance() {
        return this.getNextMaintenanceChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#EXHAUST_A}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureExhaustAChannel() {
        return this.channel(ChannelId.EXHAUST_A);
    }

    /**
     * Get the temperature of exhaust A, in decimal degree Celsius.
     * See {@link ChannelId#EXHAUST_A}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureExhaustA() {
        return this.getTemperatureExhaustAChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#EXHAUST_B}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureExhaustBChannel() {
        return this.channel(ChannelId.EXHAUST_B);
    }

    /**
     * Get the temperature of exhaust B, in decimal degree Celsius.
     * See {@link ChannelId#EXHAUST_B}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureExhaustB() {
        return this.getTemperatureExhaustBChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#EXHAUST_C}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureExhaustCChannel() {
        return this.channel(ChannelId.EXHAUST_C);
    }

    /**
     * Get the temperature of exhaust C, in decimal degree Celsius.
     * See {@link ChannelId#EXHAUST_C}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureExhaustC() {
        return this.getTemperatureExhaustCChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#EXHAUST_D}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureExhaustDChannel() {
        return this.channel(ChannelId.EXHAUST_D);
    }

    /**
     * Get the temperature of exhaust D, in decimal degree Celsius.
     * See {@link ChannelId#EXHAUST_D}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureExhaustD() {
        return this.getTemperatureExhaustDChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PT_100_1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperaturePt100_1Channel() {
        return this.channel(ChannelId.PT_100_1);
    }

    /**
     * Get the temperature of sensor Pt100_1, in decimal degree Celsius.
     * See {@link ChannelId#PT_100_1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperaturePt100_1() {
        return this.getTemperaturePt100_1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PT_100_4}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperaturePt100_4Channel() {
        return this.channel(ChannelId.PT_100_4);
    }

    /**
     * Get the temperature of sensor Pt100_4, in decimal degree Celsius.
     * See {@link ChannelId#PT_100_4}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperaturePt100_4() {
        return this.getTemperaturePt100_4Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PT_100_5}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperaturePt100_5Channel() {
        return this.channel(ChannelId.PT_100_5);
    }

    /**
     * Get the temperature of sensor Pt100_5, in decimal degree Celsius.
     * See {@link ChannelId#PT_100_5}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperaturePt100_5() {
        return this.getTemperaturePt100_5Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#PT_100_6}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperaturePt100_6Channel() {
        return this.channel(ChannelId.PT_100_6);
    }

    /**
     * Get the temperature of sensor Pt100_6, in decimal degree Celsius.
     * See {@link ChannelId#PT_100_6}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperaturePt100_6() {
        return this.getTemperaturePt100_6Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#BATTERY_VOLTAGE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getBatteryVoltageChannel() {
        return this.channel(ChannelId.BATTERY_VOLTAGE);
    }

    /**
     * Get the voltage of the battery. Unit is milli Volt.
     * See {@link ChannelId#BATTERY_VOLTAGE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getBatteryVoltage() {
        return this.getBatteryVoltageChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#OIL_PRESSURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOilPressureChannel() {
        return this.channel(ChannelId.OIL_PRESSURE);
    }

    /**
     * Get the oil pressure. Unit is Bar.
     * See {@link ChannelId#OIL_PRESSURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOilPressure() {
        return this.getOilPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#LAMBDA_PROBE_VOLTAGE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getLambdaProbeVoltageChannel() {
        return this.channel(ChannelId.LAMBDA_PROBE_VOLTAGE);
    }

    /**
     * Get the value of comparison between oxygen left in the exhaust with oxygen of it's reference.
     * Unit is Volt * 10^-4.
     * See {@link ChannelId#LAMBDA_PROBE_VOLTAGE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getLambdaProbeVoltage() {
        return this.getLambdaProbeVoltageChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ROTATION_PER_MIN}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getRotationPerMinuteChannel() {
        return this.channel(ChannelId.ROTATION_PER_MIN);
    }

    /**
     * Get the engine speed in rotation per minute.
     * See {@link ChannelId#ROTATION_PER_MIN}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getRotationPerMinute() {
        return this.getRotationPerMinuteChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TEMPERATURE_CONTROLLER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureControllerChannel() {
        return this.channel(ChannelId.TEMPERATURE_CONTROLLER);
    }

    /**
     * Get the temperature measured by the temperature controller in decimal degree Celsius.
     * See {@link ChannelId#TEMPERATURE_CONTROLLER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureController() {
        return this.getTemperatureControllerChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TEMPERATURE_CLEARANCE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureClearanceChannel() {
        return this.channel(ChannelId.TEMPERATURE_CLEARANCE);
    }

    /**
     * Get the temperature clearance in decimal degree Celsius.
     * See {@link ChannelId#TEMPERATURE_CLEARANCE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureClearance() {
        return this.getTemperatureClearanceChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SUPPLY_VOLTAGE_L1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSupplyVoltageL1Channel() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L1);
    }

    /**
     * Get the supply voltage L1 in Volt.
     * See {@link ChannelId#SUPPLY_VOLTAGE_L1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSupplyVoltageL1() {
        return this.getSupplyVoltageL1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SUPPLY_VOLTAGE_L2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSupplyVoltageL2Channel() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L2);
    }

    /**
     * Get the supply voltage L2 in Volt.
     * See {@link ChannelId#SUPPLY_VOLTAGE_L2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSupplyVoltageL2() {
        return this.getSupplyVoltageL2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SUPPLY_VOLTAGE_L3}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSupplyVoltageL3Channel() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_L3);
    }

    /**
     * Get the supply voltage L3 in Volt.
     * See {@link ChannelId#SUPPLY_VOLTAGE_L3}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSupplyVoltageL3() {
        return this.getSupplyVoltageL3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_CURRENT_L1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getGeneratorCurrentL1Channel() {
        return this.channel(ChannelId.GENERATOR_CURRENT_L1);
    }

    /**
     * Get the generator current L1 in Ampere.
     * See {@link ChannelId#GENERATOR_CURRENT_L1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getGeneratorCurrentL1() {
        return this.getGeneratorCurrentL1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_CURRENT_L2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getGeneratorCurrentL2Channel() {
        return this.channel(ChannelId.GENERATOR_CURRENT_L2);
    }

    /**
     * Get the generator current L2 in Ampere.
     * See {@link ChannelId#GENERATOR_CURRENT_L2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getGeneratorCurrentL2() {
        return this.getGeneratorCurrentL2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_CURRENT_L3}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getGeneratorCurrentL3Channel() {
        return this.channel(ChannelId.GENERATOR_CURRENT_L3);
    }

    /**
     * Get the generator current L3 in Ampere.
     * See {@link ChannelId#GENERATOR_CURRENT_L3}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getGeneratorCurrentL3() {
        return this.getGeneratorCurrentL3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SUPPLY_VOLTAGE_TOTAL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSupplyVoltageTotalChannel() {
        return this.channel(ChannelId.SUPPLY_VOLTAGE_TOTAL);
    }

    /**
     * Get the supply voltage total in Volt.
     * See {@link ChannelId#SUPPLY_VOLTAGE_TOTAL}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSupplyVoltageTotal() {
        return this.getSupplyVoltageTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_VOLTAGE_TOTAL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getGeneratorVoltageTotalChannel() {
        return this.channel(ChannelId.GENERATOR_VOLTAGE_TOTAL);
    }

    /**
     * Get the generator voltage total in Volt.
     * See {@link ChannelId#GENERATOR_VOLTAGE_TOTAL}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getGeneratorVoltageTotal() {
        return this.getGeneratorVoltageTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_CURRENT_TOTAL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getGeneratorCurrentTotalChannel() {
        return this.channel(ChannelId.GENERATOR_CURRENT_TOTAL);
    }

    /**
     * Get the generator current total in Ampere.
     * See {@link ChannelId#GENERATOR_CURRENT_TOTAL}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getGeneratorCurrentTotal() {
        return this.getGeneratorCurrentTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#SUPPLY_FREQUENCY}.
     *
     * @return the Channel
     */
    default FloatReadChannel getSupplyFrequencyChannel() {
        return this.channel(ChannelId.SUPPLY_FREQUENCY);
    }

    /**
     * Get the supply AC frequency in Hertz.
     * See {@link ChannelId#SUPPLY_FREQUENCY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Float> getSupplyFrequency() {
        return this.getSupplyFrequencyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#GENERATOR_FREQUENCY}.
     *
     * @return the Channel
     */
    default FloatReadChannel getGeneratorFrequencyChannel() {
        return this.channel(ChannelId.GENERATOR_FREQUENCY);
    }

    /**
     * Get the generator AC frequency in Hertz.
     * See {@link ChannelId#GENERATOR_FREQUENCY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Float> getGeneratorFrequency() {
        return this.getGeneratorFrequencyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_FACTOR}.
     *
     * @return the Channel
     */
    default FloatReadChannel getActivePowerFactorChannel() {
        return this.channel(ChannelId.ACTIVE_POWER_FACTOR);
    }

    /**
     * Get the active power factor. Unit is milli degree.
     * See {@link ChannelId#ACTIVE_POWER_FACTOR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Float> getActivePowerFactor() {
        return this.getActivePowerFactorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#RESERVE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getReserveChannel() {
        return this.channel(ChannelId.RESERVE);
    }

    /**
     * Get the reserve. Unit is kilo Watt hours.
     * See {@link ChannelId#RESERVE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getReserve() {
        return this.getReserveChannel().value();
    }
}