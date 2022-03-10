package io.openems.edge.heater.biomassheater.gilles.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Heater;

/**
 * Channels for the Gilles biomass heater.
 */
public interface BiomassHeaterGilles extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Holding registers, read/write, address 24576-24584.

        /*
         * Boiler temperature set point (Kesseltemperatur soll).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        //HR_24576_BOILER_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        // -> Heater SET_POINT_TEMPERATURE

        /**
         * Minimum flow temperature set point (Min. Vorlauftemperatur).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        HR_24577_MINIMUM_FLOW_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Slide in percentage value set point (Einschub %).
         * The amount of fuel delivered to the furnace.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        HR_24578_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Exhaust temperature set point at minimum heater power (Abgas min. Leistung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Exhaust temperature set point at maximum heater power (Abgas max. Leistung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Oxygen percent set point at minimum heater power (O2 min. Leistung). Range 60 - 210 per mill (6 - 21 %).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: per mill
         * </ul>
         */
        HR_24581_OXYGEN_PERCENT_MIN_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)),

        /**
         * Oxygen percent set point at maximum heater power (O2 max. Leistung). Range 60 - 210 per mill (6 - 21 %).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: per mill
         * </ul>
         */
        HR_24582_OXYGEN_PERCENT_MAX_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)),

        /**
         * Slide in percentage value lower limit set point (Einschub min).
         * The minimum amount of fuel delivered to the furnace.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        HR_24583_SLIDE_IN_MIN_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Slide in percentage value upper limit set point (Einschub max).
         * The maximum amount of fuel delivered to the furnace.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        HR_24584_SLIDE_IN_MAX_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),


        // Input registers, read only, address 20000-20035.

        /*
         * Boiler temperature (Kesseltemperatur ist).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        //IR_20000_BOILER_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        // -> Heater FLOW_TEMPERATURE

        /*
         * Return temperature (Ruecklauftemp. ist).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        //IR_20001_RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        // -> Heater RETURN_TEMPERATURE

        /**
         * Exhaust temperature (Abgastemperatur ist).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20002_EXHAUST_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Furnace temperature (Feuerraumtemperatur ist).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20003_FURNACE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //IR_20004_SLIDE_IN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        // -> Heater EFFECTIVE_HEATING_POWER_PERCENT
        // There is a second value like this, SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY. I think this value is the effective
        // value, and SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY is the setpoint.

        /**
         * Oxygen percent (O2 akt.). Range 0 - 21 %.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        IR_20005_OXYGEN_PERCENT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

        /**
         * Negative pressure (Unterdruck akt.). Range 0 - 1000 Pa.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Pascal
         * </ul>
         */
        IR_20006_NEGATIVE_PRESSURE(Doc.of(OpenemsType.INTEGER).unit(Unit.PASCAL)),

        /*
         * Heating power (Leistung akt.). Range 0 - 5000 kW.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt
         * </ul>
         */
        //IR_20007_HEATING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        // -> Heater EFFECTIVE_HEATING_POWER

        /**
         * Heating amount total (Waermemenge gesamt).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR_20008_HEATING_AMOUNT_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Percolation (Durchfluss).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: cubic meter per hour
         * </ul>
         */
        IR_20009_PERCOLATION(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBICMETER_PER_HOUR)),

        /**
         * Return temperature heat network (Ruecklauf Netz).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20010_RETURN_TEMPERATURE_HEAT_NETWORK(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 1 (Pufferfuehler 1).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20011_STORAGE_TANK_TEMPERATURE_SENSOR_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 2 (Pufferfuehler 2).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20012_STORAGE_TANK_TEMPERATURE_SENSOR_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 3 (Pufferfuehler 3).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20013_STORAGE_TANK_TEMPERATURE_SENSOR_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 4 (Pufferfuehler 4).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20014_STORAGE_TANK_TEMPERATURE_SENSOR_4(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 5 (Pufferfuehler 5).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20015_STORAGE_TANK_TEMPERATURE_SENSOR_5(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 6 (Pufferfuehler 6).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20016_STORAGE_TANK_TEMPERATURE_SENSOR_6(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 7 (Pufferfuehler 7).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20017_STORAGE_TANK_TEMPERATURE_SENSOR_7(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 8 (Pufferfuehler 8).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20018_STORAGE_TANK_TEMPERATURE_SENSOR_8(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 9 (Pufferfuehler 9).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20019_STORAGE_TANK_TEMPERATURE_SENSOR_9(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 10 (Pufferfuehler 10).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20020_STORAGE_TANK_TEMPERATURE_SENSOR_10(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 11 (Pufferfuehler 11).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20021_STORAGE_TANK_TEMPERATURE_SENSOR_11(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 12 (Pufferfuehler 12).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20022_STORAGE_TANK_TEMPERATURE_SENSOR_12(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 13 (Pufferfuehler 13).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20023_STORAGE_TANK_TEMPERATURE_SENSOR_13(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 14 (Pufferfuehler 14).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20024_STORAGE_TANK_TEMPERATURE_SENSOR_14(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 15 (Pufferfuehler 15).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20025_STORAGE_TANK_TEMPERATURE_SENSOR_15(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature sensor 16 (Pufferfuehler 16).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20026_STORAGE_TANK_TEMPERATURE_SENSOR_16(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Boiler temperature set point (Kesseltemperatur soll). Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20027_BOILER_TEMPERATURE_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Minimum flow temperature set point (Min. Vorlauftemperatur). Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20028_MINIMUM_FLOW_TEMPERATURE_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Slide in percentage value set point (Einschub %). Read only.
         * The amount of fuel delivered to the furnace.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        IR_20029_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

        /**
         * Exhaust temperature set point at minimum heater power (Abgas min. Leistung). Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20030_EXHAUST_TEMPERATURE_MIN_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Exhaust temperature set point at maximum heater power (Abgas max. Leistung). Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR_20031_EXHAUST_TEMPERATURE_MAX_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Oxygen percent set point at minimum heater power (O2 min. Leistung). Range 60 - 210 per mill (6 - 21 %).
         * Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: per mill
         * </ul>
         */
        IR_20032_OXYGEN_PERCENT_MIN_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),

        /**
         * Oxygen percent set point at maximum heater power (O2 max. Leistung). Range 60 - 210 per mill (6 - 21 %).
         * Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: per mill
         * </ul>
         */
        IR_20033_OXYGEN_PERCENT_MAX_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),

        /**
         * Slide in percentage value lower limit set point (Einschub min).
         * The minimum amount of fuel delivered to the furnace.
         * Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        IR_20034_SLIDE_IN_MIN_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

        /**
         * Slide in percentage value upper limit set point (Einschub max).
         * The minimum amount of fuel delivered to the furnace.
         * Read only.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: percent
         * </ul>
         */
        IR_20035_SLIDE_IN_MAX_SET_POINT_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),


        // Discrete input contacts, read only, address 10000-10025.

        /**
         * Error indicator (Stoerung).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10000_ERROR(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Warning indicator (Warnung).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10001_WARNING(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Cleaning indicator (Reinigung aktiv).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10002_CLEANING(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Fume extractor indicator (Saugzug ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10003_FUME_EXTRACTOR(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Air blower indicator (Geblaese ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10004_AIR_BLOWER(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Primary air blower indicator (Geblaese Prim. ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10005_PRIMARY_AIR_BLOWER(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Secondary air blower indicator (Geblaese Sek. ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10006_SECONDARY_AIR_BLOWER(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Stoker indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10007_STOKER(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Rotary valve indicator (Zellrad ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10008_ROTARY_VALVE(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * DOSI indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10009_DOSI(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Screw conveyor 1 indicator (Schnecke 1 ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10010_SCREW_CONVEYOR_1(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Screw conveyor 2 indicator (Schnecke 2 ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10011_SCREW_CONVEYOR_2(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Screw conveyor 3 indicator (Schnecke 3 ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10012_SCREW_CONVEYOR_3(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Cross conveyor indicator (Querfoerder ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10013_CROSS_CONVEYOR(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Moving floor 1 indicator (Schubboden 1 ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10014_MOVING_FLOOR_1(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Moving floor 2 indicator (Schubboden 2 ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10015_MOVING_FLOOR_2(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Ignition indicator (Zuendung ein).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10016_IGNITION(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * LS 1 indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10017_LS_1(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * LS 2 indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10018_LS_2(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * LS 3 indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10019_LS_3(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * LS lateral indicator (LS Quer).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10020_LS_LATERAL(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * LS moving floor indicator (LS Schubboden).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10021_LS_MOVING_FLOOR(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Ash screw conveyor 1 indicator (Ascheschnecke 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10022_ASH_SCREW_CONVEYOR_1(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Ash screw conveyor 2 indicator (Ascheschnecke 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10023_ASH_SCREW_CONVEYOR_2(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Signal contact 1 indicator (Meldekontakt 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10024_SIGNAL_CONTACT_1(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Signal contact 2 indicator (Meldekontakt 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_10025_SIGNAL_CONTACT_2(Doc.of(OpenemsType.BOOLEAN)),


        // Discrete output coils, read write, address 16387.

        /**
         * On/off switch.
         * Turns the heater on (true) or off (false).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DO_16387_ON_OFF_SWITCH(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24577_MINIMUM_FLOW_TEMPERATURE_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getMinimumFlowTemperatureSetPointChannel() {
        return this.channel(ChannelId.HR_24577_MINIMUM_FLOW_TEMPERATURE_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24578_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSlideInPercentageValueSetPointChannel() {
        return this.channel(ChannelId.HR_24578_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getExhaustTemperatureMinSetPointChannel() {
        return this.channel(ChannelId.HR_24579_EXHAUST_TEMPERATURE_MIN_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getExhaustTemperatureMaxSetPointChannel() {
        return this.channel(ChannelId.HR_24580_EXHAUST_TEMPERATURE_MAX_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24581_OXYGEN_PERCENT_MIN_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getOxygenPercentMinSetPointChannel() {
        return this.channel(ChannelId.HR_24581_OXYGEN_PERCENT_MIN_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24582_OXYGEN_PERCENT_MAX_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getOxygenPercentMaxSetPointChannel() {
        return this.channel(ChannelId.HR_24582_OXYGEN_PERCENT_MAX_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24583_SLIDE_IN_MIN_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSlideInMinSetPointChannel() {
        return this.channel(ChannelId.HR_24583_SLIDE_IN_MIN_SET_POINT);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_24584_SLIDE_IN_MAX_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSlideInMaxSetPointChannel() {
        return this.channel(ChannelId.HR_24584_SLIDE_IN_MAX_SET_POINT);
    }

    /**
     * Get the slide in percentage value upper limit set point (Einschub max).
     * The maximum amount of fuel delivered to the furnace.
     * Unit is percent.
     * See {@link ChannelId#HR_24584_SLIDE_IN_MAX_SET_POINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSlideInMaxSetPoint() {
        return this.getSlideInMaxSetPointChannel().value();
    }

    /**
     * Set the slide in percentage value upper limit set point (Einschub max).
     * The maximum amount of fuel delivered to the furnace.
     * Unit is percent.
     * See {@link ChannelId#HR_24584_SLIDE_IN_MAX_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSlideInMaxSetPoint(Integer value) throws OpenemsNamedException {
        this.getSlideInMaxSetPointChannel().setNextWriteValue(value);
    }

    /**
     * Set the slide in percentage value upper limit set point (Einschub max).
     * The maximum amount of fuel delivered to the furnace.
     * Unit is percent.
     * See {@link ChannelId#HR_24584_SLIDE_IN_MAX_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSlideInMaxSetPoint(int value) throws OpenemsNamedException {
        this.getSlideInMaxSetPointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20002_EXHAUST_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getExhaustTemperatureChannel() {
        return this.channel(ChannelId.IR_20002_EXHAUST_TEMPERATURE);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20003_FURNACE_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getFurnaceTemperatureChannel() {
        return this.channel(ChannelId.IR_20003_FURNACE_TEMPERATURE);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20005_OXYGEN_PERCENT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOxygenPercentChannel() {
        return this.channel(ChannelId.IR_20005_OXYGEN_PERCENT);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20006_NEGATIVE_PRESSURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getNegativePressureChannel() {
        return this.channel(ChannelId.IR_20006_NEGATIVE_PRESSURE);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20008_HEATING_AMOUNT_TOTAL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHeatingAmountTotalChannel() {
        return this.channel(ChannelId.IR_20008_HEATING_AMOUNT_TOTAL);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20009_PERCOLATION}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getPercolationChannel() {
        return this.channel(ChannelId.IR_20009_PERCOLATION);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20010_RETURN_TEMPERATURE_HEAT_NETWORK}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getReturnTemperatureHeatNetworkChannel() {
        return this.channel(ChannelId.IR_20010_RETURN_TEMPERATURE_HEAT_NETWORK);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20011_STORAGE_TANK_TEMPERATURE_SENSOR_1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor1Channel() {
        return this.channel(ChannelId.IR_20011_STORAGE_TANK_TEMPERATURE_SENSOR_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20012_STORAGE_TANK_TEMPERATURE_SENSOR_2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor2Channel() {
        return this.channel(ChannelId.IR_20012_STORAGE_TANK_TEMPERATURE_SENSOR_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20013_STORAGE_TANK_TEMPERATURE_SENSOR_3}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor3Channel() {
        return this.channel(ChannelId.IR_20013_STORAGE_TANK_TEMPERATURE_SENSOR_3);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20014_STORAGE_TANK_TEMPERATURE_SENSOR_4}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor4Channel() {
        return this.channel(ChannelId.IR_20014_STORAGE_TANK_TEMPERATURE_SENSOR_4);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20015_STORAGE_TANK_TEMPERATURE_SENSOR_5}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor5Channel() {
        return this.channel(ChannelId.IR_20015_STORAGE_TANK_TEMPERATURE_SENSOR_5);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20016_STORAGE_TANK_TEMPERATURE_SENSOR_6}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor6Channel() {
        return this.channel(ChannelId.IR_20016_STORAGE_TANK_TEMPERATURE_SENSOR_6);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20017_STORAGE_TANK_TEMPERATURE_SENSOR_7}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor7Channel() {
        return this.channel(ChannelId.IR_20017_STORAGE_TANK_TEMPERATURE_SENSOR_7);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20018_STORAGE_TANK_TEMPERATURE_SENSOR_8}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor8Channel() {
        return this.channel(ChannelId.IR_20018_STORAGE_TANK_TEMPERATURE_SENSOR_8);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20019_STORAGE_TANK_TEMPERATURE_SENSOR_9}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor9Channel() {
        return this.channel(ChannelId.IR_20019_STORAGE_TANK_TEMPERATURE_SENSOR_9);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20020_STORAGE_TANK_TEMPERATURE_SENSOR_10}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor10Channel() {
        return this.channel(ChannelId.IR_20020_STORAGE_TANK_TEMPERATURE_SENSOR_10);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20021_STORAGE_TANK_TEMPERATURE_SENSOR_11}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor11Channel() {
        return this.channel(ChannelId.IR_20021_STORAGE_TANK_TEMPERATURE_SENSOR_11);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20022_STORAGE_TANK_TEMPERATURE_SENSOR_12}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor12Channel() {
        return this.channel(ChannelId.IR_20022_STORAGE_TANK_TEMPERATURE_SENSOR_12);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20023_STORAGE_TANK_TEMPERATURE_SENSOR_13}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor13Channel() {
        return this.channel(ChannelId.IR_20023_STORAGE_TANK_TEMPERATURE_SENSOR_13);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20024_STORAGE_TANK_TEMPERATURE_SENSOR_14}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor14Channel() {
        return this.channel(ChannelId.IR_20024_STORAGE_TANK_TEMPERATURE_SENSOR_14);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20025_STORAGE_TANK_TEMPERATURE_SENSOR_15}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor15Channel() {
        return this.channel(ChannelId.IR_20025_STORAGE_TANK_TEMPERATURE_SENSOR_15);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20026_STORAGE_TANK_TEMPERATURE_SENSOR_16}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureSensor16Channel() {
        return this.channel(ChannelId.IR_20026_STORAGE_TANK_TEMPERATURE_SENSOR_16);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20027_BOILER_TEMPERATURE_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getBoilerTemperatureSetPointReadChannel() {
        return this.channel(ChannelId.IR_20027_BOILER_TEMPERATURE_SET_POINT_READ);
    }

    /**
     * Get the boiler temperature set point (Kesseltemperatur soll).
     * Unit is decimal degree Celsius.
     * See {@link ChannelId#IR_20027_BOILER_TEMPERATURE_SET_POINT_READ}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getBoilerTemperatureSetPointRead() {
        return this.getBoilerTemperatureSetPointReadChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20028_MINIMUM_FLOW_TEMPERATURE_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMinimumFlowTemperatureSetPointReadChannel() {
        return this.channel(ChannelId.IR_20028_MINIMUM_FLOW_TEMPERATURE_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20029_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSlideInPercentageValueSetPointReadChannel() {
        return this.channel(ChannelId.IR_20029_SLIDE_IN_PERCENTAGE_VALUE_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20030_EXHAUST_TEMPERATURE_MIN_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getExhaustTemperatureMinSetPointReadChannel() {
        return this.channel(ChannelId.IR_20030_EXHAUST_TEMPERATURE_MIN_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20031_EXHAUST_TEMPERATURE_MAX_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getExhaustTemperatureMaxSetPointReadChannel() {
        return this.channel(ChannelId.IR_20031_EXHAUST_TEMPERATURE_MAX_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20032_OXYGEN_PERCENT_MIN_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOxygenPercentMinSetPointReadChannel() {
        return this.channel(ChannelId.IR_20032_OXYGEN_PERCENT_MIN_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20033_OXYGEN_PERCENT_MAX_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOxygenPercentMaxSetPointReadChannel() {
        return this.channel(ChannelId.IR_20033_OXYGEN_PERCENT_MAX_SET_POINT_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20034_SLIDE_IN_MIN_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSlideInMinSetPointReadChannel() {
        return this.channel(ChannelId.IR_20034_SLIDE_IN_MIN_SET_POINT_READ);
    }

    /**
     * Get the slide in percentage value lower limit set point (Einschub min).
     * The minimum amount of fuel delivered to the furnace.
     * Unit is percent.
     * See {@link ChannelId#IR_20034_SLIDE_IN_MIN_SET_POINT_READ}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSlideInMinSetPointRead() {
        return this.getSlideInMinSetPointReadChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20035_SLIDE_IN_MAX_SET_POINT_READ}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSlideInMaxSetPointReadChannel() {
        return this.channel(ChannelId.IR_20035_SLIDE_IN_MAX_SET_POINT_READ);
    }

    /**
     * Get the slide in percentage value upper limit set point (Einschub max).
     * The maximum amount of fuel delivered to the furnace.
     * Unit is percent.
     * See {@link ChannelId#IR_20035_SLIDE_IN_MAX_SET_POINT_READ}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSlideInMaxSetPointRead() {
        return this.getSlideInMaxSetPointReadChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10000_ERROR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getErrorIndicatorChannel() {
        return this.channel(ChannelId.DI_10000_ERROR);
    }

    /**
     * Get the error indicator (Stoerung).
     * True means ’error’, false means ’no error’.
     * See {@link ChannelId#DI_10000_ERROR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getErrorIndicator() {
        return this.getErrorIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10001_WARNING}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getWarningIndicatorChannel() {
        return this.channel(ChannelId.DI_10001_WARNING);
    }

    /**
     * Get the warning indicator (Warnung).
     * True means ’warning’, false means ’no warning’.
     * See {@link ChannelId#DI_10001_WARNING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getWarningIndicator() {
        return this.getWarningIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10002_CLEANING}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getCleaningIndicatorChannel() {
        return this.channel(ChannelId.DI_10002_CLEANING);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10003_FUME_EXTRACTOR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getFumeExtractorIndicatorChannel() {
        return this.channel(ChannelId.DI_10003_FUME_EXTRACTOR);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10004_AIR_BLOWER}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getAirBlowerIndicatorChannel() {
        return this.channel(ChannelId.DI_10004_AIR_BLOWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10005_PRIMARY_AIR_BLOWER}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getPrimaryAirBlowerIndicatorChannel() {
        return this.channel(ChannelId.DI_10005_PRIMARY_AIR_BLOWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10006_SECONDARY_AIR_BLOWER}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getSecondaryAirBlowerIndicatorChannel() {
        return this.channel(ChannelId.DI_10006_SECONDARY_AIR_BLOWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10007_STOKER}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getStokerIndicatorChannel() {
        return this.channel(ChannelId.DI_10007_STOKER);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10008_ROTARY_VALVE}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getRotaryValveIndicatorChannel() {
        return this.channel(ChannelId.DI_10008_ROTARY_VALVE);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10009_DOSI}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getDosiIndicatorChannel() {
        return this.channel(ChannelId.DI_10009_DOSI);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10010_SCREW_CONVEYOR_1}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getScrewConveyor1IndicatorChannel() {
        return this.channel(ChannelId.DI_10010_SCREW_CONVEYOR_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10011_SCREW_CONVEYOR_2}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getScrewConveyor2IndicatorChannel() {
        return this.channel(ChannelId.DI_10011_SCREW_CONVEYOR_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10012_SCREW_CONVEYOR_3}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getScrewConveyor3IndicatorChannel() {
        return this.channel(ChannelId.DI_10012_SCREW_CONVEYOR_3);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10013_CROSS_CONVEYOR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getCrossConveyorIndicatorChannel() {
        return this.channel(ChannelId.DI_10013_CROSS_CONVEYOR);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10014_MOVING_FLOOR_1}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getMovingFloor1IndicatorChannel() {
        return this.channel(ChannelId.DI_10014_MOVING_FLOOR_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10015_MOVING_FLOOR_2}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getMovingFloor2IndicatorChannel() {
        return this.channel(ChannelId.DI_10015_MOVING_FLOOR_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10016_IGNITION}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getIgnitionIndicatorChannel() {
        return this.channel(ChannelId.DI_10016_IGNITION);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10017_LS_1}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getLs1IndicatorChannel() {
        return this.channel(ChannelId.DI_10017_LS_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10018_LS_2}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getLs2IndicatorChannel() {
        return this.channel(ChannelId.DI_10018_LS_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10019_LS_3}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getLs3IndicatorChannel() {
        return this.channel(ChannelId.DI_10019_LS_3);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10020_LS_LATERAL}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getLsLateralIndicatorChannel() {
        return this.channel(ChannelId.DI_10020_LS_LATERAL);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10021_LS_MOVING_FLOOR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getLsMovingFloorIndicatorChannel() {
        return this.channel(ChannelId.DI_10021_LS_MOVING_FLOOR);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10022_ASH_SCREW_CONVEYOR_1}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getAshScrewConveyor1IndicatorChannel() {
        return this.channel(ChannelId.DI_10022_ASH_SCREW_CONVEYOR_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10023_ASH_SCREW_CONVEYOR_2}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getAshScrewConveyor2IndicatorChannel() {
        return this.channel(ChannelId.DI_10023_ASH_SCREW_CONVEYOR_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10024_SIGNAL_CONTACT_1}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getSignalContact1IndicatorChannel() {
        return this.channel(ChannelId.DI_10024_SIGNAL_CONTACT_1);
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_10025_SIGNAL_CONTACT_2}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getSignalContact2IndicatorChannel() {
        return this.channel(ChannelId.DI_10025_SIGNAL_CONTACT_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#DO_16387_ON_OFF_SWITCH}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getOnOffSwitchChannel() {
        return this.channel(ChannelId.DO_16387_ON_OFF_SWITCH);
    }

    /**
     * Get the on/off indicator.
     * True means ’turn on heater’, false means ’turn off heater’.
     * See {@link ChannelId#DO_16387_ON_OFF_SWITCH}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getOnOffSwitch() {
        return this.getOnOffSwitchChannel().value();
    }

    /**
     * Set the on/off switch of the heater.
     * True means ’turn on heater’, false means ’turn off heater’.
     * See {@link ChannelId#DO_16387_ON_OFF_SWITCH}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOnOffSwitch(Boolean value) throws OpenemsNamedException {
        this.getOnOffSwitchChannel().setNextWriteValue(value);
    }
}

