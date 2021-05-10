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
import io.openems.edge.heater.api.HeatpumpSmartGridGeneralizedChannel;

/**
 * Channels for the Alpha Innotec heat pump.
 */

public interface HeatpumpAlphaInnotecChannel extends HeatpumpSmartGridGeneralizedChannel {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Discrete Inputs (DI) 0 to 7, read only. 0 = Off, 1 = On. Represented as boolean

        /**
         * Scheduled off time (EVU, Energie Versorger Unterbrechung).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_0_EVU(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Smart grid scheduled off time (EVU2). Like EVU, but triggered because of smart grid setting.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_1_EVU2(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Pool thermostat (SWT, Schwimmbadthermostat).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_2_SWT(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Compressor 1 (VD1, Verdichter 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_3_VD1(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Compressor 2 (VD2, Verdichter 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_4_VD2(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Fallback heater 1 (ZWE1, zusätzlicher Wärmeerzeuger 1).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_5_ZWE1(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Fallback heater 2 (ZWE2, zusätzlicher Wärmeerzeuger 2).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_6_ZWE2(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),

        /**
         * Fallback heater 3 (ZWE3, zusätzlicher Wärmeerzeuger 3). Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        DI_7_ZWE3(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)),


        // Input Registers (IR) 0 to 46, read only. They are 16 bit unsigned numbers unless stated otherwise.

        /**
         * Median temperature (Mitteltemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_0_MITTELTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Flow temperature (Vorlauftemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_1_VORLAUFTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Return temperature (Rücklauftemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_2_RUECKLAUFTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * External return temperature (Rücklauf extern).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_3_RUECKEXTERN(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Hot tap water temperature (Trinkwarmwassertemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_4_TRINKWWTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 flow temperature (Mischkreis 1 Vorlauf).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_5_MK1VORLAUF(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 flow temperature (Mischkreis 2 Vorlauf).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_6_MK2VORLAUF(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 3 flow temperature (Mischkreis 3 Vorlauf).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_7_MK3VORLAUF(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Hot gas temperature (Heissgastemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_8_HEISSGASTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heat source inlet (Wärmequelle Eintritt).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_9_WQEINTRITT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heat source outlet (Wärmequelle Austritt).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_10_WQAUSTRITT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Room temperature remote controller 1 (Raumfernversteller 1).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_11_RAUMFV1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Room temperature remote controller 2 (Raumfernversteller 2). Optional, depends on heat pump model if
         * available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_12_RAUMFV2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Room temperature remote controller 3 (Raumfernversteller 3). Optional, depends on heat pump model if
         * available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_13_RAUMFV3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Solar collector (Solarkollektor). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_14_SOLARKOLLEKTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Solar collector buffer tank (Solarspeicher). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_15_SOLARSPEICHER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * External heat source (Externe Energiequelle). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_16_EXTEQ(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Airflow inlet temperature (Zulufttemperatur). Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_17_ZULUFTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Airflow outlet temperature (Ablufttemperatur).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_18_ABLUFTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Compressor inlet temperature (Ansaugtemperatur Verdichter).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_19_ANSAUGTEMPVDICHTER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Evaporator inlet temperature (Ansaugtemperatur Verdampfer).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_20_ANSAUGTEMPVDAMPFER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Compressor heater temperature (Temperatur Verdichterheizung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_21_TEMPVDHEIZUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Overheating (Überhitzung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Kelvin
         * </ul>
         */
        IR_22_UEBERHITZ(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Overheating set point (Überhitzung Soll).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Kelvin
         * </ul>
         */
        IR_23_UEBERHITZSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Measured temperature at room temperature control panel (RBE, Raumbedieneinheit Raumtemperatur Ist).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_24_RBERAUMTEMPIST(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Set point temperature at room temperature control panel (RBE, Raumbedieneinheit Raumtemperatur Soll).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        IR_25_RBERAUMTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * High pressure (Druck HD, Hochdruck).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: centibar
         * </ul>
         */
        IR_26_DRUCKHD(Doc.of(OpenemsType.INTEGER)),

        /**
         * Low pressure (Druck ND, Niederdruck).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: centibar
         * </ul>
         */
        IR_27_DRUCKND(Doc.of(OpenemsType.INTEGER)),

        /**
         * Operating hours compressor 1 (Betriebsstunden VD1, Verdichter 1).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_28_TVD1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours compressor 2 (Betriebsstunden VD2, Verdichter 2).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_29_TVD2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours fallback heater 1 (Betriebsstunden ZWE1, Zusätzlicher Wärmeerzeuger).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_30_TZWE1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours fallback heater 2 (Betriebsstunden ZWE2, Zusätzlicher Wärmeerzeuger).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_31_TZWE2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours fallback heater 3 (Betriebsstunden ZWE3, Zusätzlicher Wärmeerzeuger).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_32_TZWE3(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours heat pump (Betriebsstunden Wärmepumpe).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_33_TWAERMEPUMPE(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours room heating (Betriebsstunden Heizung).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_34_THEIZUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours tap water heating (Betriebsstunden Trinkwarmwasser).
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_35_TTRINKWW(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours pool or solar panels (Betriebsstunden SWoPV, Schwimmbad oder Photovoltaik).
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: hours
         * </ul>
         */
        IR_36_TSWOPV(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Current operating state of the heat pump (Anlagenstatus).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 7
         *      <li> State 0: Heating (Heizbetrieb)
         *      <li> State 1: Heating tap water (Trinkwarmwasser)
         *      <li> State 2: Swimming pool (Schwimmbad)
         *      <li> State 3: Forced off by energy supplier (EVU-Sperre)
         *      <li> State 4: Defrost (Abtauen)
         *      <li> State 5: Off
         *      <li> State 6: External energy source (Externe Energiequelle)
         *      <li> State 7: Cooling (Kühlung)
         * </ul>
         */
        IR_37_STATUS(Doc.of(OperatingMode.values())),

        /**
         * Heat supplied to room heating (Wärmemenge Heizung). 32 bit unsigned doubleword. IR 38 is high, IR 39 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_38_WHHEIZUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat supplied to tap water (Wärmemenge Trinkwarmwasser). 32 bit unsigned doubleword. IR 40 is high, IR 41 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_40_WHTRINKWW(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat supplied to pool (Wärmemenge Schwimmbad). 32 bit unsigned doubleword. IR 42 is high, IR 43 is low.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_42_WHPOOL(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

        /**
         * Heat supplied total (Wärmemenge gesamt). 32 bit unsigned doubleword. IR 44 is high, IR 45 is low.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: kWh * 10E-1
         * </ul>
         */
        IR_44_WHTOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.HECTOWATT_HOURS)),

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
         * HUP (Heizung + Brauchwasser Umwälzpumpe), force on.
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
         * ZUP (Zusatz-Umwälzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_4_ZUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * BUP (Trinkwarmwasser-Umwälzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_5_BUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * BOSUP (Brunnen oder Sole-Umwälzpumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_6_BOSUP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * ZIP (Zirkulationspumpe), force on.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_7_ZIP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * FUP2 (Fußbodenheizungs-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_8_FUP2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * FUP3 (Fußbodenheizungs-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_9_FUP3(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SLP (Solar-Ladepumpe), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_10_SLP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SUP (Schwimmbad-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
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
         * FRH (Schütz Defrostheizung), force on. Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        COIL_13_FRH(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),


        // Holding Registers (HR) 0 to 23, read/write. They are 16 bit unsigned numbers unless stated otherwise.

        /**
         * Outside temperature. Signed 16 bit. Minimum -200, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_0_OUTSIDETEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Return temperature set point. Minimum 150, maximum 800. (<- Aus Handbuch. Minimum Wert sicher falsch, schon
         * Wert 50 ausgelesen.)
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_1_RUECKTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 1 (Mischkreis 1) flow temperature setpoint. Minimum 150, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_2_MK1VORTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 2 (Mischkreis 2) flow temperature setpoint. Minimum 150, maximum 800.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_3_MK2VORTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 3 (Mischkreis 3) flow temperature setpoint. Minimum 150, maximum 800.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_4_MK3VORTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Hot tap water (Trinkwarmwasser) temperature setpoint. Minimum 150, maximum 800.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_5_TRINKWWTEMPSOLL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heat pump run clearance (Sperre / Freigabe Wärmepumpe).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 2
         *      <li> State 0: Sperre / Off
         *      <li> State 1: Freigabe 1 Verdichter / Clearance 1 compressor
         *      <li> State 2: Freigabe 2 Verdichter / Clearance 2 compressors
         * </ul>
         */
        HR_6_RUNCLEARANCE(Doc.of(Clearance.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Room heating operating mode (Betriebsart Heizung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Automatik
         *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
         *      <li> State 2: Party / No late night throttling
         *      <li> State 3: Ferien / Vacation, full time throttling
         *      <li> State 4: Off
         * </ul>
         */
        HR_7_HEIZUNGRUNSTATE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Hot tap water operating mode (Betriebsart Trinkwarmwasser).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Automatik
         *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
         *      <li> State 2: Party / No late night throttling
         *      <li> State 3: Ferien / Vacation, full time throttling
         *      <li> State 4: Off
         * </ul>
         */
        HR_8_TRINKWWRUNSTATE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 2 operating mode (Betriebsart Mischkreis 2).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Automatik
         *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
         *      <li> State 2: Party / No late night throttling
         *      <li> State 3: Ferien / Vacation, full time throttling
         *      <li> State 4: Off
         * </ul>
         */
        HR_9_MK2RUNSTATE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 3 operating mode (Betriebsart Mischkreis 3).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Automatik
         *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
         *      <li> State 2: Party / No late night throttling
         *      <li> State 3: Ferien / Vacation, full time throttling
         *      <li> State 4: Off
         * </ul>
         */
        HR_10_MK3RUNSTATE(Doc.of(HeatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling operating mode (Betriebsart Kühlung).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 1
         *      <li> State 0: Off
         *      <li> State 1: Automatik
         * </ul>
         */
        HR_11_COOLINGRUNSTATE(Doc.of(CoolingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Ventilation operating mode (Betriebsart Lüftung).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 3
         *      <li> State 0: Automatik
         *      <li> State 1: Party / No late night throttling
         *      <li> State 2: Ferien / Vacation, full time throttling
         *      <li> State 3: Off
         * </ul>
         */
        HR_12_VENTILATIONRUNSTATE(Doc.of(VentilationMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pool heating operating mode (Betriebsart Schwimmbad).
         * Optional, depends on heat pump model if available.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Automatik
         *      <li> State 1: Wert nicht benutzt / Value not in use
         *      <li> State 2: Party / No late night throttling
         *      <li> State 3: Ferien / Vacation, full time throttling
         *      <li> State 4: Off
         * </ul>
         */
        HR_13_POOLRUNSTATE(Doc.of(PoolMode.values()).accessMode(AccessMode.READ_WRITE)),

        // HR_14 = Smart Grid, use channel of HeatpumpSmartGridGeneralizedChannel interface.

        /**
         * Heater heating curve end point (Heizkurve Heizung Endpunkt). Minimum 200, maximum 700.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_15_HKHEIZUNGENDPKT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heater heating curve parallel translation (Heizkurve Heizung Parallelverschiebung). Minimum 50, maximum 350.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_16_HKHEIZUNGPARAVER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 1 heating curve end point (Heizkurve Mischkreis 1 Endpunkt). Minimum 200, maximum 700.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_17_HKMK1ENDPKT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 1 heating curve parallel translation (Heizkurve Mischkreis 1 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_18_HKMK1PARAVER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 2 heating curve end point (Heizkurve Mischkreis 2 Endpunkt). Minimum 200, maximum 700.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_19_HKMK2ENDPKT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 3 heating curve parallel translation (Heizkurve Mischkreis 2 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_20_HKMK2PARAVER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 3 heating curve end point (Heizkurve Mischkreis 3 Endpunkt). Minimum 200, maximum 700.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_21_HKMK3ENDPKT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating circuit 3 heating curve parallel translation (Heizkurve Mischkreis 3 Parallelverschiebung).
         * Minimum 50, maximum 350.
         * Optional, depends on heat pump model if available.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_22_HKMK3PARAVER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Temperature +-. Minimum -50, maximum 50. Signed 16 bit number.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: decimal degree Celsius
         * </ul>
         */
        HR_23_TEMPPM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE));


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
     * Gets the Channel for {@link ChannelId#DI_0_EVU}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvuActiveChannel() {
        return this.channel(ChannelId.DI_0_EVU);
    }
    
    /**
     * Scheduled off time (EVU, Energie Versorger Unterbrechung).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvuActive() {
        return this.getEvuActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_1_EVU2}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvu2ActiveChannel() {
        return this.channel(ChannelId.DI_1_EVU2);
    }
    
    /**
     * Smart grid scheduled off time (EVU2). Like EVU, but triggered because of smart grid setting.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvu2Active() {
        return this.getEvu2ActiveChannel().value();
    }
    
    /**
     * Gets the Channel for {@link ChannelId#DI_2_SWT}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getSwtActiveChannel() {
        return this.channel(ChannelId.DI_2_SWT);
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
     * Gets the Channel for {@link ChannelId#DI_3_VD1}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getVD1activeChannel() {
        return this.channel(ChannelId.DI_3_VD1);
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
     * Gets the Channel for {@link ChannelId#DI_4_VD2}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getVD2activeChannel() {
        return this.channel(ChannelId.DI_4_VD2);
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
     * Gets the Channel for {@link ChannelId#DI_5_ZWE1}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe1ActiveChannel() {
        return this.channel(ChannelId.DI_5_ZWE1);
    }
    
    /**
     * Fallback heater 1 (ZWE1, zusätzlicher Wärmeerzeuger 1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe1Active() {
        return this.getZwe1ActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_6_ZWE2}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe2ActiveChannel() {
        return this.channel(ChannelId.DI_6_ZWE2);
    }
    
    /**
     * Fallback heater 2 (ZWE2, zusätzlicher Wärmeerzeuger 2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe2Active() {
        return this.getZwe2ActiveChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DI_7_ZWE3}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getZwe3ActiveChannel() {
        return this.channel(ChannelId.DI_7_ZWE3);
    }
    
    /**
     * Fallback heater 3 (ZWE3, zusätzlicher Wärmeerzeuger 3). Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getZwe3Active() {
        return this.getZwe3ActiveChannel().value();
    }


    // Input Registers

    /**
     * Gets the Channel for {@link ChannelId#IR_0_MITTELTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMittelTempChannel() {
        return this.channel(ChannelId.IR_0_MITTELTEMP);
    }
    
    /**
     * Gets the median temperature (Mitteltemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMittelTemp() {
        return this.getMittelTempChannel().value();
    }
    
    /**
     * Gets the Channel for {@link ChannelId#IR_1_VORLAUFTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getFlowTempChannel() {
        return this.channel(ChannelId.IR_1_VORLAUFTEMP);
    }
    
    /**
     * Gets the flow temperature (Vorlauftemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getFlowTemp() {
        return this.getFlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_2_RUECKLAUFTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getReturnTempChannel() {
        return this.channel(ChannelId.IR_2_RUECKLAUFTEMP);
    }
    
    /**
     * Gets the return temperature (Rücklauftemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getReturnTemp() {
        return this.getReturnTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_3_RUECKEXTERN}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRuecklaufExternTempChannel() {
        return this.channel(ChannelId.IR_3_RUECKEXTERN);
    }
    
    /**
     * Gets the external return temperature (Rücklauf extern), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRuecklaufExternTemp() {
        return this.getRuecklaufExternTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_4_TRINKWWTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getTrinkWwTempChannel() {
        return this.channel(ChannelId.IR_4_TRINKWWTEMP);
    }
    
    /**
     * Gets the hot tap water temperature (Trinkwarmwassertemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getTrinkWwTemp() {
        return this.getTrinkWwTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_5_MK1VORLAUF}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit1FlowTempChannel() {
        return this.channel(ChannelId.IR_5_MK1VORLAUF);
    }
    
    /**
     * Get the heating circuit 1 flow temperature (Mischkreis 1 Vorlauf), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit1FlowTemp() {
        return this.getCircuit1FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_6_MK2VORLAUF}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit2FlowTempChannel() {
        return this.channel(ChannelId.IR_6_MK2VORLAUF);
    }
    
    /**
     * Get the heating circuit 2 flow temperature (Mischkreis 2 Vorlauf), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit2FlowTemp() {
        return this.getCircuit2FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_7_MK3VORLAUF}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCircuit3FlowTempChannel() {
        return this.channel(ChannelId.IR_7_MK3VORLAUF);
    }
    
    /**
     * Get the heating circuit 3 flow temperature (Mischkreis 3 Vorlauf), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCircuit3FlowTemp() {
        return this.getCircuit3FlowTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_8_HEISSGASTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeissGasTempChannel() {
        return this.channel(ChannelId.IR_8_HEISSGASTEMP);
    }
    
    /**
     * Gets the hot gas temperature (Heissgastemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeissGasTemp() {
        return this.getHeissGasTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_9_WQEINTRITT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getWaermequelleEintrittTempChannel() {
        return this.channel(ChannelId.IR_9_WQEINTRITT);
    }
    
    /**
     * Gets the heat source inlet temperature (Wärmequelle Eintritt), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getWaermequelleEintrittTemp() {
        return this.getWaermequelleEintrittTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_10_WQAUSTRITT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getWaermequelleAustrittTempChannel() {
        return this.channel(ChannelId.IR_10_WQAUSTRITT);
    }
    
    /**
     * Gets the heat source outlet temperature (Wärmequelle Austritt), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getWaermequelleAustrittTemp() {
        return this.getWaermequelleAustrittTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_11_RAUMFV1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRaumfernversteller1TempChannel() {
        return this.channel(ChannelId.IR_11_RAUMFV1);
    }
    
    /**
     * Gets the temperature at the remote controller 1 (Raumfernversteller 1), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRaumfernversteller1Temp() {
        return this.getRaumfernversteller1TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_12_RAUMFV2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRaumfernversteller2TempChannel() {
        return this.channel(ChannelId.IR_12_RAUMFV2);
    }
    
    /**
     * Gets the temperature at the remote controller 2 (Raumfernversteller 2), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRaumfernversteller2Temp() {
        return this.getRaumfernversteller2TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_13_RAUMFV3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRaumfernversteller3TempChannel() {
        return this.channel(ChannelId.IR_13_RAUMFV3);
    }
    
    /**
     * Gets the temperature at the remote controller 3 (Raumfernversteller 3), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRaumfernversteller3Temp() {
        return this.getRaumfernversteller3TempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_14_SOLARKOLLEKTOR}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSolarkollektorTempChannel() {
        return this.channel(ChannelId.IR_14_SOLARKOLLEKTOR);
    }
    
    /**
     * Gets the temperature of the solar collector (Solarkollektor), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSolarkollektorTemp() {
        return this.getSolarkollektorTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_15_SOLARSPEICHER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSolarspeicherTempChannel() {
        return this.channel(ChannelId.IR_15_SOLARSPEICHER);
    }
    
    /**
     * Gets the temperature of the solar collector buffer tank (Solarspeicher), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSolarspeicherTemp() {
        return this.getSolarspeicherTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_16_EXTEQ}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEnergiequelleExternTempChannel() {
        return this.channel(ChannelId.IR_16_EXTEQ);
    }
    
    /**
     * Gets the temperature of the external heat source (Externe Energiequelle), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEnergiequelleExternTemp() {
        return this.getEnergiequelleExternTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_17_ZULUFTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getZuluftTempChannel() {
        return this.channel(ChannelId.IR_17_ZULUFTTEMP);
    }
    
    /**
     * Gets the airflow inlet temperature (Zulufttemperatur), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getZuluftTemp() {
        return this.getZuluftTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_18_ABLUFTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAbluftTempChannel() {
        return this.channel(ChannelId.IR_18_ABLUFTTEMP);
    }
    
    /**
     * Gets the airflow outlet temperature (Ablufttemperatur), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAbluftTemp() {
        return this.getAbluftTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_19_ANSAUGTEMPVDICHTER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAnsaugTempVerdichterChannel() {
        return this.channel(ChannelId.IR_19_ANSAUGTEMPVDICHTER);
    }
    
    /**
     * Gets the compressor inlet temperature (Ansaugtemperatur Verdichter), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAnsaugTempVerdichter() {
        return this.getAnsaugTempVerdichterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20_ANSAUGTEMPVDAMPFER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAnsaugTempVerdampferChannel() {
        return this.channel(ChannelId.IR_20_ANSAUGTEMPVDAMPFER);
    }
    
    /**
     * Gets the evaporator inlet temperature (Ansaugtemperatur Verdampfer), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAnsaugTempVerdampfer() {
        return this.getAnsaugTempVerdampferChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_21_TEMPVDHEIZUNG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVerdichterHeizungTempChannel() {
        return this.channel(ChannelId.IR_21_TEMPVDHEIZUNG);
    }
    
    /**
     * Gets the compressor heater temperature (Temperatur Verdichterheizung), unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVerdichterHeizungTemp() {
        return this.getVerdichterHeizungTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_22_UEBERHITZ}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getUeberhitzungChannel() {
        return this.channel(ChannelId.IR_22_UEBERHITZ);
    }
    
    /**
     * Gets the overheating (Überhitzung), unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getUeberhitzung() {
        return this.getUeberhitzungChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_23_UEBERHITZSOLL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getUeberhitzungSollChannel() {
        return this.channel(ChannelId.IR_23_UEBERHITZSOLL);
    }
    
    /**
     * Gets the overheating set point (Überhitzung Soll), unit is decimal degree Kelvin.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getUeberhitzungSoll() {
        return this.getUeberhitzungSollChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_24_RBERAUMTEMPIST}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRbeRaumtempIstChannel() {
        return this.channel(ChannelId.IR_24_RBERAUMTEMPIST);
    }
    
    /**
     * Gets the measured temperature at the room temperature control panel (RBE, Raumbedieneinheit Raumtemperatur Ist),
     * unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRbeRaumtempIst() {
        return this.getRbeRaumtempIstChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_25_RBERAUMTEMPSOLL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRbeRaumtempSollChannel() {
        return this.channel(ChannelId.IR_25_RBERAUMTEMPSOLL);
    }
    
    /**
     * Gets the temperature set point at the room temperature control panel (RBE, Raumbedieneinheit Raumtemperatur Ist),
     * unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRbeRaumtempSoll() {
        return this.getRbeRaumtempSollChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_26_DRUCKHD}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getDruckHdChannel() {
        return this.channel(ChannelId.IR_26_DRUCKHD);
    }
    
    /**
     * Gets the high pressure reading (Druck HD, Hochdruck), unit is centibar.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getDruckHD() {
        return this.getDruckHdChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_27_DRUCKND}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getDruckNdChannel() {
        return this.channel(ChannelId.IR_27_DRUCKND);
    }
    
    /**
     * Gets the low pressure reading (Druck ND, Niederdruck), unit is centibar.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getDruckND() {
        return this.getDruckNdChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_28_TVD1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursVD1Channel() {
        return this.channel(ChannelId.IR_28_TVD1);
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
     * Gets the Channel for {@link ChannelId#IR_29_TVD2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursVD2Channel() {
        return this.channel(ChannelId.IR_29_TVD2);
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
     * Gets the Channel for {@link ChannelId#IR_30_TZWE1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe1Channel() {
        return this.channel(ChannelId.IR_30_TZWE1);
    }
    
    /**
     * Gets the operating hours of fallback heater 1 (Betriebsstunden ZWE1, Zusätzlicher Wärmeerzeuger).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe1() {
        return this.getHoursZwe1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_31_TZWE2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe2Channel() {
        return this.channel(ChannelId.IR_31_TZWE2);
    }
    
    /**
     * Gets the operating hours of fallback heater 2 (Betriebsstunden ZWE2, Zusätzlicher Wärmeerzeuger).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe2() {
        return this.getHoursZwe2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_32_TZWE3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursZwe3Channel() {
        return this.channel(ChannelId.IR_32_TZWE3);
    }
    
    /**
     * Gets the operating hours of fallback heater 3 (Betriebsstunden ZWE3, Zusätzlicher Wärmeerzeuger).
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursZwe3() {
        return this.getHoursZwe3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_33_TWAERMEPUMPE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursWaermepumpeChannel() {
        return this.channel(ChannelId.IR_33_TWAERMEPUMPE);
    }
    
    /**
     * Gets the operating hours of the heat pump (Betriebsstunden Wärmepumpe).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursWaermepumpe() {
        return this.getHoursWaermepumpeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_34_THEIZUNG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursHeizungChannel() {
        return this.channel(ChannelId.IR_34_THEIZUNG);
    }
    
    /**
     * Gets the operating hours of room heating (Betriebsstunden Heizung).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursHeizung() {
        return this.getHoursHeizungChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_35_TTRINKWW}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursTrinkWwChannel() {
        return this.channel(ChannelId.IR_35_TTRINKWW);
    }
    
    /**
     * Gets the operating hours of tap water heating (Betriebsstunden Trinkwarmwasser).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHoursTrinkWW() {
        return this.getHoursTrinkWwChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_36_TSWOPV}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHoursSwoPvChannel() {
        return this.channel(ChannelId.IR_36_TSWOPV);
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
     * Gets the current operating state of the heat pump (Anlagenstatus).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 7
     *      <li> State 0: Heizbetrieb / Heating
     *      <li> State 1: Trinkwarmwasser / Heating potable water
     *      <li> State 2: Schwimmbad / Swimming pool
     *      <li> State 3: EVU-Sperre / Forced off by energy supplier
     *      <li> State 4: Abtauen / Defrost
     *      <li> State 5: Off
     *      <li> State 6: Externe Energiequelle / External energy source
     *      <li> State 7: Kühlung / Cooling
     * </ul>
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatpumpOperatingMode() {
        return this.getHeatpumpOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_38_WHHEIZUNG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountHeizungChannel() {
        return this.channel(ChannelId.IR_38_WHHEIZUNG);
    }
    
    /**
     * Gets the amount of heat supplied to room heating (Wärmemenge Heizung), unit is hectowatt hours (kWh * 10E-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountHeizung() {
        return this.getHeatAmountHeizungChannel().value();
    }


    /**
     * Gets the Channel for {@link ChannelId#IR_40_WHTRINKWW}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountTrinkWwChannel() {
        return this.channel(ChannelId.IR_40_WHTRINKWW);
    }
    
    /**
     * Gets the amount of heat supplied to tap water heating (Wärmemenge Trinkwarmwasser),
     * unit is hectowatt hours (kWh * 10E-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountTrinkWW() {
        return this.getHeatAmountTrinkWwChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_42_WHPOOL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountPoolChannel() {
        return this.channel(ChannelId.IR_42_WHPOOL);
    }
    
    /**
     * Gets the amount of heat supplied to pool heating (Wärmemenge Schwimmbad), unit is hectowatt hours (kWh * 10E-1).
     * Optional, depends on heat pump model if available.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatAmountPool() {
        return this.getHeatAmountPoolChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_44_WHTOTAL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatAmountAllChannel() {
        return this.channel(ChannelId.IR_44_WHTOTAL);
    }
    
    /**
     * Gets the total amount of heat supplied (Wärmemenge gesamt), unit is hectowatt hours (kWh * 10E-1).
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
     * Get HUP (Heizung + Brauchwasser Umwälzpumpe) force on status.
	 * See {@link ChannelId#COIL_2_HUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnHup() {
		return this.getForceOnHupChannel().value();
	}
	
	/**
     * HUP (Heizung + Brauchwasser Umwälzpumpe), force on.
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
     * Get ZUP (Zusatz-Umwälzpumpe) force on status.
	 * See {@link ChannelId#COIL_4_ZUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnZup() {
		return this.getForceOnZupChannel().value();
	}
	
	/**
     * ZUP (Zusatz-Umwälzpumpe), force on.
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
     * Get BUP (Trinkwarmwasser-Umwälzpumpe) force on status.
	 * See {@link ChannelId#COIL_5_BUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnBup() {
		return this.getForceOnBupChannel().value();
	}
	
	/**
     * BUP (Trinkwarmwasser-Umwälzpumpe), force on.
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
     * Get BOSUP (Brunnen oder Sole-Umwälzpumpe) force on status.
	 * See {@link ChannelId#COIL_6_BOSUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnBosup() {
		return this.getForceOnBosupChannel().value();
	}
	
	/**
     * BOSUP (Brunnen oder Sole-Umwälzpumpe), force on.
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
     * Get ZIP (Zirkulationspumpe) force on status.
	 * See {@link ChannelId#COIL_7_ZIP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnZip() {
		return this.getForceOnZipChannel().value();
	}
	
	/**
     * ZIP (Zirkulationspumpe), force on.
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
     * Get FUP2 (Fußbodenheizungs-Umwälzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_8_FUP2}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFup2() {
		return this.getForceOnFup2Channel().value();
	}
	
	/**
     * FUP2 (Fußbodenheizungs-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
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
     * Get FUP3 (Fußbodenheizungs-Umwälzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_9_FUP3}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFup3() {
		return this.getForceOnFup3Channel().value();
	}
	
	/**
     * FUP3 (Fußbodenheizungs-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
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
     * Get SLP (Solar-Ladepumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_10_SLP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnSlp() {
		return this.getForceOnSlpChannel().value();
	}
	
	/**
     * SLP (Solar-Ladepumpe), force on. Optional, depends on heat pump model if available.
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
     * Get SUP (Schwimmbad-Umwälzpumpe) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_11_SUP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnSup() {
		return this.getForceOnSupChannel().value();
	}
	
	/**
     * SUP (Schwimmbad-Umwälzpumpe), force on. Optional, depends on heat pump model if available.
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
     * Get FRH (Schütz Defrostheizung) force on status. Optional, depends on heat pump model if available.
	 * See {@link ChannelId#COIL_13_FRH}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getForceOnFrh() {
		return this.getForceOnFrhChannel().value();
	}
	
	/**
     * FRH (Schütz Defrostheizung), force on. Optional, depends on heat pump model if available.
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
     * Get outside temperature, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getOutsideTemp() {
		return this.getOutsideTempChannel().value();
	}
	
	/**
     * Set outside temperature, unit is decimal degree Celsius. Minimum -200, maximum 800.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutsideTemp(Integer value) throws OpenemsNamedException {
		this.getOutsideTempChannel().setNextWriteValue(value);
	}
	
	/**
     * Set outside temperature, unit is decimal degree Celsius. Minimum -200, maximum 800.
	 * See {@link ChannelId#HR_0_OUTSIDETEMP}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutsideTemp(int value) throws OpenemsNamedException {
		this.getOutsideTempChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_1_RUECKTEMPSOLL}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getReturnTempSetpointChannel() {
        return this.channel(ChannelId.HR_1_RUECKTEMPSOLL);
    }

    /**
     * Get return temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_1_RUECKTEMPSOLL}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getReturnTempSetpoint() {
		return this.getReturnTempSetpointChannel().value();
	}
	
	/**
     * Set return temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_1_RUECKTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReturnTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getReturnTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set return temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_1_RUECKTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReturnTempSetpoint(int value) throws OpenemsNamedException {
		this.getReturnTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_2_MK1VORTEMPSOLL}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit1FlowTempSetpointChannel() {
        return this.channel(ChannelId.HR_2_MK1VORTEMPSOLL);
    }

    /**
     * Get heating circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_2_MK1VORTEMPSOLL}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getCircuit1FlowTempSetpoint() {
		return this.getCircuit1FlowTempSetpointChannel().value();
	}
	
	/**
     * Set heating circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_2_MK1VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit1FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit1FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating circuit 1 (Mischkreis 1) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_2_MK1VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit1FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit1FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_3_MK2VORTEMPSOLL}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit2FlowTempSetpointChannel() {
	    return this.channel(ChannelId.HR_3_MK2VORTEMPSOLL);
	}

	/**
     * Get heating circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_3_MK2VORTEMPSOLL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit2FlowTempSetpoint() {
		return this.getCircuit2FlowTempSetpointChannel().value();
	}
	
	/**
     * Set heating circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_3_MK2VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit2FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating circuit 2 (Mischkreis 2) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_3_MK2VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit2FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit2FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_4_MK3VORTEMPSOLL}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCircuit3FlowTempSetpointChannel() {
	    return this.channel(ChannelId.HR_4_MK3VORTEMPSOLL);
	}

	/**
     * Get heating circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_4_MK3VORTEMPSOLL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCircuit3FlowTempSetpoint() {
		return this.getCircuit3FlowTempSetpointChannel().value();
	}
	
	/**
     * Set heating circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_4_MK3VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3FlowTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getCircuit3FlowTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating circuit 3 (Mischkreis 3) flow temperature set point, unit is decimal degree Celsius.
     * Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_4_MK3VORTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCircuit3FlowTempSetpoint(int value) throws OpenemsNamedException {
		this.getCircuit3FlowTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_5_TRINKWWTEMPSOLL}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getWarmWaterTempSetpointChannel() {
	    return this.channel(ChannelId.HR_5_TRINKWWTEMPSOLL);
	}

	/**
     * Get warm water (Trinkwarmwasse) temperature set point, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_5_TRINKWWTEMPSOLL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWarmWaterTempSetpoint() {
		return this.getWarmWaterTempSetpointChannel().value();
	}
	
	/**
     * Set warm water (Trinkwarmwasse) temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_5_TRINKWWTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setWarmWaterTempSetpoint(Integer value) throws OpenemsNamedException {
		this.getWarmWaterTempSetpointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set warm water (Trinkwarmwasse) temperature set point, unit is decimal degree Celsius. Minimum 150, maximum 800.
	 * See {@link ChannelId#HR_5_TRINKWWTEMPSOLL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setWarmWaterTempSetpoint(int value) throws OpenemsNamedException {
		this.getWarmWaterTempSetpointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_6_RUNCLEARANCE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getRunClearanceChannel() {
	    return this.channel(ChannelId.HR_6_RUNCLEARANCE);
	}

	/**
     * Get heat pump run clearance (Sperre / Freigabe Wärmepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Sperre / Off
     *      <li> State 1: Freigabe 1 Verdichter / Clearance 1 compressor
     *      <li> State 2: Freigabe 2 Verdichter / Clearance 2 compressors
     * </ul>
	 * See {@link ChannelId#HR_6_RUNCLEARANCE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRunClearance() {
		return this.getRunClearanceChannel().value();
	}
	
	/**
     * Set heat pump run clearance (Sperre / Freigabe Wärmepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Sperre / Off
     *      <li> State 1: Freigabe 1 Verdichter / Clearance 1 compressor
     *      <li> State 2: Freigabe 2 Verdichter / Clearance 2 compressors
     * </ul>
	 * See {@link ChannelId#HR_6_RUNCLEARANCE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRunClearance(Integer value) throws OpenemsNamedException {
		this.getRunClearanceChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heat pump run clearance (Sperre / Freigabe Wärmepumpe).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Sperre / Off
     *      <li> State 1: Freigabe 1 Verdichter / Clearance 1 compressor
     *      <li> State 2: Freigabe 2 Verdichter / Clearance 2 compressors
     * </ul>
	 * See {@link ChannelId#HR_6_RUNCLEARANCE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRunClearance(int value) throws OpenemsNamedException {
		this.getRunClearanceChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_7_HEIZUNGRUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingOperationModeChannel() {
	    return this.channel(ChannelId.HR_7_HEIZUNGRUNSTATE);
	}

	/**
     * Get heating operating mode (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_HEIZUNGRUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingOperationMode() {
		return this.getHeatingOperationModeChannel().value();
	}
	
	/**
     * Set heating operating mode (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_HEIZUNGRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingOperationMode(Integer value) throws OpenemsNamedException {
		this.getHeatingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating operating mode (Betriebsart Heizung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_7_HEIZUNGRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingOperationMode(int value) throws OpenemsNamedException {
		this.getHeatingOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_8_TRINKWWRUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getWarmWaterOperationModeChannel() {
        return this.channel(ChannelId.HR_8_TRINKWWRUNSTATE);
    }

    /**
     * Get warm water operating mode (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_TRINKWWRUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getWarmWaterOperationMode() {
		return this.getWarmWaterOperationModeChannel().value();
	}
	
	/**
     * Set warm water operating mode (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_TRINKWWRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void getWarmWaterOperationMode(Integer value) throws OpenemsNamedException {
		this.getWarmWaterOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set warm water operating mode (Betriebsart Trinkwarmwasser).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_8_TRINKWWRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void getWarmWaterOperationMode(int value) throws OpenemsNamedException {
		this.getWarmWaterOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_9_MK2RUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCircuit2OperationModeChannel() {
	    return this.channel(ChannelId.HR_9_MK2RUNSTATE);
	}

	/**
     * Get heating circuit 2 operating mode (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MK2RUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCircuit2OperationMode() {
		return this.getHeatingCircuit2OperationModeChannel().value();
	}
	
	/**
     * Set heating circuit 2 operating mode (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MK2RUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCircuit2OperationMode(Integer value) throws OpenemsNamedException {
		this.getHeatingCircuit2OperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating circuit 2 operating mode (Betriebsart Mischkreis 2).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_9_MK2RUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCircuit2OperationMode(int value) throws OpenemsNamedException {
		this.getHeatingCircuit2OperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_10_MK3RUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCircuit3OperationModeChannel() {
	    return this.channel(ChannelId.HR_10_MK3RUNSTATE);
	}

	/**
     * Get heating circuit 3 operating mode (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MK3RUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCircuit3OperationMode() {
		return this.getHeatingCircuit3OperationModeChannel().value();
	}
	
	/**
     * Set heating circuit 3 operating mode (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MK3RUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCircuit3OperationMode(Integer value) throws OpenemsNamedException {
		this.getHeatingCircuit3OperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating circuit 3 operating mode (Betriebsart Mischkreis 3).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Zusätzlicher Wärmeerzeuger / Additional heater
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_10_MK3RUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCircuit3OperationMode(int value) throws OpenemsNamedException {
		this.getHeatingCircuit3OperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_11_COOLINGRUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getCoolingOperationModeChannel() {
	    return this.channel(ChannelId.HR_11_COOLINGRUNSTATE);
	}

	/**
     * Get cooling operating mode (Betriebsart Kühlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: Automatik
     * </ul>
	 * See {@link ChannelId#HR_11_COOLINGRUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCoolingOperationMode() {
		return this.getCoolingOperationModeChannel().value();
	}
	
	/**
     * Set cooling operating mode (Betriebsart Kühlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: Automatik
     * </ul>
	 * See {@link ChannelId#HR_11_COOLINGRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCoolingOperationMode(Integer value) throws OpenemsNamedException {
		this.getCoolingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set cooling operating mode (Betriebsart Kühlung).
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 1
     *      <li> State 0: Off
     *      <li> State 1: Automatik
     * </ul>
	 * See {@link ChannelId#HR_11_COOLINGRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCoolingOperationMode(int value) throws OpenemsNamedException {
		this.getCoolingOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_12_VENTILATIONRUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getVentilationOperationModeChannel() {
	    return this.channel(ChannelId.HR_12_VENTILATIONRUNSTATE);
	}

	/**
     * Get ventilation operating mode (Betriebsart Lüftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 3
     *      <li> State 0: Automatik
     *      <li> State 1: Party / No late night throttling
     *      <li> State 2: Ferien / Vacation, full time throttling
     *      <li> State 3: Off
     * </ul>>
	 * See {@link ChannelId#HR_12_VENTILATIONRUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVentilationOperationMode() {
		return this.getVentilationOperationModeChannel().value();
	}
	
	/**
     * Set ventilation operating mode (Betriebsart Lüftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 3
     *      <li> State 0: Automatik
     *      <li> State 1: Party / No late night throttling
     *      <li> State 2: Ferien / Vacation, full time throttling
     *      <li> State 3: Off
     * </ul>
	 * See {@link ChannelId#HR_12_VENTILATIONRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationOperationMode(Integer value) throws OpenemsNamedException {
		this.getVentilationOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set ventilation operating mode (Betriebsart Lüftung).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 3
     *      <li> State 0: Automatik
     *      <li> State 1: Party / No late night throttling
     *      <li> State 2: Ferien / Vacation, full time throttling
     *      <li> State 3: Off
     * </ul>
	 * See {@link ChannelId#HR_12_VENTILATIONRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationOperationMode(int value) throws OpenemsNamedException {
		this.getVentilationOperationModeChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_13_POOLRUNSTATE}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getPoolHeatingOperationModeChannel() {
	    return this.channel(ChannelId.HR_13_POOLRUNSTATE);
	}

	/**
     * Get pool heating operating mode (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Wert nicht benutzt / Value not in use
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOLRUNSTATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPoolHeatingOperationMode() {
		return this.getPoolHeatingOperationModeChannel().value();
	}
	
	/**
     * Set pool heating operating mode (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Wert nicht benutzt / Value not in use
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOLRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPoolHeatingOperationMode(Integer value) throws OpenemsNamedException {
		this.getPoolHeatingOperationModeChannel().setNextWriteValue(value);
	}
	
	/**
     * Set pool heating operating mode (Betriebsart Schwimmbad).
     * Optional, depends on heat pump model if available.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Automatik
     *      <li> State 1: Wert nicht benutzt / Value not in use
     *      <li> State 2: Party / No late night throttling
     *      <li> State 3: Ferien / Vacation, full time throttling
     *      <li> State 4: Off
     * </ul>
	 * See {@link ChannelId#HR_13_POOLRUNSTATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPoolHeatingOperationMode(int value) throws OpenemsNamedException {
		this.getPoolHeatingOperationModeChannel().setNextWriteValue(value);
	}

	// HR_14 = smart grid, use HeatpumpSmartGridGeneralizedChannel

    /**
     * Gets the Channel for {@link ChannelId#HR_15_HKHEIZUNGENDPKT}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveEndPointChannel() {
	    return this.channel(ChannelId.HR_15_HKHEIZUNGENDPKT);
	}

	/**
     * Get heating curve end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_15_HKHEIZUNGENDPKT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveEndPoint() {
		return this.getHeatingCurveEndPointChannel().value();
	}
	
	/**
     * Set heating curve end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_15_HKHEIZUNGENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveEndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveEndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve end point (Heizkurve Heizung Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_15_HKHEIZUNGENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveEndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveEndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_16_HKHEIZUNGPARAVER}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveParallelTranslationChannel() {
	    return this.channel(ChannelId.HR_16_HKHEIZUNGPARAVER);
	}

	/**
     * Get heating curve parallel translation (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_16_HKHEIZUNGPARAVER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveParallelTranslation() {
		return this.getHeatingCurveParallelTranslationChannel().value();
	}
	
	/**
     * Set heating curve parallel translation (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
     * Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_16_HKHEIZUNGPARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveParallelTranslation(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveParallelTranslationChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve parallel translation (Heizkurve Heizung Parallelverschiebung), unit is decimal degree Celsius.
     * Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_16_HKHEIZUNGPARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveParallelTranslation(int value) throws OpenemsNamedException {
		this.getHeatingCurveParallelTranslationChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_17_HKMK1ENDPKT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit1EndPointChannel() {
	    return this.channel(ChannelId.HR_17_HKMK1ENDPKT);
	}

	/**
     * Get heating curve heating circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_17_HKMK1ENDPKT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit1EndPoint() {
		return this.getHeatingCurveCircuit1EndPointChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_17_HKMK1ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 1 end point (Heizkurve Mischkreis 1 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
	 * See {@link ChannelId#HR_17_HKMK1ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_18_HKMK1PARAVER}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit1ParallelTranslationChannel() {
	    return this.channel(ChannelId.HR_18_HKMK1PARAVER);
	}

	/**
     * Get heating curve heating circuit 1 parallel translation (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius.
	 * See {@link ChannelId#HR_18_HKMK1PARAVER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit1ParallelTranslation() {
		return this.getHeatingCurveCircuit1ParallelTranslationChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 1 parallel translation (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_18_HKMK1PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1ParallelTranslation(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1ParallelTranslationChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 1 parallel translation (Heizkurve Mischkreis 1 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
	 * See {@link ChannelId#HR_18_HKMK1PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit1ParallelTranslation(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit1ParallelTranslationChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_19_HKMK2ENDPKT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit2EndPointChannel() {
	    return this.channel(ChannelId.HR_19_HKMK2ENDPKT);
	}

	/**
     * Get heating curve heating circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_HKMK2ENDPKT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit2EndPoint() {
		return this.getHeatingCurveCircuit2EndPointChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_HKMK2ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 2 end point (Heizkurve Mischkreis 2 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_19_HKMK2ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_20_HKMK2PARAVER}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit2ParallelTranslationChannel() {
	    return this.channel(ChannelId.HR_20_HKMK2PARAVER);
	}

	/**
     * Get heating curve heating circuit 2 parallel translation (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_HKMK2PARAVER}
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit2ParallelTranslation() {
		return this.getHeatingCurveCircuit2ParallelTranslationChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 2 parallel translation (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_HKMK2PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2ParallelTranslation(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2ParallelTranslationChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 2 parallel translation (Heizkurve Mischkreis 2 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_20_HKMK2PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit2ParallelTranslation(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit2ParallelTranslationChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_21_HKMK3ENDPKT}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit3EndPointChannel() {
	    return this.channel(ChannelId.HR_21_HKMK3ENDPKT);
	}

	/**
     * Get heating curve heating circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_HKMK3ENDPKT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit3EndPoint() {
		return this.getHeatingCurveCircuit3EndPointChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_HKMK3ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3EndPoint(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3EndPointChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 3 end point (Heizkurve Mischkreis 3 Endpunkt), unit is decimal degree Celsius.
     * Minimum 200, maximum 700.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_21_HKMK3ENDPKT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3EndPoint(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3EndPointChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_22_HKMK3PARAVER}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getHeatingCurveCircuit3ParallelTranslationChannel() {
	    return this.channel(ChannelId.HR_22_HKMK3PARAVER);
	}

	/**
     * Get heating curve heating circuit 3 parallel translation (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_HKMK3PARAVER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveCircuit3ParallelTranslation() {
		return this.getHeatingCurveCircuit3ParallelTranslationChannel().value();
	}
	
	/**
     * Set heating curve heating circuit 3 parallel translation (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_HKMK3PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3ParallelTranslation(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3ParallelTranslationChannel().setNextWriteValue(value);
	}
	
	/**
     * Set heating curve heating circuit 3 parallel translation (Heizkurve Mischkreis 3 Parallelverschiebung), unit is
     * decimal degree Celsius. Minimum 50, maximum 350.
     * Optional, depends on heat pump model if available.
	 * See {@link ChannelId#HR_22_HKMK3PARAVER}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveCircuit3ParallelTranslation(int value) throws OpenemsNamedException {
		this.getHeatingCurveCircuit3ParallelTranslationChannel().setNextWriteValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HR_23_TEMPPM}.
     *
     * @return the Channel
     */
	public default IntegerWriteChannel getTempPlusMinusChannel() {
	    return this.channel(ChannelId.HR_23_TEMPPM);
	}

	/**
     * Get temperature +-, unit is decimal degree Celsius.
	 * See {@link ChannelId#HR_23_TEMPPM}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTempPlusMinus() {
		return this.getTempPlusMinusChannel().value();
	}
	
	/**
     * Set temperature +-, unit is decimal degree Celsius. Minimum -50, maximum 50.
	 * See {@link ChannelId#HR_23_TEMPPM}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTempPlusMinus(Integer value) throws OpenemsNamedException {
		this.getTempPlusMinusChannel().setNextWriteValue(value);
	}
	
	/**
     * Set temperature +-, unit is decimal degree Celsius. Minimum -50, maximum 50.
	 * See {@link ChannelId#HR_23_TEMPPM}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTempPlusMinus(int value) throws OpenemsNamedException {
		this.getTempPlusMinusChannel().setNextWriteValue(value);
	}
}
