package io.openems.edge.heater.gasboiler.viessmann;

public enum ErrorList {

    STANDARD_ERRORS(new String[]{
            "01",
            "02",
            "03",
            "04",
            "05",
            "06",
            "07",
            "08",
            "09",
            "0A",
            "0B",
            "0C",
            "0D",
            "0E",
            "0F Maintenance needed (Wartung durchfuehren)",
            "10 Short circuit in outside temperature sensor (Kurzschluss Aussentemperatursensor)",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18 No connection to outside temperature sensor (Unterbrechung Aussentemperatursensor)",
            "19 Connection error to outside temperature sensor (Verbindungsfehler Aussentemperaturfuehler)",
            "1A",
            "1B",
            "1C",
            "1D Percolation sensor communication error (Stoerung Volumenstromsensor Kommunikationsfehler)",
            "1E Percolation sensor error 1 (Stoerung Volumenstromsensor Fehler 1)",
            "1F Percolation sensor error 2 (Stoerung Volumenstromsensor Fehler 2)",
            "20 Short circuit flow temperature heating circuit 1 (Kurzschluss Vorlauftemp. HK1 / Anlage)",
            "21 Temperature error solar circuit (Fehler Temperaturwerte Solarkreislauf)",
            "22 Temperature error storage tank (Fehler Speichertemperaturwerte)",
            "23 Error in state of charge calculation (Fehler Ladezustandsberechnung)",
            "24 Error in circulation detected (Fehler Zirkulation detektiert)",
            "25 Error in shifting detected (Fehler Umschichtung detektiert)",
            "26 Error actor signal (Fehler Aktor Signale)",
            "27 Error in measured percolation (Fehler Volumenstromwert)",
            "28 No connection flow temperature heating circuit 1 (Unterbrechung Vorlauftemp. HK1 / Anlage)",
            "29 Signal error electric heating inserts (Fehler Signal Elektroheizeinsaetze)",
            "2A Inversion detected (Inversion detektiert)",
            "2B",
            "2C",
            "2D",
            "2E",
            "2F",
            "30 Short circuit boiler temperature sensor (Kurzschluss Kesseltemperatursensor)",
            "31",
            "32",
            "33",
            "34",
            "35",
            "36",
            "37",
            "38 No connection boiler temperature sensor (Unterbrechung Kesseltemperatursensor)",
            "39",
            "3A LAN hardware error (LAN Hardware Fehler)",
            "3B LAN system error (LAN System Fehler)",
            "3C LAN DHCP-server not answering (LAN DHCP-Server antwortet nicht)",
            "3D LAN ethernet not connected (LAN Ethernet-Leitung nicht verbunden)",
            "3E LAN broker connection error (LAN Broker Verbindungsfehler)",
            "3F LAN update error (LAN Update Fehler)",
            "40 Short circuit flow temperature heating circuit 2 (Kurzschluss Vorlauftemp. HK2)",
            "41 Short circuit return temperature heating circuit 2 (Kurzschluss Ruecklauftemp. HK2)",
            "42",
            "43",
            "44 Short circuit flow temperature heating circuit 3 (Kurzschluss Vorlauftemp. HK3)",
            "45 Short circuit return temperature heating circuit 3 (Kurzschluss Ruecklauftemp. HK3)",
            "46",
            "47",
            "48 No connection flow temperature heating circuit 2 (Unterbrechung Vorlauftemp. HK2)",
            "49 No connection return temperature heating circuit 2 (Unterbrechung Ruecklauftemp. HK 2)",
            "5A",
            "5B",
            "4C No connection flow temperature heating circuit 3 (Unterbrechung Vorlauftemp. HK3)",
            "4D No connection flow temperature heating circuit 3 (Unterbrechung Ruecklauftemp. HK3)",
            "4E",
            "4F",
            "50 Short circuit storage tank temperature sensor 1 (Kurzschluss Speichertemperatursensor 1)",
            "51 Short circuit storage tank temperature sensor 2 (Kurzschluss Speichertemperatursensor 2)",
            "52 Short circuit buffer tank temperature sensor (Kurzschluss Puffertemperatursensor)",
            "53",
            "54 Error boiler 5 (Fehler Kessel 5)",
            "55 Error boiler 6 (Fehler Kessel 6)",
            "56 Error boiler 7 (Fehler Kessel 7)",
            "57 Error boiler 8 (Fehler Kessel 8)",
            "58 No connection storage tank temperature sensor 1 (Unterbrechung Speichertemperatursensor 1)",
            "59 No connection storage tank temperature sensor 2 (Unterbrechung Speichertemperatursensor 2)",
            "5A No connection buffer tank temperature sensor / flow temperature sensor hydraulic switch (Unterbr. Puffersensor/Vorlauftemperaturs. Hydr. Weiche)",
            "5B",
            "5C Outage boiler 5 (Ausfall-Kessel 5)",
            "5D Outage boiler 6 (Ausfall-Kessel 6)",
            "5E Outage boiler 7 (Ausfall-Kessel 7)",
            "5F Outage boiler 8 (Ausfall-Kessel 8)",
            "60 Short circuit temperature sensor 17A (Kurzschluss Temperatursensor 17A)",
            "61",
            "62",
            "63",
            "64",
            "65",
            "66",
            "67",
            "68 No connection temperature sensor 17A (Unterbrechung Temperatursensor 17A)",
            "69",
            "6A",
            "6B",
            "6C",
            "6D",
            "6E",
            "6F",
            "70 Short circuit temperature sensor 17B (Kurzschluss Temperatursensor 17B)",
            "71",
            "72",
            "73",
            "74 Short circuit sensor 1 pump module PM1 (Kurzschluss Sensor 1 Pumpenmodul PM1)",
            "75 Short circuit sensor 2 pump module PM1 (Kurzschluss Sensor 2 Pumpenmodul PM1)",
            "76 Short circuit sensor 3 pump module PM1 (Kurzschluss Sensor 3 Pumpenmodul PM1)",
            "77 Short circuit sensor 4 pump module PM1 (Kurzschluss Sensor 4 Pumpenmodul PM1)",
            "78 No connection temperature sensor 17B (Unterbrechung Temperatursensor 17B)",
            "79",
            "7A",
            "7B",
            "7C No connection sensor 1 pump module PM1 (Unterbrechung Sensor 1 Pumpenmodul PM1)",
            "7D No connection sensor 2 pump module PM1 (Unterbrechung Sensor 2 Pumpenmodul PM1)",
            "7E No connection sensor 3 pump module PM1 (Unterbrechung Sensor 3 Pumpenmodul PM1)",
            "7F No connection sensor 4 pump module PM1 (Unterbrechung Sensor 4 Pumpenmodul PM1)",
            "80 Short circuit boiler temperature limit sensor (Kurzschluss Sicherheits-Kesseltemperaturbegrenzer (SKTS))",
            "81 Sensor drift boiler temperature limit sensor (Sensordrift Sicherheits-Kesseltemperaturbegrenzer (SKTS))",
            "82 Short circuit exhaust temperature limit sensor (Kurzschluss Sicherheits-Abastemperaturbegrenzer (SAGTS))",
            "83 Sensor drift exhaust temperature limit sensor (Sensordrift Sicherheits-Abastemperaturbegrenzer (SAGTS))",
            "84 Error boiler 1 (Fehler Kessel 1)",
            "85 Error boiler 2 (Fehler Kessel 2)",
            "86 Error boiler 3 (Fehler Kessel 3)",
            "87 Error boiler 4 (Fehler Kessel 4)",
            "88 No connection boiler temperature limit sensor (Unterbrechung Sicherheits-Kesseltemperaturbegrenzer (SKTS))",
            "89 No connection exhaust temperature limit sensor (Unterbrechung Sicherheits-Abastemperaturbegrenzer (SAGTS))",
            "8A",
            "8B",
            "8C Outage boiler 1 (Ausfall-Kessel 1)",
            "8D Outage boiler 2 (Ausfall-Kessel 2)",
            "8E Outage boiler 3 (Ausfall-Kessel 3)",
            "8F Outage boiler 4 (Ausfall-Kessel 4)",
            "90 Solar: short circuit sensor 7 (Solar: Kurzschluss Sensor 7)",
            "91 Solar: short circuit sensor 10 (Solar: Kurzschluss Sensor 10)",
            "92 Solar: Short circuit collector temperature sensor (Solar: Kurzschluss Kollektortemperatursensor)",
            "93 Solar: Short circuit collector return temperature sensor S3 (Solar: Kurzschluss Kollektorruecklauftemperatursensor S3)",
            "94 Solar: Short circuit storage tank temperature sensor (Solar: Kurzschluss Speichertemperatursensorsensor)",
            "95",
            "96",
            "97",
            "98 Solar: no connection sensor 7 (Solar: Unterbrechung Sensor 7)",
            "99 Solar: no connection sensor 10 (Solar: Unterbrechung Sensor 10)",
            "9A Solar: no connection collector temperature sensor (Solar: Unterbrechung Kollektortemperatursensor)",
            "9B Solar: no connection collector return temperature sensor S3 (Solar: Unterbrechung Kollektorruecklauftemperatursensor S3)",
            "9C Solar: no connection storage tank temperature sensor (Solar: Unterbrechung Speichertemperatursensor)",
            "9D",
            "9E Solar: delta-T surveillance (Solar: Delta-T Ueberwachung)",
            "9F Solar: general error (Solar: allgemeiner Fehler)",
            "A0 Error module 2: connector 1 (Stoermeldemodul 2: Eingang 1)",
            "A1 Error module 2: connector 2 (Stoermeldemodul 2: Eingang 2)",
            "A2 Error module 2: connector 3 / controller low water pressure (Stoermeldemodul 2: Eingang 3/niedriger Wasserdruck Regelung)",
            "A3 Error module 2: connector 4 / exhaust temperature sensor position incorrect (Stoermeldemodul 2: Eingang 4/Abgastemperaturs. nicht richtig po)",
            "A4 Error module 2: external error / device limit reached (Stoermeldemodul 2: externe Stoerung/Ueberschreitung Anlagenmaxi)",
            "A5 External error pump module PM1 (Externe Stoerung Pumpenmodul PM1)",
            "A6 Internal error pump module PM1 / error power grid anode (Interne Stoerung Pumpenmodul PM1/Fehler Fremdstromanode)",
            "A7 Error user interface time module (Fehler Bedienteil (Uhrenbaustein))",
            "A8 Error internal pump running dry (Fehler interne Pumpe meldet Luft)",
            "A9 Error internal pump blocked (Fehler interne Pumpe blockiert)",
            "AA Configuration error TSA-function (Konfigurationsfehler TSA-Funktion)",
            "AB Configuration error heat exchange set (Konfigurationsfehler Waermetauscherset)",
            "AC Configuration error return controller (Konfigurationsfehler Ruecklaufregelung)",
            "AD Configuration error throttle valve (Konfigurationsfehler Drosselklappe)",
            "AE Internal error mixer (Interner Fehler Mischer)",
            "AF Internal error mixer (Interner Fehler Mischer)",
            "B0 Short circuit exhaust temperature sensor (Kurzschluss Abgastemperatursensor)",
            "B1 Error user interface (Fehler Bedieneinheit)",
            "B2",
            "B3",
            "B4 Internal error ADC reference channel (Interner Fehler ADC (Referenzkanal))",
            "B5 Internal error EEPROM (Interner Fehler EEPROM)",
            "B6 Hardware recognition invalid (Hardwarekennung ungueltig)",
            "B7 Boiler protection code card invalid / faulty (Kesselschutzcodierkarte falsch/fehlerhaft)",
            "B8 No connection exhaust temperature sensor (Unterbrechung Abgastemperatursensor)",
            "B9 Faulty transmission of encoding connector data (Fehlerhafte Uebertragung Codiersteckerdaten)",
            "BA Communication error mixing module heating circuit 2 (Kommunikationsfehler Mischermodul HK2)",
            "BB Communication error mixing module heating circuit 3 (Kommunikationsfehler Mischermodul HK3)",
            "BC Communication error remote controller heating circuit 1 (Kommunikationsfehler Fernbedienung HK1)",
            "BD Communication error remote controller heating circuit 2 (Kommunikationsfehler Fernbedienung HK2)",
            "BE Communication error remote controller heating circuit 3 (Kommunikationsfehler Fernbedienung HK3)",
            "BF LON-module invalid / faulty (LON-Modul falsch/fehlerhaft)",
            "C0",
            "C1 External safety boiler 1 / communication error triggered (externe Sicherheitseinrichtung Kessel 1/Kommunikationsfehler An)",
            "C2 Communication error solar controller (Kommunikationsfehler Solarregelung)",
            "C3 Communication error connector extension AM1 (Kommunikationsfehler Anschlusserweiterung AM1)",
            "C4 Communication error connector extension 0-10V / OT / pump module (Kommunikationsfehler Anschlusserweiterung 0-10V/OT/Pumpenmodul)",
            "C5 Communication error KM-bus device DAP1 (Kommunikationsfehler KM-Bus Geraet DAP1/Kommunikationsfehler dr)",
            "C6 Communication error KM-bus device DAP2 (Kommunikationsfehler KM-Bus Geraet DAP2/Kommunikationsfehler dr)",
            "C7 Error module 1 external error / communication error rotation speed (Stoermeldem. 1 externe Stoerung/Kommunikationsfehler drehzahlge)",
            "C8 Error module 1 connector 1: water level limiter (Stoermeldemodul 1 Eingang 1: Wasserstandsbegrenzer/Kommunikatio)",
            "C9 Error module 1 connector 2: maximum pressure 1 (Stoermeldemodul 1 Eingang 2: Maximaldruck 1/Kommunikationsfehle)",
            "CA Error module 1 connector 3: minimal pressure / maximum pressure 2 (Stoermeldemodul 1 Eingang 3: Minimaldruck/Maximaldruck 2/Kommun)",
            "CB Error module 1 connector 4: maximum pressure 2 (Stoermeldemodul 1 Eingang 4: Maximaldruck 2)",
            "CC Reserved external devices (reserviert externe Peripherie)",
            "CD Communication error Vitocom 100 (Kommunikationsfehler Vitocom 100)",
            "CE Error module 1 communication error / connector extension (Kommunikationsfehler Stoermeldemodul 1/Anschlusserweiterung)",
            "CF Communication error LON-module / HV-module (Kommunikationsfehler LON-Modul/HV-Modul)",
            "D0",
            "D1 Error burner boiler 1 (Brennerstoerung Kessel 1)",
            "D2 Error module 2 communication error (Kommunikationsfehler Stoermeldemodul 2)",
            "D3 Communication error connector extension EA1 (Kommunikationsfehler Anschlusserweiterung EA1)",
            "D4 Temperature limiter boiler (Sicherheitstemperaturbegrenzer Kessel)",
            "D5 Cascade: boiler not responding (Kaskade: Kessel meldet sich nicht)",
            "D6 EA1: external error 1 (EA1: Externe Stoerung 1)",
            "D7 EA1: external error 2 (EA1: Externe Stoerung 2)",
            "D8 EA1: external error 3 (EA1: Externe Stoerung 3)",
            "D9",
            "DA Short circuit room temperature sensor heating circuit 1 (Kurzschluss Raumtemperatursensor HK 1)",
            "DB Short circuit room temperature sensor heating circuit 2 (Kurzschluss Raumtemperatursensor HK 2)",
            "DC Short circuit room temperature sensor heating circuit 3 (Kurzschluss Raumtemperatursensor HK 3)",
            "DD No connection room temperature sensor heating circuit 1 (Unterbrechung Raumtemperatursensor HK 1)",
            "DE No connection room temperature sensor heating circuit 2 (Unterbrechung Raumtemperatursensor HK 2)",
            "DF No connection room temperature sensor heating circuit 3 (Unterbrechung Raumtemperatursensor HK 3)",
            "E0 Error external participant LON (Fehler externer Teilnehmer LON)",
            "E1 Gas valve 1 leaking / gas pressure monitor 2 not opening (Gasventil 1 undicht/Gasdruckw. 2 oeffnet nicht/SCOT Kalibration)",
            "E2 Gas valve 2 leaking / gas pressure monitor 2 not closing (Gasventil 2 undicht/Gasdruckw. 2 schliesst nicht/keine Kalibrat)",
            "E3 Error security chain (Fehler Sicherheitskette/Kalibrationsfehler thermisch)",
            "E4 Error supply voltage (Fehler Versorgungsspannung/)",
            "E5 Error flame amplifier (Fehler Flammenverstaerker)",
            "E6 Minimum air / water pressure not reached / exhaust system / air supply (Minimalen Luft-/Wasserdruck nicht erreicht/Abgas-/ Zuluftsystem)",
            "E7 Short circuit boiler temperature sensor 2 (Kurzschluss Kesseltemperatursensor 2/SCOT Kalibrationswert Gren)",
            "E8 Sensor drift boiler temperature sensor 2 (Sensordrift Kesseltemperatursensor 2/SCOT Ionisationssignal wei)",
            "E9 No connection boiler temperature sensor 2 (Unterbrechung Kesseltemperatursensor 2)",
            "EA Error main valve (Fehler Hauptventil mehrere Gasarten/SCOT Kalibrationswert weich)",
            "EB Error: no run clearance for boiler by external contacts (Fehler: keine Freigabe Brenner ueber ext. Kontakt / SCOT Kalibr)",
            "EC Error security relay (Fehler Sicherheitsrelais/SCOT Ionisationssollwert fehlerhaft)",
            "ED Error ignition relay (Fehler Zuendrelais/SCOT Systemfehler)",
            "EE Error fuel relay 1 (Fehler Brennstoffrelais 1/keine Flammbildung)",
            "EF Error fuel relay 2 (Fehler Brennstoffrelais 2/Flammenausfall in Sicherheitszeit)",
            "F0 Communication error automatic burner (Kommunikationsfehler Feuerungsautomat)",
            "F1 Exhaust temperature limit triggered (Abgasuebertemperatur)",
            "F2 Boiler temperature limit triggered (Kesseluebertemperatur)",
            "F3 Unintentional flame / light detected (Fremdlichterkennung/Flamme vor Start)",
            "F4 No flame formation (keine Flammenbildung)",
            "F5 Air pressure monitor not closing (Luftdruckwaechter schliesst nicht)",
            "F6 Gas pressure monitor not closing (Gasdruckwaechter schliesst nicht)",
            "F7 Air pressure monitor not opening (Luftdruckwaechter oeffnet nicht)",
            "F8 Error in fuel valve response (Fehlerhafte Brennstoffventil Rueckmeldung)",
            "F9 Fan speed too low (Luefter Drehzahl nicht erreicht)",
            "FA Fan not stopping Luefter Stillstand nicht erreicht",
            "FB Incineration chamber pressure too high, chamber hatch not opening, condensate buildup (Feuerraumdruck, Brennerklappe oeffnet nicht, Kondensatstau/Flam)",
            "FC Error electric fan control (Luefter Fehler elektrische Ansteuerung)",
            "FD Internal error automatic burner (Interner Fehler Feuerungsautomat (Fehlerursache steht an))",
            "FE Encoding connector fauly or invalid (Codierstecker defekt oder falsch oder EMV-Stoerung)",
            "FF Internal error automatic burner (Interner Fehler Feuerungsautomat (Fehlerursache steht nicht meh))",
    });


    private final String[] errors;

    ErrorList(String[] errors) {
        this.errors = errors;
    }

    /**
     * Get the list of errors as a string array.
     *
     * @return the error list.
     */
    public String[] getErrorList() {
        return this.errors;
    }

}