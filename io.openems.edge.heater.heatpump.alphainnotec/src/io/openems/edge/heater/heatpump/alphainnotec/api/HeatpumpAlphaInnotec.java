package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

/**
 * Channels for the Alpha Innotec heat pump.
 */

public interface HeatpumpAlphaInnotec extends HeatpumpSmartGrid {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Discrete Inputs (DI) 0 to 7, read only. 0 = Off, 1 = On. Represented as boolean.

        /**
         * El.Sup.bl., electric supplier block (EVU, Energie Versorger Unterbrechung).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_0_EL_SUP_BLOCK(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * El.Sup.bl.2, electric supplier block 2 (EVU2). Like El.Sup.bl., but triggered because of smart grid setting.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_1_EL_SUP_BLOCK_SG(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Pool thermostat (SWT, Schwimmbadthermostat).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_2_POOL_THERMOSTAT(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Compressor 1 (VD1, Verdichter 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_3_COMPRESSOR1(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Compressor 2 (VD2, Verdichter 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_4_COMPRESSOR2(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * 2nd heat generator 1 (ZWE1, zusaetzlicher Waermeerzeuger 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_5_AUX1(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * 2nd heat generator 2 (ZWE2, zusaetzlicher Waermeerzeuger 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_6_AUX2(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * 2nd heat generator 3 (ZWE3, zusaetzlicher Waermeerzeuger 3). Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_7_AUX3(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),


        // Input Registers (IR) 0 to 46, read only. They are 16 bit unsigned numbers unless stated otherwise.

        /**
         * Average temperature (Mitteltemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_0_AVERAGE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Flow temperature (Vorlauftemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        // IR_1_FLOW_TEMP - use FLOW_TEMPERATURE from heater interface

        /**
         * Return temperature (Ruecklauftemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        // IR_2_RETURN_TEMP - use RETURN_TEMPERATURE from heater interface

        /**
         * External return temperature (Ruecklauf extern).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_3_RETURN_TEMP_EXT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Domestic hot water temperature (Trinkwarmwassertemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_4_WATER_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Mixing circuit 1 flow temperature (Mischkreis 1 Vorlauf).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_5_FLOW_TEMP_MC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Mixing circuit 2 flow temperature (Mischkreis 2 Vorlauf).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_6_FLOW_TEMP_MC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Mixing circuit 3 flow temperature (Mischkreis 3 Vorlauf).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_7_FLOW_TEMP_MC3(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Hot gas temperature (Heissgastemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_8_HOT_GAS_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heat source inlet (Waermequelle Eintritt).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_9_HEAT_SOURCE_INLET_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Heat source outlet (Waermequelle Austritt).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_10_HEAT_SOURCE_OUTLET_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Room remote adjuster 1 (Raumfernversteller 1).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_11_ROOM_REMOTE_ADJ1_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Room remote adjuster 2 (Raumfernversteller 2). Optional, depends on heat pump model if
         * available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_12_ROOM_REMOTE_ADJ2_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Room remote adjuster 3 (Raumfernversteller 3). Optional, depends on heat pump model if
         * available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_13_ROOM_REMOTE_ADJ3_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Solar collector (Solarkollektor). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_14_SOLAR_COLLECTOR_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Solar collector storage tank (Solarspeicher). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_15_SOLAR_STORAGE_TANK_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * External energy source (Externe Energiequelle). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_16_EXT_ENERGY_SOURCE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Supply air temperature (Zulufttemperatur). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_17_SUPPLY_AIR_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Extract air temperature (Ablufttemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_18_EXTRACT_AIR_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Compressor intake temperature (Ansaugtemperatur Verdichter).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_19_COMPRESSOR_INTAKE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Evaporator intake temperature (Ansaugtemperatur Verdampfer).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_20_EVAPORATOR_INTAKE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Compressor heater temperature (Temperatur Verdichterheizung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_21_COMPRESSOR_HEATER_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Overheating (Ueberhitzung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Kelvin
         * </ul>
         */
        IR_22_OVERHEAT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * Overheating setpoint (Ueberhitzung Soll).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Kelvin
         * </ul>
         */
        IR_23_OVERHEAT_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN)),

        /**
         * RBE room temperature actual (RBE, Raumbedieneinheit Raumtemperatur Ist).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_24_RBE_ROOM_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * RBE room temperature setpoint (RBE, Raumbedieneinheit Raumtemperatur Soll).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_25_RBE_ROOM_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * High pressure (Druck HD, Hochdruck).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: centibar
         * </ul>
         */
        IR_26_HIGH_PRESSURE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Low pressure (Druck ND, Niederdruck).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: centibar
         * </ul>
         */
        IR_27_LOW_PRESSURE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Operating hours compressor 1 (Betriebsstunden VD1, Verdichter 1).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_28_HOURS_COMP1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours compressor 2 (Betriebsstunden VD2, Verdichter 2).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_29_HOURS_COMP2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours 2nd heat generator 1 (Betriebsstunden ZWE1, Zusaetzlicher Waermeerzeuger).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_30_HOURS_AUX1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours 2nd heat generator 2 (Betriebsstunden ZWE2, Zusaetzlicher Waermeerzeuger).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_31_HOURS_AUX2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours 2nd heat generator 3 (Betriebsstunden ZWE3, Zusaetzlicher Waermeerzeuger).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_32_HOURS_AUX3(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours heat pump (Betriebsstunden Waermepumpe).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_33_HOURS_HEAT_PUMP(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours room heating (Betriebsstunden Heizung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_34_HOURS_CIRCUIT_HEATING(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours domestic hot water heating (Betriebsstunden Trinkwarmwasser).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_35_HOURS_WATER_HEATING(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours swimming pool or solar panels (Betriebsstunden SWoPV, Schwimmbad oder Photovoltaik).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_36_HOURS_POOL_SOLAR(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Current system status of the heat pump (Anlagenstatus).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 7
         *      <li> State -1: Undefined
         *      <li> State 0: Room heating (Heizbetrieb)
         *      <li> State 1: Domestic hot water heating (Trinkwarmwasser)
         *      <li> State 2: Swimming pool heating (Schwimmbad)
         *      <li> State 3: Electric supplier block (EVU-Sperre)
         *      <li> State 4: Defrost (Abtauen)
         *      <li> State 5: Off
         *      <li> State 6: External energy source (Externe Energiequelle)
         *      <li> State 7: Cooling (Kuehlung)
         * </ul>
         */
        IR_37_STATUS(Doc.of(SystemStatus.values())),

        /**
         * Heat quantity room heating (Waermemenge Heizung). 32 bit unsigned doubleword. IR 38 is high, IR 39 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_38_ENERGY_CIRCUIT_HEATING(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat quantity domestic hot water heating (Waermemenge Trinkwarmwasser). 32 bit unsigned doubleword.
         * IR 40 is high, IR 41 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_40_ENERGY_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat quantity swimming pool heating (Waermemenge Schwimmbad). 32 bit unsigned doubleword. IR 42 is high,
         * IR 43 is low.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_42_ENERGY_POOL(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat quantity total (Waermemenge gesamt). 32 bit unsigned doubleword. IR 44 is high, IR 45 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_44_ENERGY_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Error buffer. Only displays current error.
         * <ul>
         *     <li> Type: Integer
         * </ul>
         */
        IR_46_ERROR(Doc.of(OpenemsType.INTEGER)),


        // Coils 0 to 13, read/write. When reading, 0 = Off, 1 = On. When writing, 0 = automatic, 1 = force on.
        // Represented as boolean.

        /**
         * Error reset. Reset current error message.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_0_ERRORRESET(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        // Coil 1 not used

        /**
         * Heat. sys. pump (HUP, Heizung + Brauchwasser Umwaelzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_2_HUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * VEN (Ventilator), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_3_VEN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * CP (ZUP, Zusatz-Umwaelzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_4_ZUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * DHW pump (BUP, Trinkwarmwasser-Umwaelzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_5_BUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heats.-pump (BOSUP, Brunnen oder Sole-Umwaelzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_6_BOSUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Circulation pump (ZIP, Zirkulationspumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_7_ZIP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * CP2 (FUP2, Fussbodenheizungs-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_8_FUP2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * CP3 (FUP3, Fussbodenheizungs-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_9_FUP3(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Solar pump (SLP, Solar-Ladepumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_10_SLP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pool pump (SUP, Schwimmbad-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_11_SUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * VSK (Bypassklappe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_12_VSK(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * FRH (Schuetz Defrostheizung), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_13_FRH(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),


        // Holding Registers (HR) 0 to 23, read/write. They are 16 bit unsigned numbers unless stated otherwise.

        /**
         * Outdoor temperature. Signed 16 bit. Minimum -200, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_0_OUTSIDETEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Return temperature set point. Minimum 150, maximum 800. (<- Aus Handbuch. Minimum Wert sicher falsch, schon
         * Wert 50 ausgelesen.)
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_1_RETURN_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mixing circuit 1 (Mischkreis 1) flow temperature setpoint. Minimum 150, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_2_FLOW_TEMP_SETPOINT_MC1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mixing circuit 2 (Mischkreis 2) flow temperature setpoint. Minimum 150, maximum 800.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_3_FLOW_TEMP_SETPOINT_MC2(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mixing circuit 3 (Mischkreis 3) flow temperature setpoint. Minimum 150, maximum 800.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_4_FLOW_TEMP_SETPOINT_MC3(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Domestic hot water (Trinkwarmwasser) temperature desired value. Minimum 150, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_5_WATER_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Block / release heat pump (Sperre / Freigabe Waermepumpe).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 2
         *      <li> State -1: Undefined
         *      <li> State 0: Block heat pump (Sperre)
         *      <li> State 1: Release 1 compressor (Freigabe 1 Verdichter)
         *      <li> State 2: Release 2 compressors (Freigabe 2 Verdichter)
         * </ul>
         */
        HR_6_BLOCK_RELEASE(Doc.of(BlockRelease.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation room heating (Betriebsart Heizung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 4
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
         *      <li> State 2: No late night throttling (Party)
         *      <li> State 3: Holidays, full time throttling (Ferien)
         *      <li> State 4: Off
         * </ul>
         */
        HR_7_CIRCUIT_HEATING_OPERATION_MODE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation domestic hot water (Betriebsart Trinkwarmwasser).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 4
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
         *      <li> State 2: No late night throttling (Party)
         *      <li> State 3: Holidays, full time throttling (Ferien)
         *      <li> State 4: Off
         * </ul>
         */
        HR_8_WATER_OPERATION_MODE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation mixing circuit 2 (Betriebsart Mischkreis 2).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 4
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
         *      <li> State 2: No late night throttling (Party)
         *      <li> State 3: Holidays, full time throttling (Ferien)
         *      <li> State 4: Off
         * </ul>
         */
        HR_9_MC2_OPERATION_MODE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation mixing circuit 3 (Betriebsart Mischkreis 3).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 4
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
         *      <li> State 2: No late night throttling (Party)
         *      <li> State 3: Holidays, full time throttling (Ferien)
         *      <li> State 4: Off
         * </ul>
         */
        HR_10_MC3_OPERATION_MODE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation cooling (Betriebsart Kuehlung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 1
         *      <li> State -1: Undefined
         *      <li> State 0: Off
         *      <li> State 1: Automatic
         * </ul>
         */
        HR_11_COOLING_OPERATION_MODE(Doc.of(CoolingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation ventilation (Betriebsart Lueftung).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 3
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: No late night throttling (Party)
         *      <li> State 2: Holidays, full time throttling (Ferien)
         *      <li> State 3: Off
         * </ul>
         */
        HR_12_VENTILATION_OPERATION_MODE(Doc.of(VentilationMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Mode of operation swimming pool heating (Betriebsart Schwimmbad).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 4
         *      <li> State -1: Undefined
         *      <li> State 0: Automatic
         *      <li> State 1: Value not in use (Wert nicht benutzt)
         *      <li> State 2: No late night throttling (Party)
         *      <li> State 3: Holidays, full time throttling (Ferien)
         *      <li> State 4: Off
         * </ul>
         */
        HR_13_POOL_OPERATION_MODE(Doc.of(PoolMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Smart Grid. Do not use this channel, use SMART_GRID_STATE in HeatpumpSmartGrid interface.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 3
         *      <li> State 0: Electric supplier block (EVU-Sperre)
         *      <li> State 1: Smart Grid Low
         *      <li> State 2: Standard
         *      <li> State 3: Smart Grid High
         * </ul>
         */
        HR_14_SMART_GRID(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve room heating end point (Heizkurve Heizung Endpunkt). Minimum 200, maximum 700.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_15_CURVE_CIRCUIT_HEATING_END_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve room heating parallel shift (Heizkurve Heizung Parallelverschiebung). Minimum 50, maximum 350.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_16_CURVE_CIRCUIT_HEATING_SHIFT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt). Minimum 200, maximum 700.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_17_CURVE_MC1_END_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 1 parallel shift (Heizkurve Mischkreis 1 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_18_CURVE_MC1_SHIFT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt). Minimum 200, maximum 700.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_19_CURVE_MC2_END_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 2 parallel shift (Heizkurve Mischkreis 2 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_20_CURVE_MC2_SHIFT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt). Minimum 200, maximum 700.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_21_CURVE_MC3_END_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve mixing circuit 3 parallel shift (Heizkurve Mischkreis 3 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_22_CURVE_MC3_SHIFT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Temperature +-. Minimum -50, maximum 50. Signed 16 bit number.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_23_TEMP_PM(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Discrete Inputs

    /**
     * Gets the Channel for {@link ChannelId#DI_0_EL_SUP_BLOCK}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvuActiveChannel() {
        return this.channel(ChannelId.DI_0_EL_SUP_BLOCK);
    }
    
    /**
     * El.Sup.bl., electric supplier block (EVU, Energie Versorger Unterbrechung).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvuActive() {
        return this.getEvuActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_1_EL_SUP_BLOCK_SG}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvu2ActiveChannel() {
        return this.channel(ChannelId.DI_1_EL_SUP_BLOCK_SG);
    }
    
    /**
     * El.Sup.bl.2, electric supplier block 2 (EVU2). Like El.Sup.bl., but triggered because of smart grid setting.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvu2Active() {
        return this.getEvu2ActiveChannel().value();
    }
    
    /**
     * Gets the Channel for {@link ChannelId#DI_2_POOL_THERMOSTAT}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getSwtActiveChannel() {
        return this.channel(ChannelId.DI_2_POOL_THERMOSTAT);
    }
    
    /**
     * Pool thermostat (SWT, Schwimmbadthermostat).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getSwtActive() {
        return this.getSwtActiveChannel().value();
    }
    
    /**
     * Gets the Channel for {@link ChannelId#DI_3_COMPRESSOR1}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getVD1activeChannel() {
        return this.channel(ChannelId.DI_3_COMPRESSOR1);
    }
    
    /**
     * Compressor 1 (VD1, Verdichter 1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getVD1active() {
        return this.getVD1activeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_4_COMPRESSOR2}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getVD2activeChannel() {
        return this.channel(ChannelId.DI_4_COMPRESSOR2);
    }
    
    /**
     * Compressor 2 (VD2, Verdichter 2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getVD2active() {
        return this.getVD2activeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_5_AUX1}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe1ActiveChannel() {
        return this.channel(ChannelId.DI_5_AUX1);
    }
    
    /**
     * 2nd heat generator 1 (ZWE1, zusaetzlicher Waermeerzeuger 1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe1Active() {
        return this.getZwe1ActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_6_AUX2}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe2ActiveChannel() {
        return this.channel(ChannelId.DI_6_AUX2);
    }
    
    /**
     * 2nd heat generator 2 (ZWE2, zusaetzlicher Waermeerzeuger 2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe2Active() {
        return this.getZwe2ActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_7_AUX3}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe3ActiveChannel() {
        return this.channel(ChannelId.DI_7_AUX3);
    }
    
    /**
     * 2nd heat generator 3 (ZWE3, zusaetzlicher Waermeerzeuger 3). Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe3Active() {
        return this.getZwe3ActiveChannel().value();
    }


    // Input Registers

    /**
     * Gets the Channel for {@link ChannelId#IR_0_AVERAGE_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAverageTempChannel() {
        return this.channel(ChannelId.IR_0_AVERAGE_TEMP);
    }
    
    /**
     * Gets the average temperature (Mitteltemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAverageTemp() {
        return this.getAverageTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_3_RETURN_TEMP_EXT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getExternalReturnTempChannel() {
        return this.channel(ChannelId.IR_3_RETURN_TEMP_EXT);
    }
    
    /**
     * Gets the external return temperature (Ruecklauf extern), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getExternalReturnTemp() {
        return this.getExternalReturnTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_4_WATER_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getDomesticHotWaterTempChannel() {
        return this.channel(ChannelId.IR_4_WATER_TEMP);
    }
    
    /**
     * Gets the domestic hot water temperature (Trinkwarmwassertemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getDomesticHotWaterTemp() {
        return this.getDomesticHotWaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_5_FLOW_TEMP_MC1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit1FlowTempChannel() {
        return this.channel(ChannelId.IR_5_FLOW_TEMP_MC1);
    }
    
    /**
     * Get the mixing circuit 1 flow temperature (Mischkreis 1 Vorlauf), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit1FlowTemp() {
        return this.getCircuit1FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_6_FLOW_TEMP_MC2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit2FlowTempChannel() {
        return this.channel(ChannelId.IR_6_FLOW_TEMP_MC2);
    }
    
    /**
     * Get the mixing circuit 2 flow temperature (Mischkreis 2 Vorlauf), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit2FlowTemp() {
        return this.getCircuit2FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_7_FLOW_TEMP_MC3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit3FlowTempChannel() {
        return this.channel(ChannelId.IR_7_FLOW_TEMP_MC3);
    }
    
    /**
     * Get the mixing circuit 3 flow temperature (Mischkreis 3 Vorlauf), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit3FlowTemp() {
        return this.getCircuit3FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_8_HOT_GAS_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHotGasTempChannel() {
        return this.channel(ChannelId.IR_8_HOT_GAS_TEMP);
    }
    
    /**
     * Gets the hot gas temperature (Heissgastemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHotGasTemp() {
        return this.getHotGasTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_9_HEAT_SOURCE_INLET_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatSourceInletTempChannel() {
        return this.channel(ChannelId.IR_9_HEAT_SOURCE_INLET_TEMP);
    }
    
    /**
     * Gets the heat source inlet temperature (Waermequelle Eintritt), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatSourceInletTemp() {
        return this.getHeatSourceInletTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_10_HEAT_SOURCE_OUTLET_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatSourceOutletTempChannel() {
        return this.channel(ChannelId.IR_10_HEAT_SOURCE_OUTLET_TEMP);
    }
    
    /**
     * Gets the heat source outlet temperature (Waermequelle Austritt), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatSourceOutletTemp() {
        return this.getHeatSourceOutletTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_11_ROOM_REMOTE_ADJ1_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRoomRemoteAdjuster1TempChannel() {
        return this.channel(ChannelId.IR_11_ROOM_REMOTE_ADJ1_TEMP);
    }
    
    /**
     * Gets the temperature at the room remote adjuster 1 (Raumfernversteller 1), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRoomRemoteAdjuster1Temp() {
        return this.getRoomRemoteAdjuster1TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_12_ROOM_REMOTE_ADJ2_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRoomRemoteAdjuster2TempChannel() {
        return this.channel(ChannelId.IR_12_ROOM_REMOTE_ADJ2_TEMP);
    }
    
    /**
     * Gets the temperature at the room remote adjuster 2 (Raumfernversteller 2), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRoomRemoteAdjuster2Temp() {
        return this.getRoomRemoteAdjuster2TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_13_ROOM_REMOTE_ADJ3_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRoomRemoteAdjuster3TempChannel() {
        return this.channel(ChannelId.IR_13_ROOM_REMOTE_ADJ3_TEMP);
    }
    
    /**
     * Gets the temperature at the room remote adjuster 3 (Raumfernversteller 3), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRoomRemoteAdjuster3Temp() {
        return this.getRoomRemoteAdjuster3TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_14_SOLAR_COLLECTOR_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSolarCollectorTempChannel() {
        return this.channel(ChannelId.IR_14_SOLAR_COLLECTOR_TEMP);
    }
    
    /**
     * Gets the temperature of the solar collector (Solarkollektor), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSolarCollectorTemp() {
        return this.getSolarCollectorTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_15_SOLAR_STORAGE_TANK_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSolarStorageTankTempChannel() {
        return this.channel(ChannelId.IR_15_SOLAR_STORAGE_TANK_TEMP);
    }
    
    /**
     * Gets the temperature of the solar collector storage tank (Solarspeicher), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSolarStorageTankTemp() {
        return this.getSolarStorageTankTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_16_EXT_ENERGY_SOURCE_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getExternalEnergySourceTempChannel() {
        return this.channel(ChannelId.IR_16_EXT_ENERGY_SOURCE_TEMP);
    }
    
    /**
     * Gets the temperature of the external energy source (Externe Energiequelle), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getExternalEnergySourceTemp() {
        return this.getExternalEnergySourceTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_17_SUPPLY_AIR_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSupplyAirTempChannel() {
        return this.channel(ChannelId.IR_17_SUPPLY_AIR_TEMP);
    }
    
    /**
     * Gets the supply air temperature (Zulufttemperatur), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSupplyAirTemp() {
        return this.getSupplyAirTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_18_EXTRACT_AIR_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getExtractAirTempChannel() {
        return this.channel(ChannelId.IR_18_EXTRACT_AIR_TEMP);
    }
    
    /**
     * Gets the extract air temperature (Ablufttemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getExtractAirTemp() {
        return this.getExtractAirTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_19_COMPRESSOR_INTAKE_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCompressorIntakeTempChannel() {
        return this.channel(ChannelId.IR_19_COMPRESSOR_INTAKE_TEMP);
    }
    
    /**
     * Gets the compressor intake temperature (Ansaugtemperatur Verdichter), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCompressorIntakeTemp() {
        return this.getCompressorIntakeTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20_EVAPORATOR_INTAKE_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEvaporatorIntakeTempChannel() {
        return this.channel(ChannelId.IR_20_EVAPORATOR_INTAKE_TEMP);
    }
    
    /**
     * Gets the evaporator intake temperature (Ansaugtemperatur Verdampfer), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEvaporatorIntakeTemp() {
        return this.getEvaporatorIntakeTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_21_COMPRESSOR_HEATER_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCompressorHeaterTempChannel() {
        return this.channel(ChannelId.IR_21_COMPRESSOR_HEATER_TEMP);
    }
    
    /**
     * Gets the compressor heater temperature (Temperatur Verdichterheizung), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCompressorHeaterTemp() {
        return this.getCompressorHeaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_22_OVERHEAT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getOverheatingChannel() {
        return this.channel(ChannelId.IR_22_OVERHEAT);
    }
    
    /**
     * Gets the overheating (Ueberhitzung), unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOverheating() {
        return this.getOverheatingChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_23_OVERHEAT_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getOverheatingSetpointChannel() {
        return this.channel(ChannelId.IR_23_OVERHEAT_SETPOINT);
    }
    
    /**
     * Gets the overheating set point (Ueberhitzung Soll), unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOverheatingSetpoint() {
        return this.getOverheatingSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_24_RBE_ROOM_TEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRbeRoomTempActualChannel() {
        return this.channel(ChannelId.IR_24_RBE_ROOM_TEMP);
    }
    
    /**
     * Gets the RBE room temperature actual (RBE, Raumbedieneinheit Raumtemperatur Ist),
     * unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRbeRoomTempActual() {
        return this.getRbeRoomTempActualChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_25_RBE_ROOM_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRbeRoomTempSetpointChannel() {
        return this.channel(ChannelId.IR_25_RBE_ROOM_TEMP_SETPOINT);
    }
    
    /**
     * Gets the RBE room temperature setpoint (RBE, Raumbedieneinheit Raumtemperatur Ist),
     * unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRbeRoomTempSetpoint() {
        return this.getRbeRoomTempSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_26_HIGH_PRESSURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHighPressureChannel() {
        return this.channel(ChannelId.IR_26_HIGH_PRESSURE);
    }
    
    /**
     * Gets the high pressure reading (Druck HD, Hochdruck), unit is centibar.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHighPressure() {
        return this.getHighPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_27_LOW_PRESSURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getLowPressureChannel() {
        return this.channel(ChannelId.IR_27_LOW_PRESSURE);
    }
    
    /**
     * Gets the low pressure reading (Druck ND, Niederdruck), unit is centibar.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getLowPressure() {
        return this.getLowPressureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_28_HOURS_COMP1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursVD1Channel() {
        return this.channel(ChannelId.IR_28_HOURS_COMP1);
    }
    
    /**
     * Gets the operating hours of compressor 1 (Betriebsstunden VD1, Verdichter 1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursVD1() {
        return this.getHoursVD1Channel().value();
    }
    
    /**
     * Gets the Channel for {@link ChannelId#IR_29_HOURS_COMP2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursVD2Channel() {
        return this.channel(ChannelId.IR_29_HOURS_COMP2);
    }
    
    /**
     * Gets the operating hours of compressor 2 (Betriebsstunden VD2, Verdichter 2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursVD2() {
        return this.getHoursVD2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_30_HOURS_AUX1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe1Channel() {
        return this.channel(ChannelId.IR_30_HOURS_AUX1);
    }
    
    /**
     * Gets the operating hours of 2nd heat generator 1 (Betriebsstunden ZWE1, Zusaetzlicher Waermeerzeuger).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe1() {
        return this.getHoursZwe1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_31_HOURS_AUX2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe2Channel() {
        return this.channel(ChannelId.IR_31_HOURS_AUX2);
    }
    
    /**
     * Gets the operating hours of 2nd heat generator 2 (Betriebsstunden ZWE2, Zusaetzlicher Waermeerzeuger).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe2() {
        return this.getHoursZwe2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_32_HOURS_AUX3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe3Channel() {
        return this.channel(ChannelId.IR_32_HOURS_AUX3);
    }
    
    /**
     * Gets the operating hours of 2nd heat generator 3 (Betriebsstunden ZWE3, Zusaetzlicher Waermeerzeuger).
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe3() {
        return this.getHoursZwe3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_33_HOURS_HEAT_PUMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursHeatPumpChannel() {
        return this.channel(ChannelId.IR_33_HOURS_HEAT_PUMP);
    }
    
    /**
     * Gets the operating hours of the heat pump (Betriebsstunden Waermepumpe).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursHeatPump() {
        return this.getHoursHeatPumpChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_34_HOURS_CIRCUIT_HEATING}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursHeatingChannel() {
        return this.channel(ChannelId.IR_34_HOURS_CIRCUIT_HEATING);
    }
    
    /**
     * Gets the operating hours of room heating (Betriebsstunden Heizung).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursHeating() {
        return this.getHoursHeatingChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_35_HOURS_WATER_HEATING}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursDomesticHotWaterChannel() {
        return this.channel(ChannelId.IR_35_HOURS_WATER_HEATING);
    }
    
    /**
     * Gets the operating hours of tap water heating (Betriebsstunden Trinkwarmwasser).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursDomesticHotWater() {
        return this.getHoursDomesticHotWaterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_36_HOURS_POOL_SOLAR}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursSwoPvChannel() {
        return this.channel(ChannelId.IR_36_HOURS_POOL_SOLAR);
    }
    
    /**
     * Gets the operating hours "pool or solar panels" (Betriebsstunden SWoPV, Schwimmbad oder Photovoltaik).
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursSWoPV() {
        return this.getHoursSwoPvChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_37_STATUS}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatpumpOperatingModeChannel() {
        return this.channel(ChannelId.IR_37_STATUS);
    }
    
    /**
     * Gets the current system status of the heat pump (Anlagenstatus).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 7
     *      <li> State -1: Undefined
     *      <li> State 0: Room heating (Heizbetrieb)
     *      <li> State 1: Domestic hot water heating (Trinkwarmwasser)
     *      <li> State 2: Swimming pool heating (Schwimmbad)
     *      <li> State 3: Electric supplier block (EVU-Sperre)
     *      <li> State 4: Defrost (Abtauen)
     *      <li> State 5: Off
     *      <li> State 6: External energy source (Externe Energiequelle)
     *      <li> State 7: Cooling (Kuehlung)
     * </ul>
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatpumpOperatingMode() {
        return this.getHeatpumpOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_38_ENERGY_CIRCUIT_HEATING}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountHeatingChannel() {
        return this.channel(ChannelId.IR_38_ENERGY_CIRCUIT_HEATING);
    }
    
    /**
     * Gets the heat quantity supplied to room heating (Waermemenge Heizung), unit is hectowatt hours (kWh * 10E-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountHeating() {
        return this.getHeatAmountHeatingChannel().value();
    }


    /**
     * Gets the Channel for {@link ChannelId#IR_40_ENERGY_WATER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountDomesticHotWaterChannel() {
        return this.channel(ChannelId.IR_40_ENERGY_WATER);
    }
    
    /**
     * Gets the heat quantity supplied to domestic hot water heating (Waermemenge Trinkwarmwasser),
     * unit is hectowatt hours (kWh * 10E-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountDomesticHotWater() {
        return this.getHeatAmountDomesticHotWaterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_42_ENERGY_POOL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountPoolChannel() {
        return this.channel(ChannelId.IR_42_ENERGY_POOL);
    }
    
    /**
     * Gets the heat quantity supplied to swimming pool heating (Waermemenge Schwimmbad), unit is hectowatt hours
     * (kWh * 10E-1).
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountPool() {
        return this.getHeatAmountPoolChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_44_ENERGY_TOTAL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountAllChannel() {
        return this.channel(ChannelId.IR_44_ENERGY_TOTAL);
    }
    
    /**
     * Gets the total heat quantity supplied (Waermemenge gesamt), unit is hectowatt hours (kWh * 10E-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountAll() {
        return this.getHeatAmountPoolChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_46_ERROR}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.IR_46_ERROR);
    }
    
    /**
     * Error buffer. Only displays current error.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }


    // Coils, read / write. When reading, false = Off, true = On. When writing, false = automatic, true = force on.

    /**
     * Gets the Channel for {@link ChannelId#COIL_0_ERRORRESET}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getClearErrorChannel() {
        return this.channel(ChannelId.COIL_0_ERRORRESET);
    }

    /**
     * Get error reset status.
	 * See {@link ChannelId#COIL_0_ERRORRESET}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getClearError() {
		return this.getClearErrorChannel().value();
	}
	
	/**
     * Error reset. Reset current error message.
	 * See {@link ChannelId#COIL_0_ERRORRESET}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setClearError(Boolean value) throws OpenemsNamedException {
		this.getClearErrorChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_2_HUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnHupChannel() {
        return this.channel(ChannelId.COIL_2_HUP);
    }

    /**
     * Get heat. sys. pump (HUP, Heizung + Brauchwasser Umwaelzpumpe) force on status.
	 * See {@link ChannelId#COIL_2_HUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnHup() {
		return this.getForceOnHupChannel().value();
	}
	
	/**
     * Heat. sys. pump (HUP, Heizung + Brauchwasser Umwaelzpumpe), force on.
	 * See {@link ChannelId#COIL_2_HUP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnHup(Boolean value) throws OpenemsNamedException {
		this.getForceOnHupChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_3_VEN}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnVenChannel() {
        return this.channel(ChannelId.COIL_3_VEN);
    }

    /**
     * Get VEN (Ventilator) force on status.
	 * See {@link ChannelId#COIL_3_VEN}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnVen() {
		return this.getForceOnVenChannel().value();
	}
	
	/**
     * VEN (Ventilator), force on.
	 * See {@link ChannelId#COIL_3_VEN}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnVen(Boolean value) throws OpenemsNamedException {
		this.getForceOnVenChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_4_ZUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnZupChannel() {
        return this.channel(ChannelId.COIL_4_ZUP);
    }

    /**
     * Get CP (ZUP, Zusatz-Umwaelzpumpe) force on status.
	 * See {@link ChannelId#COIL_4_ZUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnZup() {
		return this.getForceOnZupChannel().value();
	}
	
	/**
     * CP (ZUP, Zusatz-Umwaelzpumpe), force on.
	 * See {@link ChannelId#COIL_4_ZUP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnZup(Boolean value) throws OpenemsNamedException {
		this.getForceOnZupChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_5_BUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnBupChannel() {
        return this.channel(ChannelId.COIL_5_BUP);
    }

    /**
     * Get DHW pump (BUP, Trinkwarmwasser-Umwaelzpumpe) force on status.
	 * See {@link ChannelId#COIL_5_BUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnBup() {
		return this.getForceOnBupChannel().value();
	}
	
	/**
     * DHW pump (BUP, Trinkwarmwasser-Umwaelzpumpe), force on.
	 * See {@link ChannelId#COIL_5_BUP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnBup(Boolean value) throws OpenemsNamedException {
		this.getForceOnBupChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_6_BOSUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnBosupChannel() {
        return this.channel(ChannelId.COIL_6_BOSUP);
    }

    /**
     * Get heats.-pump (BOSUP, Brunnen oder Sole-Umwaelzpumpe) force on status.
	 * See {@link ChannelId#COIL_6_BOSUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnBosup() {
		return this.getForceOnBosupChannel().value();
	}
	
	/**
     * Heats.-pump (BOSUP, Brunnen oder Sole-Umwaelzpumpe), force on.
	 * See {@link ChannelId#COIL_7_ZIP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnBosup(Boolean value) throws OpenemsNamedException {
		this.getForceOnBosupChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_6_BOSUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnZipChannel() {
        return this.channel(ChannelId.COIL_7_ZIP);
    }

    /**
     * Get circulation pump (ZIP, Zirkulationspumpe) force on status.
	 * See {@link ChannelId#COIL_7_ZIP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnZip() {
		return this.getForceOnZipChannel().value();
	}
	
	/**
     * Circulation pump (ZIP, Zirkulationspumpe), force on.
	 * See {@link ChannelId#COIL_7_ZIP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnZip(Boolean value) throws OpenemsNamedException {
		this.getForceOnZipChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_8_FUP2}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnFup2Channel() {
        return this.channel(ChannelId.COIL_8_FUP2);
    }

    /**
     * Get CP2 (FUP2, Fussbodenheizungs-Umwaelzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_8_FUP2}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFup2() {
		return this.getForceOnFup2Channel().value();
	}
	
	/**
     * CP2 (FUP2, Fussbodenheizungs-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_8_FUP2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnFup2(Boolean value) throws OpenemsNamedException {
		this.getForceOnFup2Channel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_9_FUP3}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnFup3Channel() {
        return this.channel(ChannelId.COIL_9_FUP3);
    }

    /**
     * Get CP3 (FUP3, Fussbodenheizungs-Umwaelzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_9_FUP3}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFup3() {
		return this.getForceOnFup3Channel().value();
	}
	
	/**
     * CP3 (FUP3, Fussbodenheizungs-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_9_FUP3}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnFup3(Boolean value) throws OpenemsNamedException {
		this.getForceOnFup3Channel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_10_SLP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnSlpChannel() {
        return this.channel(ChannelId.COIL_10_SLP);
    }

    /**
     * Get solar pump (SLP, Solar-Ladepumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_10_SLP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnSlp() {
		return this.getForceOnSlpChannel().value();
	}
	
	/**
     * Solar pump (SLP, Solar-Ladepumpe), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_10_SLP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnSlp(Boolean value) throws OpenemsNamedException {
		this.getForceOnSlpChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_11_SUP}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnSupChannel() {
        return this.channel(ChannelId.COIL_11_SUP);
    }

    /**
     * Get pool pump (SUP, Schwimmbad-Umwaelzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_11_SUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnSup() {
		return this.getForceOnSupChannel().value();
	}
	
	/**
     * Pool pump (SUP, Schwimmbad-Umwaelzpumpe), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_11_SUP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnSup(Boolean value) throws OpenemsNamedException {
		this.getForceOnSupChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_12_VSK}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnVskChannel() {
        return this.channel(ChannelId.COIL_12_VSK);
    }

    /**
     * Get VSK (Bypassklappe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_12_VSK}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnVsk() {
		return this.getForceOnVskChannel().value();
	}
	
	/**
     * VSK (Bypassklappe), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_12_VSK}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnVsk(Boolean value) throws OpenemsNamedException {
		this.getForceOnVskChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#COIL_13_FRH}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getForceOnFrhChannel() {
        return this.channel(ChannelId.COIL_13_FRH);
    }

    /**
     * Get FRH (Schuetz Defrostheizung) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_13_FRH}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFrh() {
		return this.getForceOnFrhChannel().value();
	}
	
	/**
     * FRH (Schuetz Defrostheizung), force on. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_13_FRH}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceOnFrh(Boolean value) throws OpenemsNamedException {
		this.getForceOnFrhChannel().setNextWriteValue(value);
	}


    // Holding Registers, read / write.

    /**
     * Gets the Channel for {@link ChannelId#HR_0_OUTSIDETEMP}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getOutsideTempChannel() {
        return this.channel(ChannelId.HR_0_OUTSIDETEMP);
    }

    /**
     * Get outdoor temperature, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getOutsideTemp() {
		return this.getOutsideTempChannel().value();
	}
	
	/**
     * Set outdoor temperature, unit is decimal degree Celsius. Minimum -200, maximum 800.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutsideTemp(Integer value) throws OpenemsNamedException {
		this.getOutsideTempChannel().setNextWriteValue(value);
	}
	
	/**
     * Set outdoor temperature, unit is decimal degree Celsius. Minimum -200, maximum 800.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutsideTemp(int value) throws OpenemsNamedException {
		this.getOutsideTempChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_1_RETURN_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getReturnTempSetpointChannel() {
        return this.channel(ChannelId.HR_1_RETURN_TEMP_SETPOINT);
    }

    /**
     * Get return temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_1_RETURN_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getReturnTempSetpoint() {
		return this.getReturnTempSetpointChannel().value();
	}
	
	/**
     * Set return temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_1_RETURN_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReturnTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getReturnTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set return temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_1_RETURN_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReturnTempSetpoint(int value) throws OpenemsNamedException {
		this.getReturnTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_2_FLOW_TEMP_SETPOINT_MC1}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit1FlowTempSetpointChannel() {
        return this.channel(ChannelId.HR_2_FLOW_TEMP_SETPOINT_MC1);
    }

    /**
     * Get mixing circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_2_FLOW_TEMP_SETPOINT_MC1}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getCircuit1FlowTempSetpoint() {
		return this.getCircuit1FlowTempSetpointChannel().value();
	}
	
	/**
     * Set mixing circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_2_FLOW_TEMP_SETPOINT_MC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit1FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit1FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mixing circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_2_FLOW_TEMP_SETPOINT_MC1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit1FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit1FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_3_FLOW_TEMP_SETPOINT_MC2}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit2FlowTempSetpointChannel() {
	    return this.channel(ChannelId.HR_3_FLOW_TEMP_SETPOINT_MC2);
	}

	/**
     * Get mixing circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_3_FLOW_TEMP_SETPOINT_MC2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit2FlowTempSetpoint() {
		return this.getCircuit2FlowTempSetpointChannel().value();
	}
	
	/**
     * Set mixing circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_3_FLOW_TEMP_SETPOINT_MC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit2FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mixing circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_3_FLOW_TEMP_SETPOINT_MC2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit2FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_4_FLOW_TEMP_SETPOINT_MC3}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit3FlowTempSetpointChannel() {
	    return this.channel(ChannelId.HR_4_FLOW_TEMP_SETPOINT_MC3);
	}

	/**
     * Get mixing circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_4_FLOW_TEMP_SETPOINT_MC3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit3FlowTempSetpoint() {
		return this.getCircuit3FlowTempSetpointChannel().value();
	}
	
	/**
     * Set mixing circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_4_FLOW_TEMP_SETPOINT_MC3}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit3FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mixing circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_4_FLOW_TEMP_SETPOINT_MC3}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit3FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_5_WATER_TEMP_SETPOINT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getDomesticHotWaterTempSetpointChannel() {
	    return this.channel(ChannelId.HR_5_WATER_TEMP_SETPOINT);
	}

	/**
     * Get domestic hot water (Trinkwarmwasser) desired temperature value, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_5_WATER_TEMP_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDomesticHotWaterTempSetpoint() {
		return this.getDomesticHotWaterTempSetpointChannel().value();
	}
	
	/**
     * Set domestic hot water (Trinkwarmwasser) desired temperature value, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_5_WATER_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getDomesticHotWaterTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set domestic hot water (Trinkwarmwasser) desired temperature value, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_5_WATER_TEMP_SETPOINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterTempSetpoint(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_6_BLOCK_RELEASE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getBlockReleaseChannel() {
	    return this.channel(ChannelId.HR_6_BLOCK_RELEASE);
	}

	/**
     * Get block / release heat pump (Sperre / Freigabe Waermepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Block heat pump (Sperre)
     *      <li> State 1: Release 1 compressor (Freigabe 1 Verdichter)
     *      <li> State 2: Release 2 compressors (Freigabe 2 Verdichter)
     * </ul>
	 * See {@link ChannelId#HR_6_BLOCK_RELEASE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBlockRelease() {
		return this.getBlockReleaseChannel().value();
	}
	
	/**
     * Set block / release heat pump (Sperre / Freigabe Waermepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Block heat pump (Sperre)
     *      <li> State 1: Release 1 compressor (Freigabe 1 Verdichter)
     *      <li> State 2: Release 2 compressors (Freigabe 2 Verdichter)
     * </ul>
	 * See {@link ChannelId#HR_6_BLOCK_RELEASE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBlockRelease(Integer value) throws OpenemsNamedException {
		this.getBlockReleaseChannel().setNextWriteValue(value);
	}
	
	/**
     * Set block / release heat pump (Sperre / Freigabe Waermepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Block heat pump (Sperre)
     *      <li> State 1: Release 1 compressor (Freigabe 1 Verdichter)
     *      <li> State 2: Release 2 compressors (Freigabe 2 Verdichter)
     * </ul>
	 * See {@link ChannelId#HR_6_BLOCK_RELEASE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBlockRelease(int value) throws OpenemsNamedException {
		this.getBlockReleaseChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_7_CIRCUIT_HEATING_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingOperationModeChannel() {
	    return this.channel(ChannelId.HR_7_CIRCUIT_HEATING_OPERATION_MODE);
	}

	/**
     * Get mode of operation room heating (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_CIRCUIT_HEATING_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingOperationMode() {
		return this.getHeatingOperationModeChannel().value();
	}
	
	/**
     * Set mode of operation room heating (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_CIRCUIT_HEATING_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingOperationMode(Integer value) throws OpenemsNamedException {
		this.getHeatingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation room heating (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_CIRCUIT_HEATING_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingOperationMode(int value) throws OpenemsNamedException {
		this.getHeatingOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_8_WATER_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getDomesticHotWaterOperationModeChannel() {
        return this.channel(ChannelId.HR_8_WATER_OPERATION_MODE);
    }

    /**
     * Get mode of operation domestic hot water (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_WATER_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getDomesticHotWaterOperationMode() {
		return this.getDomesticHotWaterOperationModeChannel().value();
	}
	
	/**
     * Set mode of operation domestic hot water (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_WATER_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterOperationMode(Integer value) throws OpenemsNamedException {
		this.getDomesticHotWaterOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation domestic hot water (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_WATER_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDomesticHotWaterOperationMode(int value) throws OpenemsNamedException {
		this.getDomesticHotWaterOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_9_MC2_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit2OperationModeChannel() {
	    return this.channel(ChannelId.HR_9_MC2_OPERATION_MODE);
	}

	/**
     * Get mode of operation mixing circuit 2 (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MC2_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit2OperationMode() {
		return this.getCircuit2OperationModeChannel().value();
	}
	
	/**
     * Set mode of operation mixing circuit 2 (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MC2_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2OperationMode(Integer value) throws OpenemsNamedException {
		this.getCircuit2OperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation mixing circuit 2 (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MC2_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2OperationMode(int value) throws OpenemsNamedException {
		this.getCircuit2OperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_10_MC3_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit3OperationModeChannel() {
	    return this.channel(ChannelId.HR_10_MC3_OPERATION_MODE);
	}

	/**
     * Get mode of operation mixing circuit 3 (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MC3_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit3OperationMode() {
		return this.getCircuit3OperationModeChannel().value();
	}
	
	/**
     * Set mode of operation mixing circuit 3 (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MC3_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3OperationMode(Integer value) throws OpenemsNamedException {
		this.getCircuit3OperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation mixing circuit 3 (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Second heat generator (Zusaetzlicher Waermeerzeuger)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MC3_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3OperationMode(int value) throws OpenemsNamedException {
		this.getCircuit3OperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_11_COOLING_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCoolingOperationModeChannel() {
	    return this.channel(ChannelId.HR_11_COOLING_OPERATION_MODE);
	}

	/**
     * Get mode of operation cooling (Betriebsart Kuehlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 1
     *      <li> State -1: Undefined
     *      <li> State 0: Off
     *      <li> State 1: Automatic
     * </ul>
	 * See {@link ChannelId#HR_11_COOLING_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCoolingOperationMode() {
		return this.getCoolingOperationModeChannel().value();
	}
	
	/**
     * Set mode of operation cooling (Betriebsart Kuehlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 1
     *      <li> State -1: Undefined
     *      <li> State 0: Off
     *      <li> State 1: Automatic
     * </ul>
	 * See {@link ChannelId#HR_11_COOLING_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCoolingOperationMode(Integer value) throws OpenemsNamedException {
		this.getCoolingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation cooling (Betriebsart Kuehlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 1
     *      <li> State -1: Undefined
     *      <li> State 0: Off
     *      <li> State 1: Automatic
     * </ul>
	 * See {@link ChannelId#HR_11_COOLING_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCoolingOperationMode(int value) throws OpenemsNamedException {
		this.getCoolingOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_12_VENTILATION_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getVentilationOperationModeChannel() {
	    return this.channel(ChannelId.HR_12_VENTILATION_OPERATION_MODE);
	}

	/**
     * Get mode of operation ventilation (Betriebsart Lueftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 3
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: No late night throttling (Party)
     *      <li> State 2: Holidays, full time throttling (Ferien)
     *      <li> State 3: Off
     * </ul>
	 * See {@link ChannelId#HR_12_VENTILATION_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVentilationOperationMode() {
		return this.getVentilationOperationModeChannel().value();
	}
	
	/**
     * Set mode of operation ventilation (Betriebsart Lueftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 3
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: No late night throttling (Party)
     *      <li> State 2: Holidays, full time throttling (Ferien)
     *      <li> State 3: Off
     * </ul>
	 * See {@link ChannelId#HR_12_VENTILATION_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationOperationMode(Integer value) throws OpenemsNamedException {
		this.getVentilationOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation ventilation (Betriebsart Lueftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 3
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: No late night throttling (Party)
     *      <li> State 2: Holidays, full time throttling (Ferien)
     *      <li> State 3: Off
     * </ul>
	 * See {@link ChannelId#HR_12_VENTILATION_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationOperationMode(int value) throws OpenemsNamedException {
		this.getVentilationOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_13_POOL_OPERATION_MODE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getPoolHeatingOperationModeChannel() {
	    return this.channel(ChannelId.HR_13_POOL_OPERATION_MODE);
	}

	/**
     * Get mode of operation swimming pool heating (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Value not in use (Wert nicht benutzt)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOL_OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPoolHeatingOperationMode() {
		return this.getPoolHeatingOperationModeChannel().value();
	}
	
	/**
     * Set mode of operation swimming pool heating (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Value not in use (Wert nicht benutzt)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOL_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPoolHeatingOperationMode(Integer value) throws OpenemsNamedException {
		this.getPoolHeatingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set mode of operation swimming pool heating (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 4
     *      <li> State -1: Undefined
     *      <li> State 0: Automatic
     *      <li> State 1: Value not in use (Wert nicht benutzt)
     *      <li> State 2: No late night throttling (Party)
     *      <li> State 3: Holidays, full time throttling (Ferien)
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOL_OPERATION_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPoolHeatingOperationMode(int value) throws OpenemsNamedException {
		this.getPoolHeatingOperationModeChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR_14_SMART_GRID}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSmartGridToModbusChannel() {
        return this.channel(ChannelId.HR_14_SMART_GRID);
    }

    /**
     * Internal method. Use getSmartGridState() of HeatpumpSmartGrid interface.
     * See {@link ChannelId#HR_14_SMART_GRID}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSmartGridFromModbus() {
        return this.getSmartGridToModbusChannel().value();
    }

    /**
     * Internal method. Use setSmartGridState() of HeatpumpSmartGrid interface.
     * See {@link ChannelId#HR_14_SMART_GRID}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setSmartGridToModbus(Integer value) throws OpenemsNamedException {
        this.getSmartGridToModbusChannel().setNextWriteValue(value);
    }

    /**
     * Internal method. Use setSmartGridState() of HeatpumpSmartGrid interface.
     * See {@link ChannelId#HR_14_SMART_GRID}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setSmartGridToModbus(int value) throws OpenemsNamedException {
        this.getSmartGridToModbusChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_15_CURVE_CIRCUIT_HEATING_END_POINT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveEndPointChannel() {
	    return this.channel(ChannelId.HR_15_CURVE_CIRCUIT_HEATING_END_POINT);
	}

	/**
     * Get heating curve room heating end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_15_CURVE_CIRCUIT_HEATING_END_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveEndPoint() {
		return this.getHeatingCurveEndPointChannel().value();
	}
	
	/**
     * Set heating curve room heating end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_15_CURVE_CIRCUIT_HEATING_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveEndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveEndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve room heating end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_15_CURVE_CIRCUIT_HEATING_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveEndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveEndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_16_CURVE_CIRCUIT_HEATING_SHIFT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveParallelShiftChannel() {
	    return this.channel(ChannelId.HR_16_CURVE_CIRCUIT_HEATING_SHIFT);
	}

	/**
     * Get heating curve room heating parallel shift (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_16_CURVE_CIRCUIT_HEATING_SHIFT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveParallelShift() {
		return this.getHeatingCurveParallelShiftChannel().value();
	}
	
	/**
     * Set heating curve room heating parallel shift (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
     * Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_16_CURVE_CIRCUIT_HEATING_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveParallelShift(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveParallelShiftChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve room heating parallel shift (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
     * Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_16_CURVE_CIRCUIT_HEATING_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveParallelShift(int value) throws OpenemsNamedException {
		this.getHeatingCurveParallelShiftChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_17_CURVE_MC1_END_POINT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit1EndPointChannel() {
	    return this.channel(ChannelId.HR_17_CURVE_MC1_END_POINT);
	}

	/**
     * Get heating curve mixing circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_17_CURVE_MC1_END_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit1EndPoint() {
		return this.getHeatingCurveCircuit1EndPointChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_17_CURVE_MC1_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_17_CURVE_MC1_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_18_CURVE_MC1_SHIFT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit1ParallelShiftChannel() {
	    return this.channel(ChannelId.HR_18_CURVE_MC1_SHIFT);
	}

	/**
     * Get heating curve mixing circuit 1 parallel shift (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius.
	 * See {@link ChannelId#HR_18_CURVE_MC1_SHIFT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit1ParallelShift() {
		return this.getHeatingCurveCircuit1ParallelShiftChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 1 parallel shift (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_18_CURVE_MC1_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1ParallelShift(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1ParallelShiftChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 1 parallel shift (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_18_CURVE_MC1_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1ParallelShift(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1ParallelShiftChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_19_CURVE_MC2_END_POINT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit2EndPointChannel() {
	    return this.channel(ChannelId.HR_19_CURVE_MC2_END_POINT);
	}

	/**
     * Get heating curve mixing circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_CURVE_MC2_END_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit2EndPoint() {
		return this.getHeatingCurveCircuit2EndPointChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_CURVE_MC2_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_CURVE_MC2_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_20_CURVE_MC2_SHIFT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit2ParallelShiftChannel() {
	    return this.channel(ChannelId.HR_20_CURVE_MC2_SHIFT);
	}

	/**
     * Get heating curve mixing circuit 2 parallel shift (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_CURVE_MC2_SHIFT}
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit2ParallelShift() {
		return this.getHeatingCurveCircuit2ParallelShiftChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 2 parallel shift (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_CURVE_MC2_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2ParallelShift(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2ParallelShiftChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 2 parallel shift (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_CURVE_MC2_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2ParallelShift(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2ParallelShiftChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_21_CURVE_MC3_END_POINT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit3EndPointChannel() {
	    return this.channel(ChannelId.HR_21_CURVE_MC3_END_POINT);
	}

	/**
     * Get heating curve mixing circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_CURVE_MC3_END_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit3EndPoint() {
		return this.getHeatingCurveCircuit3EndPointChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_CURVE_MC3_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_CURVE_MC3_END_POINT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_22_CURVE_MC3_SHIFT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit3ParallelShiftChannel() {
	    return this.channel(ChannelId.HR_22_CURVE_MC3_SHIFT);
	}

	/**
     * Get heating curve mixing circuit 3 parallel shift (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_CURVE_MC3_SHIFT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit3ParallelShift() {
		return this.getHeatingCurveCircuit3ParallelShiftChannel().value();
	}
	
	/**
     * Set heating curve mixing circuit 3 parallel shift (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_CURVE_MC3_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3ParallelShift(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3ParallelShiftChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve mixing circuit 3 parallel shift (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_CURVE_MC3_SHIFT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3ParallelShift(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3ParallelShiftChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_23_TEMP_PM}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getTempPlusMinusChannel() {
	    return this.channel(ChannelId.HR_23_TEMP_PM);
	}

	/**
     * Get temperature +-, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_23_TEMP_PM}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTempPlusMinus() {
		return this.getTempPlusMinusChannel().value();
	}
	
	/**
     * Set temperature +-, unit is decimal degree Celsius. Minimum -50, maximum 50.
	 * See {@link ChannelId#HR_23_TEMP_PM}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTempPlusMinus(Integer value) throws OpenemsNamedException {
		this.getTempPlusMinusChannel().setNextWriteValue(value);
	}
	
	/**
     * Set temperature +-, unit is decimal degree Celsius. Minimum -50, maximum 50.
	 * See {@link ChannelId#HR_23_TEMP_PM}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTempPlusMinus(int value) throws OpenemsNamedException {
		this.getTempPlusMinusChannel().setNextWriteValue(value);
	}
}
