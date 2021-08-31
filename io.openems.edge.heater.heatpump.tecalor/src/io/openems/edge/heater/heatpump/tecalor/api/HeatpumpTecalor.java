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
        IR507_OUTSIDE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR508_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature, if software version is WPM 3i.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR509_TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature, if software version is WPMsystem and WPM 3.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR510_TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature, independent of software version.
         * (Mapped from the software dependent channels)
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        TEMP_HC1_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR511_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 setpoint temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR512_TEMP_HC2_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Flow temperature heat pump.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR513_FLOW_TEMP_HEAT_PUMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Flow temperature auxiliary heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR514_FLOW_TEMP_AUX(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        //IR515_FLOW_TEMPERATURE -> Heater FLOW_TEMPERATURE

        //IR516_RETURN_TEMPERATURE -> Heater RETURN_TEMPERATURE

        /**
         * Constant temperature setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR517_CONST_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR518_STORAGE_TANK_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Storage tank temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR519_STORAGE_TANK_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

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
        IR522_WATER_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Domestic hot water temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR523_WATER_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Ventilation cooling temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR524_VENT_COOLING_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Ventilation cooling temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR525_VENT_COOLING_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Surface cooling temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR526_SURFACE_COOLING_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Surface cooling temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR527_SURFACE_COOLING_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Status bits.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2501_STATUSBITS(Doc.of(OpenemsType.INTEGER)),

        /**
         * Electric supplier block/release (EVU Freigabe). True = release.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2502_ELSUP_BLOCK_RELEASE(Doc.of(OpenemsType.BOOLEAN)),

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
         *      <li> State 1: Die Anlage darf nicht starten. Nur der Frostschutz wird gewährleistet.
         *      <li> State 2: Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemäß BI der angeschlossenen Wärmepumpe.
         *      <li> State 3: Forcierter Betrieb der Anlage mit erhöhten Werten für Heiz- und/oder Warmwassertemperatur.
         *      <li> State 4: Sofortige Ansteuerung der Maximalwerte für Heiz- und Warmwassertemperatur.
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
        HR1502_COMFORT_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1503_ECO_TEMP_HC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

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
        HR1505_COMFORT_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1506_ECO_TEMP_HC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

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
        HR1508_CONST_TEMP_MODE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Auxiliary heater activation temperature, heating circuit (Bivalenztemperatur). Below this temperature the
         * auxiliary heater will activate, depending on heat demand.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1509_CIRCUIT_AUX_ACT_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, domestic hot water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1510_COMFORT_TEMP_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, domestic hot water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1511_ECO_TEMP_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

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
        HR1513_WATER_AUX_ACT_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature setpoint, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature hysteresis, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1515_SURFACE_COOLING_FLOW_TEMP_HYST(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature setpoint, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature setpoint, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Flow temperature hysteresis, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1518_VENT_COOLING_FLOW_TEMP_HYST(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature setpoint, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

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
        HR4003_SGREADY_INPUT2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
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
    public default IntegerReadChannel getOutsideTempChannel() {
        return this.channel(ChannelId.IR507_OUTSIDE_TEMP);
    }

    /**
     * Gets the outside temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOutsideTemp() {
        return this.getOutsideTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR508_TEMP_HC1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit1TempChannel() {
        return this.channel(ChannelId.IR508_TEMP_HC1);
    }

    /**
     * Gets the heating circuit 1 temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit1Temp() {
        return this.getCircuit1TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#TEMP_HC1_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit1SetpointTempChannel() {
        return this.channel(ChannelId.TEMP_HC1_SETPOINT);
    }

    /**
     * Gets the heating circuit 1 setpoint temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit1SetpointTemp() {
        return this.getCircuit1TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR511_TEMP_HC2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit2TempChannel() {
        return this.channel(ChannelId.IR511_TEMP_HC2);
    }

    /**
     * Gets the heating circuit 2 temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit2Temp() {
        return this.getCircuit2TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR512_TEMP_HC2_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit2SetpointTempChannel() {
        return this.channel(ChannelId.IR512_TEMP_HC2_SETPOINT);
    }

    /**
     * Gets the heating circuit 2 setpoint temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit2SetpointTemp() {
        return this.getCircuit2SetpointTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR513_FLOW_TEMP_HEAT_PUMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getFlowTempHeatPumpChannel() {
        return this.channel(ChannelId.IR513_FLOW_TEMP_HEAT_PUMP);
    }

    /**
     * Gets the flow temperature heat pump. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getFlowTempHeatPump() {
        return this.getFlowTempHeatPumpChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR514_FLOW_TEMP_AUX}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getFlowTempAuxHeaterChannel() {
        return this.channel(ChannelId.IR514_FLOW_TEMP_AUX);
    }

    /**
     * Gets the flow temperature auxiliary heater. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getFlowTempAuxHeater() {
        return this.getFlowTempAuxHeaterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR517_CONST_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConstTempSetpointChannel() {
        return this.channel(ChannelId.IR517_CONST_TEMP_SETPOINT);
    }

    /**
     * Gets the constant temperature setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConstTempSetpoint() {
        return this.getConstTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR518_STORAGE_TANK_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStorageTankTempChannel() {
        return this.channel(ChannelId.IR518_STORAGE_TANK_TEMP);
    }

    /**
     * Gets the storage tank temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStorageTankTemp() {
        return this.getStorageTankTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR519_STORAGE_TANK_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStorageTankTempSetpointChannel() {
        return this.channel(ChannelId.IR519_STORAGE_TANK_TEMP_SETPOINT);
    }

    /**
     * Gets the storage tank temperature setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStorageTankTempSetpoint() {
        return this.getStorageTankTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR520_CIRCUIT_PRESSURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatingCircuitPressureChannel() {
        return this.channel(ChannelId.IR520_CIRCUIT_PRESSURE);
    }

    /**
     * Gets the heating circuit pressure. Unit is centi bar (bar e-2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatingCircuitPressure() {
        return this.getHeatingCircuitPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR521_CIRCUIT_CURRENT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatingCircuitCurrentChannel() {
        return this.channel(ChannelId.IR521_CIRCUIT_CURRENT);
    }

    /**
     * Gets the heating circuit current. Unit is decimal liters per minute (l/min e-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatingCircuitCurrent() {
        return this.getHeatingCircuitCurrentChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR522_WATER_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getDomesticHotWaterTempChannel() {
        return this.channel(ChannelId.IR522_WATER_TEMP);
    }

    /**
     * Gets the domestic hot water temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getDomesticHotWaterTemp() {
        return this.getDomesticHotWaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR523_WATER_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getDomesticHotWaterTempSetpointChannel() {
        return this.channel(ChannelId.IR523_WATER_TEMP_SETPOINT);
    }

    /**
     * Gets the domestic hot water temperature setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getDomesticHotWaterTempSetpoint() {
        return this.getDomesticHotWaterTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR524_VENT_COOLING_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVentilationCoolingTempChannel() {
        return this.channel(ChannelId.IR524_VENT_COOLING_TEMP);
    }

    /**
     * Gets the ventilation cooling temperature. Unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVentilationCoolingTemp() {
        return this.getVentilationCoolingTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR525_VENT_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVentilationCoolingTempSetpointChannel() {
        return this.channel(ChannelId.IR525_VENT_COOLING_TEMP_SETPOINT);
    }

    /**
     * Gets the ventilation cooling temperature setpoint. Unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVentilationCoolingTempSetpoint() {
        return this.getVentilationCoolingTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR526_SURFACE_COOLING_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSurfaceCoolingTempChannel() {
        return this.channel(ChannelId.IR526_SURFACE_COOLING_TEMP);
    }

    /**
     * Gets the surface cooling temperature. Unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSurfaceCoolingTemp() {
        return this.getSurfaceCoolingTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR527_SURFACE_COOLING_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSurfaceCoolingTempSetpointChannel() {
        return this.channel(ChannelId.IR527_SURFACE_COOLING_TEMP_SETPOINT);
    }

    /**
     * Gets the surface cooling temperature setpoint. Unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSurfaceCoolingTempSetpoint() {
        return this.getSurfaceCoolingTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2501_STATUSBITS}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusBitsChannel() {
        return this.channel(ChannelId.IR2501_STATUSBITS);
    }

    /**
     * Gets the status bits.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusBits() {
        return this.getStatusBitsChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2502_ELSUP_BLOCK_RELEASE}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getElSupBlockReleaseChannel() {
        return this.channel(ChannelId.IR2502_ELSUP_BLOCK_RELEASE);
    }

    /**
     * Gets the electric supplier block/release status (EVU Freigabe). True = release.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getElSupBlockRelease() {
        return this.getElSupBlockReleaseChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2504_ERRORSTATUS}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getErrorStatusChannel() {
        return this.channel(ChannelId.IR2504_ERRORSTATUS);
    }

    /**
     * Gets the error status. False for no error.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getErrorStatus() {
        return this.getErrorStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2505_BUSSTATUS}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBusStatusChannel() {
        return this.channel(ChannelId.IR2505_BUSSTATUS);
    }

    /**
     * Gets the BUS status.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBusStatus() {
        return this.getBusStatusChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2506_DEFROST}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getDefrostActiveChannel() {
        return this.channel(ChannelId.IR2506_DEFROST);
    }

    /**
     * Defrost active.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getDefrostActive() {
        return this.getDefrostActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR2507_ERROR_CODE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.IR2507_ERROR_CODE);
    }

    /**
     * Gets the error code.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3501_HEATPRODUCED_CIRCUIT_DAILY}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatCircuitDailyChannel() {
        return this.channel(ChannelId.IR3501_HEATPRODUCED_CIRCUIT_DAILY);
    }

    /**
     * Produced heat for heating circuits, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatCircuitDaily() {
        return this.getProducedHeatCircuitDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_CIRCUIT_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatCircuitTotalChannel() {
        return this.channel(ChannelId.HEATPRODUCED_CIRCUIT_SUM);
    }

    /**
     * Produced heat for heating circuits total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatCircuitTotal() {
        return this.getProducedHeatCircuitTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3504_HEATPRODUCED_WATER_DAILY}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatWaterDailyChannel() {
        return this.channel(ChannelId.IR3504_HEATPRODUCED_WATER_DAILY);
    }

    /**
     * Produced heat for domestic hot water, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatWaterDaily() {
        return this.getProducedHeatWaterDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_WATER_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatWaterTotalChannel() {
        return this.channel(ChannelId.HEATPRODUCED_WATER_SUM);
    }

    /**
     * Produced heat for domestic hot water total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatWaterTotal() {
        return this.getProducedHeatWaterTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_AUX_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatCircuitTotalAuxChannel() {
        return this.channel(ChannelId.HEATPRODUCED_AUX_SUM);
    }

    /**
     * Produced heat for heating circuits total, auxiliary heater.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatCircuitTotalAux() {
        return this.getProducedHeatCircuitTotalAuxChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_WATER_AUX_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatWaterTotalAuxChannel() {
        return this.channel(ChannelId.HEATPRODUCED_WATER_AUX_SUM);
    }

    /**
     * Produced heat for domestic hot water total, auxiliary heater.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatWaterTotalAux() {
        return this.getProducedHeatWaterTotalAuxChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3511_CONSUMEDPOWER_CIRCUIT_DAILY}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerCircuitDailyChannel() {
        return this.channel(ChannelId.IR3511_CONSUMEDPOWER_CIRCUIT_DAILY);
    }

    /**
     * Consumed power for heating the heating circuits, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerCircuitDaily() {
        return this.getConsumedPowerCircuitDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_CIRCUIT_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerCircuitTotalChannel() {
        return this.channel(ChannelId.CONSUMEDPOWER_CIRCUIT_SUM);
    }

    /**
     * Consumed power for heating the heating circuits total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerCircuitTotal() {
        return this.getConsumedPowerCircuitTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3514_CONSUMEDPOWER_WATER_DAILY}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerWaterDailyChannel() {
        return this.channel(ChannelId.IR3514_CONSUMEDPOWER_WATER_DAILY);
    }

    /**
     * Consumed power for heating domestic hot water, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerWaterDaily() {
        return this.getConsumedPowerWaterDailyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_WATER_SUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerWaterTotalChannel() {
        return this.channel(ChannelId.CONSUMEDPOWER_WATER_SUM);
    }

    /**
     * Consumed power for heating domestic hot water total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerWaterTotal() {
        return this.getConsumedPowerWaterTotalChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR5001_SGREADY_OPERATINGMODE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSgReadyOperatingModeChannel() {
        return this.channel(ChannelId.IR5001_SGREADY_OPERATINGMODE);
    }

    /**
     * SG-Ready Operating mode, read. Mapped to SmartGridState of HeatpumpSmartGrid interface.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 1 ... 4
     *      <li> State 1: Die Anlage darf nicht starten. Nur der Frostschutz wird gewährleistet.
     *      <li> State 2: Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemäß BI der angeschlossenen Wärmepumpe.
     *      <li> State 3: Forcierter Betrieb der Anlage mit erhöhten Werten für Heiz- und/oder Warmwassertemperatur.
     *      <li> State 4: Sofortige Ansteuerung der Maximalwerte für Heiz- und Warmwassertemperatur.
     * </ul>
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSgReadyOperatingMode() {
        return this.getSgReadyOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR5002_CONTROLLER_MODEL_ID}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getControllerModelIdChannel() {
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
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getControllerModelId() {
        return this.getControllerModelIdChannel().value();
    }


    // Holding Registers. Read/Write.

    /**
     * Gets the Channel for {@link ChannelId#HR1501_OPERATING_MODE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getOperatingModeChannel() {
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
    public default Value<Integer> getOperatingMode() {
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
	public default void setOperatingMode(int value) throws OpenemsNamedException {
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
    public default void setOperatingMode(Integer value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getComfortTempCircuit1Channel() {
        return this.channel(ChannelId.HR1502_COMFORT_TEMP_HC1);
    }
    
    /**
     * Get the comfort temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getComfortTempCircuit1() {
		return this.getComfortTempCircuit1Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setComfortTempCircuit1(int value) throws OpenemsNamedException {
		this.getComfortTempCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting, heating circuit 1. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1502_COMFORT_TEMP_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setComfortTempCircuit1(Integer value) throws OpenemsNamedException {
        this.getComfortTempCircuit1Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1503_ECO_TEMP_HC1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getEcoTempCircuit1Channel() {
        return this.channel(ChannelId.HR1503_ECO_TEMP_HC1);
    }

    /**
     * Get the ECO temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getEcoTempCircuit1() {
		return this.getEcoTempCircuit1Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEcoTempCircuit1(int value) throws OpenemsNamedException {
		this.getEcoTempCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting, heating circuit 1. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1503_ECO_TEMP_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setEcoTempCircuit1(Integer value) throws OpenemsNamedException {
        this.getEcoTempCircuit1Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1504_SLOPE_HC1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveSlopeCircuit1Channel() {
        return this.channel(ChannelId.HR1504_SLOPE_HC1);
    }

    /**
     * Get the heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPE_HC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getHeatingCurveSlopeCircuit1() {
		return this.getHeatingCurveSlopeCircuit1Channel().value();
	}
	
	/**
     * Set the heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPE_HC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveSlopeCircuit1(int value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeCircuit1Channel().setNextWriteValue(value);
	}

    /**
     * Set the heating curve slope setting, heating circuit 1.
     * See {@link ChannelId#HR1504_SLOPE_HC1}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setHeatingCurveSlopeCircuit1(Integer value) throws OpenemsNamedException {
        this.getHeatingCurveSlopeCircuit1Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getComfortTempCircuit2Channel() {
        return this.channel(ChannelId.HR1505_COMFORT_TEMP_HC2);
    }

    /**
     * Get the comfort temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getComfortTempCircuit2() {
		return this.getComfortTempCircuit2Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setComfortTempCircuit2(int value) throws OpenemsNamedException {
		this.getComfortTempCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting, heating circuit 2. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1505_COMFORT_TEMP_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setComfortTempCircuit2(Integer value) throws OpenemsNamedException {
        this.getComfortTempCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1506_ECO_TEMP_HC2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getEcoTempCircuit2Channel() {
        return this.channel(ChannelId.HR1506_ECO_TEMP_HC2);
    }

    /**
     * Get the ECO temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getEcoTempCircuit2() {
		return this.getEcoTempCircuit2Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEcoTempCircuit2(int value) throws OpenemsNamedException {
		this.getEcoTempCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting, heating circuit 2. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1506_ECO_TEMP_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setEcoTempCircuit2(Integer value) throws OpenemsNamedException {
        this.getEcoTempCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1507_SLOPE_HC2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveSlopeCircuit2Channel() {
        return this.channel(ChannelId.HR1507_SLOPE_HC2);
    }

    /**
     * Get the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPE_HC2}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getHeatingCurveSlopeCircuit2() {
		return this.getHeatingCurveSlopeCircuit2Channel().value();
	}
	
	/**
     * Set the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPE_HC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveSlopeCircuit2(int value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeCircuit2Channel().setNextWriteValue(value);
	}

    /**
     * Set the Heating curve slope setting, heating circuit 2.
     * See {@link ChannelId#HR1507_SLOPE_HC2}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setHeatingCurveSlopeCircuit2(Integer value) throws OpenemsNamedException {
        this.getHeatingCurveSlopeCircuit2Channel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1508_CONST_TEMP_MODE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getConstTempOperationModeChannel() {
        return this.channel(ChannelId.HR1508_CONST_TEMP_MODE);
    }

    /**
     * Get the constant temperature mode setting. A value of 0x9000 means disabled. When enabled, the value will be
     * a temperature value between 200 and 700. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getConstTempOperationMode() {
		return this.getConstTempOperationModeChannel().value();
	}
	
	/**
     * Set the constant temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700
     * enables this mode. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setConstTempOperationMode(Integer value) throws OpenemsNamedException {
		this.getConstTempOperationModeChannel().setNextWriteValue(value);
	}

    /**
     * Set the constant temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700
     * enables this mode. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1508_CONST_TEMP_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setConstTempOperationMode(int value) throws OpenemsNamedException {
        this.getConstTempOperationModeChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getAuxHeaterActivationTempCircuitChannel() {
        return this.channel(ChannelId.HR1509_CIRCUIT_AUX_ACT_TEMP);
    }

    /**
     * Get the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getAuxHeaterActivationTempCircuit() {
		return this.getAuxHeaterActivationTempCircuitChannel().value();
	}
	
	/**
     * Set the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setAuxHeaterActivationTempCircuit(int value) throws OpenemsNamedException {
		this.getAuxHeaterActivationTempCircuitChannel().setNextWriteValue(value);
	}

    /**
     * Set the auxiliary heater activation temperature of the heating circuit. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1509_CIRCUIT_AUX_ACT_TEMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setAuxHeaterActivationTempCircuit(Integer value) throws OpenemsNamedException {
        this.getAuxHeaterActivationTempCircuitChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getDomesticHotWaterComfortTempChannel() {
        return this.channel(ChannelId.HR1510_COMFORT_TEMP_WATER);
    }

    /**
     * Get the comfort temperature setting for domestic hot water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getDomesticHotWaterComfortTemp() {
		return this.getDomesticHotWaterComfortTempChannel().value();
	}
	
	/**
     * Set the comfort temperature setting for domestic hot water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterComfortTemp(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterComfortTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the comfort temperature setting for domestic hot water. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1510_COMFORT_TEMP_WATER}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setDomesticHotWaterComfortTemp(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterComfortTempChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1511_ECO_TEMP_WATER}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getDomesticHotWaterEcoTempChannel() {
        return this.channel(ChannelId.HR1511_ECO_TEMP_WATER);
    }

    /**
     * Get the ECO temperature setting for domestic hot water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getDomesticHotWaterEcoTemp() {
		return this.getDomesticHotWaterEcoTempChannel().value();
	}
	
	/**
     * Set the ECO temperature setting for domestic hot water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterEcoTemp(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterEcoTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the ECO temperature setting for domestic hot water. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1511_ECO_TEMP_WATER}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setDomesticHotWaterEcoTemp(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterEcoTempChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1512_WATER_STAGES}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getDomesticHotWaterStagesChannel() {
        return this.channel(ChannelId.HR1512_WATER_STAGES);
    }
    
    /**
     * Get the number of stages for domestic hot water.
	 * See {@link ChannelId#HR1512_WATER_STAGES}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getDomesticHotWaterStages() {
		return this.getDomesticHotWaterStagesChannel().value();
	}
	
	/**
     * Set the Number of stages for domestic hot water.
	 * See {@link ChannelId#HR1512_WATER_STAGES}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterStages(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterStagesChannel().setNextWriteValue(value);
	}

    /**
     * Set the Number of stages for domestic hot water.
     * See {@link ChannelId#HR1512_WATER_STAGES}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setDomesticHotWaterStages(Integer value) throws OpenemsNamedException {
        this.getDomesticHotWaterStagesChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getAuxHeaterActivationTempWaterChannel() {
        return this.channel(ChannelId.HR1513_WATER_AUX_ACT_TEMP);
    }
    
    /**
     * Get the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getAuxHeaterActivationTempWater() {
		return this.getAuxHeaterActivationTempWaterChannel().value();
	}
	
	/**
     * Set the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setAuxHeaterActivationTempWater(int value) throws OpenemsNamedException {
		this.getAuxHeaterActivationTempWaterChannel().setNextWriteValue(value);
	}

    /**
     * Set the auxiliary heater activation temperature of the domestic hot water. Below this temperature the auxiliary
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1513_WATER_AUX_ACT_TEMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setAuxHeaterActivationTempWater(Integer value) throws OpenemsNamedException {
        this.getAuxHeaterActivationTempWaterChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingFlowTempChannel() {
        return this.channel(ChannelId.HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT);
    }
    
    /**
     * Get the flow temperature setpoint, surface cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getSurfaceCoolingFlowTemp() {
		return this.getSurfaceCoolingFlowTempChannel().value();
	}
	
	/**
     * Set the flow temperature setpoint, surface cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingFlowTemp(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingFlowTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature setpoint, surface cooling. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1514_SURFACE_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setSurfaceCoolingFlowTemp(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingFlowTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingFlowTempHysteresisChannel() {
        return this.channel(ChannelId.HR1515_SURFACE_COOLING_FLOW_TEMP_HYST);
    }
    
    /**
     * Get the flow temperature hysteresis, surface cooling. Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getSurfaceCoolingFlowTempHysteresis() {
		return this.getSurfaceCoolingFlowTempHysteresisChannel().value();
	}
	
	/**
     * Set the flow temperature hysteresis, surface cooling. Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingFlowTempHysteresis(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature hysteresis, surface cooling. Unit is dezidegree Kelvin.
     * See {@link ChannelId#HR1515_SURFACE_COOLING_FLOW_TEMP_HYST}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setSurfaceCoolingFlowTempHysteresis(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT);
    }
    
    /**
     * Get the room temperature setpoint, surface cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getSurfaceCoolingRoomTemp() {
		return this.getSurfaceCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the room temperature setpoint, surface cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingRoomTemp(int value) throws OpenemsNamedException {
		this.getSurfaceCoolingRoomTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the room temperature setpoint, surface cooling. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1516_SURFACE_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setSurfaceCoolingRoomTemp(Integer value) throws OpenemsNamedException {
        this.getSurfaceCoolingRoomTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingFlowTempChannel() {
        return this.channel(ChannelId.HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT);
    }
    
    /**
     * Get the flow temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getVentilationCoolingFlowTemp() {
		return this.getVentilationCoolingFlowTempChannel().value();
	}
	
	/**
     * Set the flow temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingFlowTemp(int value) throws OpenemsNamedException {
		this.getVentilationCoolingFlowTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1517_VENT_COOLING_FLOW_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setVentilationCoolingFlowTemp(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingFlowTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingFlowTempHysteresisChannel() {
        return this.channel(ChannelId.HR1518_VENT_COOLING_FLOW_TEMP_HYST);
    }

    /**
     * Get the flow temperature hysteresis, ventilation cooling. Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getVentilationCoolingFlowTempHysteresis() {
		return this.getVentilationCoolingFlowTempHysteresisChannel().value();
	}
	
	/**
     * Set the flow temperature hysteresis, ventilation cooling. Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingFlowTempHysteresis(int value) throws OpenemsNamedException {
		this.getVentilationCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
	}

    /**
     * Set the flow temperature hysteresis, ventilation cooling. Unit is dezidegree Kelvin.
     * See {@link ChannelId#HR1518_VENT_COOLING_FLOW_TEMP_HYST}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setVentilationCoolingFlowTempHysteresis(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingFlowTempHysteresisChannel().setNextWriteValue(value);
    }
    
    /**
     * Gets the Channel for {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT);
    }

    /**
     * Get the room temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getVentilationCoolingRoomTemp() {
		return this.getVentilationCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the room temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingRoomTemp(int value) throws OpenemsNamedException {
		this.getVentilationCoolingRoomTempChannel().setNextWriteValue(value);
	}

    /**
     * Set the room temperature setpoint, ventilation cooling. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR1519_VENT_COOLING_ROOM_TEMP_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setVentilationCoolingRoomTemp(Integer value) throws OpenemsNamedException {
        this.getVentilationCoolingRoomTempChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR4001_SGREADY_ONOFF}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyOnOffChannel() {
        return this.channel(ChannelId.HR4001_SGREADY_ONOFF);
    }

    /**
     * Get on/off state of SG-Ready capabilities. True = on, false = off.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getSgReadyOnOff() {
		return this.getSgReadyOnOffChannel().value();
	}
	
	/**
     * Turn on/off SG-Ready mode. True = on, false = off.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSgReadyOnOff(Boolean value) throws OpenemsNamedException {
		this.getSgReadyOnOffChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR4002_SGREADY_INPUT1}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyInput1Channel() {
        return this.channel(ChannelId.HR4002_SGREADY_INPUT1);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR4003_SGREADY_INPUT2}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyInput2Channel() {
        return this.channel(ChannelId.HR4003_SGREADY_INPUT2);
    }


}
