package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

/**
 * Channels for the Tecalor heat pump.
 */

public interface HeatpumpTecalor extends HeatpumpSmartGrid {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /* Input Registers, read only. They are 16 bit signed numbers unless stated otherwise.
           Addresses are 1 based, so 40001 is the first holding register. OpenEMS Modbus is 0 based,
           so 40001 has address 0. */

        /**
         * Outside temperature.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR507_OUTSIDE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR508_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 set point temperature, if software version is WPM 3i.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR509_TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 set point temperature, if software version is WPMsystem and WPM 3.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR510_TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 set point temperature, independent of software version.
         * (Mapped from the software dependent channels)
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR511_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 set point temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR512_TEMP_HC2_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Flow temperature heat pump.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR513_FLOW_TEMP_HEAT_PUMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Flow temperature auxiliary heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR514_FLOW_TEMP_AUX(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //IR515_FLOW_TEMPERATURE -> Heater FLOW_TEMPERATURE

        //IR516_RETURN_TEMPERATURE -> Heater RETURN_TEMPERATURE

        /**
         * Constant temperature set point.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR517_CONST_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR518_STORAGE_TANK_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature, set point.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR519_STORAGE_TANK_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heating circuit pressure.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: centi bar (bar e-2)
         * </ul>
         */
        IR520_CIRCUIT_PRESSURE(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTI_BAR)),

        /**
         * Heating circuit current.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: decimal liters per minute (l/min e-1)
         * </ul>
         */
        IR521_CIRCUIT_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECILITER_PER_MINUTE)),

        /**
         * Domestic hot water temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR522_WATER_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Domestic hot water temperature, set point.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR523_WATER_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Ventilation cooling temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR524_VENT_COOLING_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * Ventilation cooling temperature, set point.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR525_VENT_COOLING_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * Surface cooling temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR526_SURFACE_COOLING_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * Surface cooling temperature, set point.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR527_SURFACE_COOLING_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * Status bits.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2501_STATUSBITS(Doc.of(OpenemsType.INTEGER)),

        /**
         * Demand side management (DSM) switch (EVU Freigabe).
         * ’true’ = heat pump is allowed to run, ’false’ = stop heat pump.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2502_DSM_SWITCH(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Error status. False for no error.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2504_ERRORSTATUS(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * BUS status.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2505_BUSSTATUS(Doc.of(OpenemsType.INTEGER)),

        /**
         * Defrost active.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2506_DEFROST(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Error code.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2507_ERROR_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Produced heat for heating circuits, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3501_HEATPRODUCED_CIRCUIT_DAILY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3502_HEATPRODUCED_CIRCUIT_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3503_HEATPRODUCED_CIRCUIT_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_CIRCUIT_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for domestic hot water, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3504_HEATPRODUCED_WATER_DAILY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for domestic hot water total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3505_HEATPRODUCED_WATER_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for domestic hot water total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3506_HEATPRODUCED_WATER_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for domestic hot water total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_WATER_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, auxiliary heater. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3507_HEATPRODUCED_AUX_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, auxiliary heater. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3508_HEATPRODUCED_AUX_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, auxiliary heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_AUX_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, auxiliary heater. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3509_HEATPRODUCED_WATER_AUX_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, auxiliary heater. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3510_HEATPRODUCED_WATER_AUX_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for warm water total, auxiliary heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_WATER_AUX_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3511_CONSUMEDPOWER_CIRCUIT_DAILY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3512_CONSUMEDPOWER_CIRCUIT_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3513_CONSUMEDPOWER_CIRCUIT_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        CONSUMEDPOWER_CIRCUIT_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating domestic hot water, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3514_CONSUMEDPOWER_WATER_DAILY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating domestic hot water total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3515_CONSUMEDPOWER_WATER_SUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating domestic hot water total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3516_CONSUMEDPOWER_WATER_SUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Consumed power for heating domestic hot water total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        CONSUMEDPOWER_WATER_SUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * SG-Ready Operating mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 1 ... 4
         *      <li> State 1: Blocked (Die Anlage darf nicht starten. Nur der Frostschutz wird gewaehrleistet.)
         *      <li> State 2: Standard (Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemaess BI der angeschlossenen Waermepumpe.)
         *      <li> State 3: Force on with increased temperature levels (Forcierter Betrieb der Anlage mit erhoehten Werten fuer Heiz- und/oder Warmwassertemperatur.)
         *      <li> State 4: Force on with maximum temperature levels (Sofortige Ansteuerung der Maximalwerte fuer Heiz- und Warmwassertemperatur.)
         * </ul>
         */
        IR5001_SGREADY_OPERATINGMODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Controller model id (Reglerkennung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Value 103: THZ 303, 403 (Integral/SOL), THD 400 AL, THZ 304 eco, 404 eco, THZ 304/404 FLEX,
         *      THZ 5.5 eco, THZ 5.5 FLEX, TCO 2.5
         *      <li> Value 104: THZ 304, 404 (SOL), THZ 504
         *      <li> Value 390: WPM 3
         *      <li> Value 391: WPM 3i
         *      <li> Value 449: WPMsystem
         * </ul>
         */
        IR5002_CONTROLLER_MODEL_ID(Doc.of(OpenemsType.INTEGER)),


        // Holding Registers, read/write. Signed 16 bit, unless stated otherwise.

        /**
         * Operating mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 5
         *      <li> State 0: Antifreeze protection only (Notbetrieb)
         *      <li> State 1: Standby mode (Bereitschaftsbetrieb)
         *      <li> State 2: Program mode (Programmbetrieb)
         *      <li> State 3: Comfort mode (Komfortbetrieb)
         *      <li> State 4: ECO mode (ECO-Betrieb)
         *      <li> State 5: Domestic hot water (Warmwasserbetrieb)
         * </ul>
         */
        HR1501_OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1502_COMFORT_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1503_ECO_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve slope setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1504_SLOPE_HC1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1505_COMFORT_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1506_ECO_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve slope setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1507_SLOPE_HC2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Constant temperature mode setting (Festwertbetrieb). 0x9000 disables this mode, a temperature value
         * between 200 and 700 enables this mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1508_CONST_TEMP_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Auxiliary heater activation temperature, heating circuit (Bivalenztemperatur). Below this temperature the
         * auxiliary heater will activate, depending on heat demand.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1509_CIRCUIT_AUX_ACT_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, domestic hot water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1510_COMFORT_TEMP_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, domestic hot water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1511_ECO_TEMP_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Number of stages, domestic hot water.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1512_WATER_STAGES(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Auxiliary heater activation temperature, domestic hot water. Below this temperature the auxiliary heater will
         * activate, depending on heat demand.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1513_WATER_AUX_ACT_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature set point, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature hysteresis, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1515_SURFACE_COOLING_FLOW_TEMP_HYST(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature set point, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature set point, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature hysteresis, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1518_VENT_COOLING_FLOW_TEMP_HYST(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature set point, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Turn on/off SG-Ready mode.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4001_SGREADY_ONOFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SG-Ready input 1.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4002_SGREADY_INPUT1(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SG-Ready input 2.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4003_SGREADY_INPUT2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Status of the heat pump.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        ;


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Input Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR507_OUTSIDE_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOutsideTempChannel() {
        return this.channel(ChannelId.IR507_OUTSIDE_TEMP);
    }

    /**
     * Gets the outside temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR507_OUTSIDE_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOutsideTemp() {
        return this.getOutsideTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR508_TEMP_HC1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getCircuit1TempChannel() {
        return this.channel(ChannelId.IR508_TEMP_HC1);
    }

    /**
     * Gets the heating circuit 1 temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR508_TEMP_HC1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getCircuit1Temp() {
        return this.getCircuit1TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TEMP_HC1_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getCircuit1SetpointTempChannel() {
        return this.channel(ChannelId.TEMP_HC1_SETPOINT);
    }

    /**
     * Gets the heating circuit 1 set point temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#TEMP_HC1_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getCircuit1SetpointTemp() {
        return this.getCircuit1TempChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TEMP_HC1_SETPOINT} Channel.
     *
     * @param value the next value
     */
    default void _setCircuit1SetpointTemp(Integer value) {
        this.getCircuit1SetpointTempChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#IR511_TEMP_HC2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getCircuit2TempChannel() {
        return this.channel(ChannelId.IR511_TEMP_HC2);
    }

    /**
     * Gets the heating circuit 2 temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR511_TEMP_HC2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getCircuit2Temp() {
        return this.getCircuit2TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR512_TEMP_HC2_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getCircuit2SetpointTempChannel() {
        return this.channel(ChannelId.IR512_TEMP_HC2_SETPOINT);
    }

    /**
     * Gets the heating circuit 2 set point temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR512_TEMP_HC2_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getCircuit2SetpointTemp() {
        return this.getCircuit2SetpointTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR513_FLOW_TEMP_HEAT_PUMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getFlowTempHeatPumpChannel() {
        return this.channel(ChannelId.IR513_FLOW_TEMP_HEAT_PUMP);
    }

    /**
     * Gets the flow temperature heat pump. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR513_FLOW_TEMP_HEAT_PUMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getFlowTempHeatPump() {
        return this.getFlowTempHeatPumpChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR514_FLOW_TEMP_AUX}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getFlowTempAuxHeaterChannel() {
        return this.channel(ChannelId.IR514_FLOW_TEMP_AUX);
    }

    /**
     * Gets the flow temperature auxiliary heater. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR514_FLOW_TEMP_AUX}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getFlowTempAuxHeater() {
        return this.getFlowTempAuxHeaterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR517_CONST_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getConstTempSetpointChannel() {
        return this.channel(ChannelId.IR517_CONST_TEMP_SETPOINT);
    }

    /**
     * Gets the constant temperature set point. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR517_CONST_TEMP_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getConstTempSetpoint() {
        return this.getConstTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR518_STORAGE_TANK_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTempChannel() {
        return this.channel(ChannelId.IR518_STORAGE_TANK_TEMP);
    }

    /**
     * Gets the storage tank temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR518_STORAGE_TANK_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStorageTankTemp() {
        return this.getStorageTankTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR519_STORAGE_TANK_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTempSetpointChannel() {
        return this.channel(ChannelId.IR519_STORAGE_TANK_TEMP_SETPOINT);
    }

    /**
     * Gets the storage tank temperature set point. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR519_STORAGE_TANK_TEMP_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStorageTankTempSetpoint() {
        return this.getStorageTankTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR520_CIRCUIT_PRESSURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHeatingCircuitPressureChannel() {
        return this.channel(ChannelId.IR520_CIRCUIT_PRESSURE);
    }

    /**
     * Gets the heating circuit pressure. Unit is centi bar (bar e-2).
     * See {@link ChannelId#IR520_CIRCUIT_PRESSURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHeatingCircuitPressure() {
        return this.getHeatingCircuitPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR521_CIRCUIT_CURRENT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHeatingCircuitCurrentChannel() {
        return this.channel(ChannelId.IR521_CIRCUIT_CURRENT);
    }

    /**
     * Gets the heating circuit current. Unit is decimal liters per minute (l/min e-1).
     * See {@link ChannelId#IR521_CIRCUIT_CURRENT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHeatingCircuitCurrent() {
        return this.getHeatingCircuitCurrentChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR522_WATER_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getDomesticHotWaterTempChannel() {
        return this.channel(ChannelId.IR522_WATER_TEMP);
    }

    /**
     * Gets the domestic hot water temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR522_WATER_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getDomesticHotWaterTemp() {
        return this.getDomesticHotWaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR523_WATER_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getDomesticHotWaterTempSetpointChannel() {
        return this.channel(ChannelId.IR523_WATER_TEMP_SETPOINT);
    }

    /**
     * Gets the domestic hot water temperature set point. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR523_WATER_TEMP_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getDomesticHotWaterTempSetpoint() {
        return this.getDomesticHotWaterTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR524_VENT_COOLING_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getVentilationCoolingTempChannel() {
        return this.channel(ChannelId.IR524_VENT_COOLING_TEMP);
    }

    /**
     * Gets the ventilation cooling temperature. Unit is decimal degree Kelvin.
     * See {@link ChannelId#IR524_VENT_COOLING_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getVentilationCoolingTemp() {
        return this.getVentilationCoolingTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR525_VENT_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getVentilationCoolingTempSetpointChannel() {
        return this.channel(ChannelId.IR525_VENT_COOLING_TEMP_SETPOINT);
    }

    /**
     * Gets the ventilation cooling temperature set point. Unit is decimal degree Kelvin.
     * See {@link ChannelId#IR525_VENT_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getVentilationCoolingTempSetpoint() {
        return this.getVentilationCoolingTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR526_SURFACE_COOLING_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSurfaceCoolingTempChannel() {
        return this.channel(ChannelId.IR526_SURFACE_COOLING_TEMP);
    }

    /**
     * Gets the surface cooling temperature. Unit is decimal degree Kelvin.
     * See {@link ChannelId#IR526_SURFACE_COOLING_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSurfaceCoolingTemp() {
        return this.getSurfaceCoolingTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR527_SURFACE_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSurfaceCoolingTempSetpointChannel() {
        return this.channel(ChannelId.IR527_SURFACE_COOLING_TEMP_SETPOINT);
    }

    /**
     * Gets the surface cooling temperature set point. Unit is decimal degree Kelvin.
     * See {@link ChannelId#IR527_SURFACE_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSurfaceCoolingTempSetpoint() {
        return this.getSurfaceCoolingTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2501_STATUSBITS}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStatusBitsChannel() {
        return this.channel(ChannelId.IR2501_STATUSBITS);
    }

    /**
     * Gets the status bits.
     * See {@link ChannelId#IR2501_STATUSBITS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStatusBits() {
        return this.getStatusBitsChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2502_DSM_SWITCH}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getDsmSwitchChannel() {
        return this.channel(ChannelId.IR2502_DSM_SWITCH);
    }

    /**
     * Gets the demand side management (DSM) status (EVU Freigabe).
     * ’true’ = heat pump is allowed to run, ’false’ = stop heat pump.
     * See {@link ChannelId#IR2502_DSM_SWITCH}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getDsmSwitch() {
        return this.getDsmSwitchChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2504_ERRORSTATUS}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getErrorStatusChannel() {
        return this.channel(ChannelId.IR2504_ERRORSTATUS);
    }

    /**
     * Gets the error status. False for no error.
     * See {@link ChannelId#IR2504_ERRORSTATUS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getErrorStatus() {
        return this.getErrorStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2505_BUSSTATUS}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getBusStatusChannel() {
        return this.channel(ChannelId.IR2505_BUSSTATUS);
    }

    /**
     * Gets the BUS status.
     * See {@link ChannelId#IR2505_BUSSTATUS}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getBusStatus() {
        return this.getBusStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2506_DEFROST}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getDefrostActiveChannel() {
        return this.channel(ChannelId.IR2506_DEFROST);
    }

    /**
     * Defrost active.
     * See {@link ChannelId#IR2506_DEFROST}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getDefrostActive() {
        return this.getDefrostActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2507_ERROR_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.IR2507_ERROR_CODE);
    }

    /**
     * Gets the error code.
     * See {@link ChannelId#IR2507_ERROR_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3501_HEATPRODUCED_CIRCUIT_DAILY}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatCircuitDailyChannel() {
        return this.channel(ChannelId.IR3501_HEATPRODUCED_CIRCUIT_DAILY);
    }

    /**
     * Produced heat for heating circuits, all heat pumps combined for this day.
     * See {@link ChannelId#IR3501_HEATPRODUCED_CIRCUIT_DAILY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatCircuitDaily() {
        return this.getProducedHeatCircuitDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_CIRCUIT_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatCircuitTotalChannel() {
        return this.channel(ChannelId.HEATPRODUCED_CIRCUIT_SUM);
    }

    /**
     * Produced heat for heating circuits total, all heat pumps combined.
     * See {@link ChannelId#HEATPRODUCED_CIRCUIT_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatCircuitTotal() {
        return this.getProducedHeatCircuitTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3504_HEATPRODUCED_WATER_DAILY}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatWaterDailyChannel() {
        return this.channel(ChannelId.IR3504_HEATPRODUCED_WATER_DAILY);
    }

    /**
     * Produced heat for domestic hot water, all heat pumps combined for this day.
     * See {@link ChannelId#IR3504_HEATPRODUCED_WATER_DAILY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatWaterDaily() {
        return this.getProducedHeatWaterDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_WATER_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatWaterTotalChannel() {
        return this.channel(ChannelId.HEATPRODUCED_WATER_SUM);
    }

    /**
     * Produced heat for domestic hot water total, all heat pumps combined.
     * See {@link ChannelId#HEATPRODUCED_WATER_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatWaterTotal() {
        return this.getProducedHeatWaterTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_AUX_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatCircuitTotalAuxChannel() {
        return this.channel(ChannelId.HEATPRODUCED_AUX_SUM);
    }

    /**
     * Produced heat for heating circuits total, auxiliary heater.
     * See {@link ChannelId#HEATPRODUCED_AUX_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatCircuitTotalAux() {
        return this.getProducedHeatCircuitTotalAuxChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_WATER_AUX_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatWaterTotalAuxChannel() {
        return this.channel(ChannelId.HEATPRODUCED_WATER_AUX_SUM);
    }

    /**
     * Produced heat for domestic hot water total, auxiliary heater.
     * See {@link ChannelId#HEATPRODUCED_WATER_AUX_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeatWaterTotalAux() {
        return this.getProducedHeatWaterTotalAuxChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3511_CONSUMEDPOWER_CIRCUIT_DAILY}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getConsumedPowerCircuitDailyChannel() {
        return this.channel(ChannelId.IR3511_CONSUMEDPOWER_CIRCUIT_DAILY);
    }

    /**
     * Consumed power for heating the heating circuits, all heat pumps combined for this day.
     * See {@link ChannelId#IR3511_CONSUMEDPOWER_CIRCUIT_DAILY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getConsumedPowerCircuitDaily() {
        return this.getConsumedPowerCircuitDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_CIRCUIT_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getConsumedPowerCircuitTotalChannel() {
        return this.channel(ChannelId.CONSUMEDPOWER_CIRCUIT_SUM);
    }

    /**
     * Consumed power for heating the heating circuits total, all heat pumps combined.
     * See {@link ChannelId#CONSUMEDPOWER_CIRCUIT_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getConsumedPowerCircuitTotal() {
        return this.getConsumedPowerCircuitTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3514_CONSUMEDPOWER_WATER_DAILY}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getConsumedPowerWaterDailyChannel() {
        return this.channel(ChannelId.IR3514_CONSUMEDPOWER_WATER_DAILY);
    }

    /**
     * Consumed power for heating domestic hot water, all heat pumps combined for this day.
     * See {@link ChannelId#IR3514_CONSUMEDPOWER_WATER_DAILY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getConsumedPowerWaterDaily() {
        return this.getConsumedPowerWaterDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_WATER_SUM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getConsumedPowerWaterTotalChannel() {
        return this.channel(ChannelId.CONSUMEDPOWER_WATER_SUM);
    }

    /**
     * Consumed power for heating domestic hot water total, all heat pumps combined.
     * See {@link ChannelId#CONSUMEDPOWER_WATER_SUM}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getConsumedPowerWaterTotal() {
        return this.getConsumedPowerWaterTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR5001_SGREADY_OPERATINGMODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getSgReadyOperatingModeChannel() {
        return this.channel(ChannelId.IR5001_SGREADY_OPERATINGMODE);
    }

    /**
     * SG-Ready Operating mode, read. Mapped to SmartGridState of HeatpumpSmartGrid interface.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 1 ... 4
     *      <li> State 1: Blocked (Die Anlage darf nicht starten. Nur der Frostschutz wird gewaehrleistet.)
     *      <li> State 2: Standard (Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemaess BI der angeschlossenen Waermepumpe.)
     *      <li> State 3: Force on with increased temperature levels (Forcierter Betrieb der Anlage mit erhoehten Werten fuer Heiz- und/oder Warmwassertemperatur.)
     *      <li> State 4: Force on with maximum temperature levels (Sofortige Ansteuerung der Maximalwerte fuer Heiz- und Warmwassertemperatur.)
     * </ul>
     * See {@link ChannelId#IR5001_SGREADY_OPERATINGMODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getSgReadyOperatingMode() {
        return this.getSgReadyOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR5002_CONTROLLER_MODEL_ID}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getControllerModelIdChannel() {
        return this.channel(ChannelId.IR5002_CONTROLLER_MODEL_ID);
    }

    /**
     * Controller model id (Reglerkennung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Value 103: THZ 303, 403 (Integral/SOL), THD 400 AL, THZ 304 eco, 404 eco, THZ 304/404 FLEX,
     *      THZ 5.5 eco, THZ 5.5 FLEX, TCO 2.5
     *      <li> Value 104: THZ 304, 404 (SOL), THZ 504
     *      <li> Value 390: WPM 3
     *      <li> Value 391: WPM 3i
     *      <li> Value 449: WPMsystem
     * </ul>
     * See {@link ChannelId#IR5002_CONTROLLER_MODEL_ID}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getControllerModelId() {
        return this.getControllerModelIdChannel().value();
    }


    // Holding Registers. Read/Write.

    /**
     * Gets the Channel for {@link ChannelId#HR1501_OPERATING_MODE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getOperatingModeChannel() {
        return this.channel(ChannelId.HR1501_OPERATING_MODE);
    }

    /**
     * Get the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Antifreeze protection only (Notbetrieb)
     *      <li> State 1: Standby mode (Bereitschaftsbetrieb)
     *      <li> State 2: Program mode (Programmbetrieb)
     *      <li> State 3: Comfort mode (Komfortbetrieb)
     *      <li> State 4: ECO mode (ECO-Betrieb)
     *      <li> State 5: Domestic hot water (Warmwasserbetrieb)
     * </ul>
	 * See {@link ChannelId#HR1501_OPERATING_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getOperatingMode() {
		return this.getOperatingModeChannel().value();
	}
	
	/**
     * Set the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Antifreeze protection only (Notbetrieb)
     *      <li> State 1: Standby mode (Bereitschaftsbetrieb)
     *      <li> State 2: Program mode (Programmbetrieb)
     *      <li> State 3: Comfort mode (Komfortbetrieb)
     *      <li> State 4: ECO mode (ECO-Betrieb)
     *      <li> State 5: Domestic hot water (Warmwasserbetrieb)
     * </ul> 
	 * See {@link ChannelId#HR1501_OPERATING_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setOperatingMode(int value) throws OpenemsNamedException {
		this.getOperatingModeChannel().setNextWriteValue(value);
	}

    /**
     * Set the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Antifreeze protection only (Notbetrieb)
     *      <li> State 1: Standby mode (Bereitschaftsbetrieb)
     *      <li> State 2: Program mode (Programmbetrieb)
     *      <li> State 3: Comfort mode (Komfortbetrieb)
     *      <li> State 4: ECO mode (ECO-Betrieb)
     *      <li> State 5: Domestic hot water (Warmwasserbetrieb)
     * </ul>
     * See {@link ChannelId#HR1501_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(Integer value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getComfortTempCircuit1Channel() {
        return this.channel(ChannelId.HR1502_COMFORT_TEMP_HC1);
    }
    
    /**
     * Get the comfort temperature setting, heating circuit 1. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getComfortTempCircuit1() {
		return this.getComfortTempCircuit1Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 1. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setComfortTempCircuit1(int value) throws OpenemsNamedException {
		this.getComfortTempCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting, heating circuit 1. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setComfortTempCircuit1(Integer value) throws OpenemsNamedException {
        this.getComfortTempCircuit1Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1503_ECO_TEMP_HC1}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getEcoTempCircuit1Channel() {
        return this.channel(ChannelId.HR1503_ECO_TEMP_HC1);
    }

    /**
     * Get the ECO temperature setting, heating circuit 1. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getEcoTempCircuit1() {
		return this.getEcoTempCircuit1Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 1. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setEcoTempCircuit1(int value) throws OpenemsNamedException {
		this.getEcoTempCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting, heating circuit 1. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setEcoTempCircuit1(Integer value) throws OpenemsNamedException {
        this.getEcoTempCircuit1Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1504_SLOPE_HC1}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHeatingCurveSlopeCircuit1Channel() {
        return this.channel(ChannelId.HR1504_SLOPE_HC1);
    }

    /**
     * Get the heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPE_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getHeatingCurveSlopeCircuit1() {
		return this.getHeatingCurveSlopeCircuit1Channel().value();
	}
	
	/**
     * Set the heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPE_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setHeatingCurveSlopeCircuit1(int value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the heating curve slope setting, heating circuit 1.
     * See {@link ChannelId#HR1504_SLOPE_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHeatingCurveSlopeCircuit1(Integer value) throws OpenemsNamedException {
        this.getHeatingCurveSlopeCircuit1Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getComfortTempCircuit2Channel() {
        return this.channel(ChannelId.HR1505_COMFORT_TEMP_HC2);
    }

    /**
     * Get the comfort temperature setting, heating circuit 2. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getComfortTempCircuit2() {
		return this.getComfortTempCircuit2Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 2. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setComfortTempCircuit2(int value) throws OpenemsNamedException {
		this.getComfortTempCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting, heating circuit 2. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setComfortTempCircuit2(Integer value) throws OpenemsNamedException {
        this.getComfortTempCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1506_ECO_TEMP_HC2}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getEcoTempCircuit2Channel() {
        return this.channel(ChannelId.HR1506_ECO_TEMP_HC2);
    }

    /**
     * Get the ECO temperature setting, heating circuit 2. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getEcoTempCircuit2() {
		return this.getEcoTempCircuit2Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 2. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setEcoTempCircuit2(int value) throws OpenemsNamedException {
		this.getEcoTempCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting, heating circuit 2. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setEcoTempCircuit2(Integer value) throws OpenemsNamedException {
        this.getEcoTempCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1507_SLOPE_HC2}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHeatingCurveSlopeCircuit2Channel() {
        return this.channel(ChannelId.HR1507_SLOPE_HC2);
    }

    /**
     * Get the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPE_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getHeatingCurveSlopeCircuit2() {
		return this.getHeatingCurveSlopeCircuit2Channel().value();
	}
	
	/**
     * Set the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPE_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setHeatingCurveSlopeCircuit2(int value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the Heating curve slope setting, heating circuit 2.
     * See {@link ChannelId#HR1507_SLOPE_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHeatingCurveSlopeCircuit2(Integer value) throws OpenemsNamedException {
        this.getHeatingCurveSlopeCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1508_CONST_TEMP_MODE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getConstTempOperationModeChannel() {
        return this.channel(ChannelId.HR1508_CONST_TEMP_MODE);
    }

    /**
     * Get the constant temperature mode setting. A value of 0x9000 means disabled. When enabled, the value will be
     * a temperature value between 200 and 700. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getConstTempOperationMode() {
		return this.getConstTempOperationModeChannel().value();
	}
	
	/**
     * Set the constant temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700
     * enables this mode. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setConstTempOperationMode(Integer value) throws OpenemsNamedException {
		this.getConstTempOperationModeChannel().setNextWriteValue(value);
	}

    /**
     * Set the constant temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700
     * enables this mode. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setConstTempOperationMode(int value) throws OpenemsNamedException {
        this.getConstTempOperationModeChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getAuxHeaterActivationTempCircuitChannel() {
        return this.channel(ChannelId.HR1509_CIRCUIT_AUX_ACT_TEMP);
    }

    /**
     * Get the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getAuxHeaterActivationTempCircuit() {
		return this.getAuxHeaterActivationTempCircuitChannel().value();
	}
	
	/**
     * Set the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setAuxHeaterActivationTempCircuit(int value) throws OpenemsNamedException {
		this.getAuxHeaterActivationTempCircuitChannel().setNextWriteValue(value);
	}

    /**
     * Set the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setAuxHeaterActivationTempCircuit(Integer value) throws OpenemsNamedException {
        this.getAuxHeaterActivationTempCircuitChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getDomesticHotWaterComfortTempChannel() {
        return this.channel(ChannelId.HR1510_COMFORT_TEMP_WATER);
    }

    /**
     * Get the comfort temperature setting for domestic hot water. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getDomesticHotWaterComfortTemp() {
		return this.getDomesticHotWaterComfortTempChannel().value();
	}
	
	/**
     * Set the comfort temperature setting for domestic hot water. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setDomesticHotWaterComfortTemp(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterComfortTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting for domestic hot water. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setDomesticHotWaterComfortTemp(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterComfortTempChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1511_ECO_TEMP_WATER}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getDomesticHotWaterEcoTempChannel() {
        return this.channel(ChannelId.HR1511_ECO_TEMP_WATER);
    }

    /**
     * Get the ECO temperature setting for domestic hot water. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getDomesticHotWaterEcoTemp() {
		return this.getDomesticHotWaterEcoTempChannel().value();
	}
	
	/**
     * Set the ECO temperature setting for domestic hot water. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setDomesticHotWaterEcoTemp(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterEcoTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting for domestic hot water. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setDomesticHotWaterEcoTemp(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterEcoTempChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1512_WATER_STAGES}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getDomesticHotWaterStagesChannel() {
        return this.channel(ChannelId.HR1512_WATER_STAGES);
    }
    
    /**
     * Get the number of stages for domestic hot water.
	 * See {@link ChannelId#HR1512_WATER_STAGES}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getDomesticHotWaterStages() {
		return this.getDomesticHotWaterStagesChannel().value();
	}
	
	/**
     * Set the Number of stages for domestic hot water.
	 * See {@link ChannelId#HR1512_WATER_STAGES}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setDomesticHotWaterStages(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterStagesChannel().setNextWriteValue(value);
	}

    /**
     * Set the Number of stages for domestic hot water.
     * See {@link ChannelId#HR1512_WATER_STAGES}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setDomesticHotWaterStages(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterStagesChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getAuxHeaterActivationTempWaterChannel() {
        return this.channel(ChannelId.HR1513_WATER_AUX_ACT_TEMP);
    }
    
    /**
     * Get the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getAuxHeaterActivationTempWater() {
		return this.getAuxHeaterActivationTempWaterChannel().value();
	}
	
	/**
     * Set the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setAuxHeaterActivationTempWater(int value) throws OpenemsNamedException {
		this.getAuxHeaterActivationTempWaterChannel().setNextWriteValue(value);
	}

    /**
     * Set the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setAuxHeaterActivationTempWater(Integer value) throws OpenemsNamedException {
        this.getAuxHeaterActivationTempWaterChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSurfaceCoolingFlowTempChannel() {
        return this.channel(ChannelId.HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT);
    }
    
    /**
     * Get the flow temperature set point, surface cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getSurfaceCoolingFlowTemp() {
		return this.getSurfaceCoolingFlowTempChannel().value();
	}
	
	/**
     * Set the flow temperature set point, surface cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSurfaceCoolingFlowTemp(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingFlowTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature set point, surface cooling. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSurfaceCoolingFlowTemp(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingFlowTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSurfaceCoolingFlowTempHysteresisChannel() {
        return this.channel(ChannelId.HR1515_SURFACE_COOLING_FLOW_TEMP_HYST);
    }
    
    /**
     * Get the flow temperature hysteresis, surface cooling. Unit is decimal degree Kelvin.
	 * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getSurfaceCoolingFlowTempHysteresis() {
		return this.getSurfaceCoolingFlowTempHysteresisChannel().value();
	}
	
	/**
     * Set the flow temperature hysteresis, surface cooling. Unit is decimal degree Kelvin.
	 * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSurfaceCoolingFlowTempHysteresis(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature hysteresis, surface cooling. Unit is decimal degree Kelvin.
     * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSurfaceCoolingFlowTempHysteresis(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSurfaceCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT);
    }
    
    /**
     * Get the room temperature set point, surface cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getSurfaceCoolingRoomTemp() {
		return this.getSurfaceCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the room temperature set point, surface cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSurfaceCoolingRoomTemp(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingRoomTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the room temperature set point, surface cooling. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setSurfaceCoolingRoomTemp(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingRoomTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getVentilationCoolingFlowTempChannel() {
        return this.channel(ChannelId.HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT);
    }
    
    /**
     * Get the flow temperature set point, ventilation cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getVentilationCoolingFlowTemp() {
		return this.getVentilationCoolingFlowTempChannel().value();
	}
	
	/**
     * Set the flow temperature set point, ventilation cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setVentilationCoolingFlowTemp(int value) throws OpenemsNamedException {
		this.getVentilationCoolingFlowTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature set point, ventilation cooling. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setVentilationCoolingFlowTemp(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingFlowTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getVentilationCoolingFlowTempHysteresisChannel() {
        return this.channel(ChannelId.HR1518_VENT_COOLING_FLOW_TEMP_HYST);
    }

    /**
     * Get the flow temperature hysteresis, ventilation cooling. Unit is decimal degree Kelvin.
	 * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getVentilationCoolingFlowTempHysteresis() {
		return this.getVentilationCoolingFlowTempHysteresisChannel().value();
	}
	
	/**
     * Set the flow temperature hysteresis, ventilation cooling. Unit is decimal degree Kelvin.
	 * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setVentilationCoolingFlowTempHysteresis(int value) throws OpenemsNamedException {
		this.getVentilationCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature hysteresis, ventilation cooling. Unit is decimal degree Kelvin.
     * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setVentilationCoolingFlowTempHysteresis(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getVentilationCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT);
    }

    /**
     * Get the room temperature set point, ventilation cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getVentilationCoolingRoomTemp() {
		return this.getVentilationCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the room temperature set point, ventilation cooling. Unit is decimal degree Celsius.
	 * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setVentilationCoolingRoomTemp(int value) throws OpenemsNamedException {
		this.getVentilationCoolingRoomTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the room temperature set point, ventilation cooling. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setVentilationCoolingRoomTemp(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingRoomTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR4001_SGREADY_ONOFF}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getSgReadyOnOffChannel() {
        return this.channel(ChannelId.HR4001_SGREADY_ONOFF);
    }

    /**
     * Get on/off state of SG-Ready capabilities. True = on, false = off.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Boolean> getSgReadyOnOff() {
		return this.getSgReadyOnOffChannel().value();
	}
	
	/**
     * Turn on/off SG-Ready mode. True = on, false = off.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSgReadyOnOff(Boolean value) throws OpenemsNamedException {
		this.getSgReadyOnOffChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR4002_SGREADY_INPUT1}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getSgReadyInput1Channel() {
        return this.channel(ChannelId.HR4002_SGREADY_INPUT1);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR4003_SGREADY_INPUT2}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getSgReadyInput2Channel() {
        return this.channel(ChannelId.HR4003_SGREADY_INPUT2);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Gets the status message.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getStatusMessage() {
        return this.getStatusMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#STATUS_MESSAGE} Channel.
     *
     * @param value the next value
     */
    default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }

}
