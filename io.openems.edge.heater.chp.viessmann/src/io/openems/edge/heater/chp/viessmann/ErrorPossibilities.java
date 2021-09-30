package io.openems.edge.heater.chp.viessmann;

public enum ErrorPossibilities {
    STANDARD_ERRORS(new String[]{
            "Fan error (Luefter gestoert)",
            "Coolant pump error (Kuehlwasserpumpe gestoert)",
            "Exhaust counter pressure maximal (Abgasgegendruck max.)",
            "Feed-in switch (Einspeiseschalter)",
            "External error (Externe Stoerung)",
            "Engine speed over target (Ueberdrehzahl)",
            "Coolant temperature (Kuehlwassertemperatur)",
            "Exhaust temperature maximum (Abgastemperatur max.)",
            "",
            "Emergency stop (Not-Stopp)",
            "Oil level minimal (Oelstand min.)",
            "Coolant pressure minimal (Kuehlwasserdruck min.)",
            "Gas pressure minimal (Gasdruck min.)",
            "Safety temperature (Sicherheitstemperatur)",
            "Generator temperature (Generatortemperatur)",
            "Noise cover temperature (Schallhaubentemperatur)",
            "Activation error (Zuschaltung gestoert)",
            "Synchronisation error (Synchronisierung gestoert)",
            "Engine speed < 50 (Drehzahl < 50)",
            "Oil level maximum (Oelstand max.)",
            "Temperature Pt100_2 maximum (Temperatur Pt100_2 max.)",
            "Temperature Pt100_3 maximum (Temperatur Pt100_3 max.)",
            "Maximum power (Leistung max.)",
            "Return power (Rueckleistung)",
            "Exhaust temperature minimal (Abgastemperatur min.)",
            "Oil pressure minimal (Oeldruck min.)",
            "Gas pressure maximum (Gasdruck max.)",
            "Error in heating circuit pump (Heizwasserpumpe gestoert)",
            "Starter speed < 50 rpm (Anlassdrehzahl < 50 Upm)",
            "Ignition speed (Zuenddrehzahl)",
            "Engine speed window (Drehzahlfenster)",
            "Engine speed < 1200 rpm (Drehzahl < 1200 Upm)",
            "Timer deselect indicator (Schaltuhr Abwahl Meldung)",
            "Timer release indicator (Schaltuhr Freigabe Meldung)",
            "Power grid disturbance (Netzstoerung F < nicht)",
            "Power grid disturbance (Netzstoerung F > nicht)",
            "Power grid disturbance (Netzstoerung U+F <> nicht)",
            "Engine knocking at minimal power (Klopfen Leistung Min.)",
            "Engine knocking at maximum power (Klopfen Leistung Max.)",
            "Power grid connector switch (Netzkuppelschalter)",
            "Engine power controller disturbed (Leistungsregler gestoert)",
            "Lambda controller disturbed (Lambdaregler gestoert)",
            "Generator protection disturbed (Generatorschuetz gestoert)",
            "Ignition disturbed (Zuendung gestoert)",
            "Oil pressure disturbed (Oeldruck gestoert)",
            "Lambda start position (Lambda Startposition)",
            "Engine knocking ON (Klopfen EIN)",
            "Engine knocking OFF (Klopfen AUS)",
            "Battery undervoltage (Batterie Unterspannung)",
            "Generator undervoltage (Generator Unterspannung)",
            "Generator overvoltage (Generator Ueberspannung)",
            "Generator excess current (Generator Ueberstrom)",
            "Generator unbalanced loads (Generator Schieflast)",
            "Seal test disturbed (Dichttest gestoert)",
            "Grid protection disturbed (Netzschutz gestoert)",
            "Sensors disturbed (Sensoren gestoert)",
            "Engine knocking disturbance (Klopfen Stoerung)",
            "Grid ok indicator (Netz o.k. Meldung)",
            "Grid disturbance warning (Netzstoerung Warnung)",
            "Temperature deselect indicator (Temperatur Abwahl Meldung)",
            "Temperature release indicator (Temperatur Freigabe Meldung)",
            "Maintenance interval exceeded (Wartung ueberschritten)",
            "Safety shutdown (Sicherheitsabschaltung)",
            "Engine not stopped (Motor steht nicht)",
            "Exhaust temperature difference A/B (Abgastemp. Differenz A/B)",
            "Reserve",
            "Return temperature max PT100/2 (Temp. Ruecklauf max PT100/2)",
            "Water temperature max PT200/3 (Temp. Heizwasser max PT100/3)",
            "Engine oil temperature max (Temp. Motoroel max)",
            "Gas mixture temperature max (Temp. Gasgemisch max)",
            "Gas mixture coolant temperature max (Temp. Gemischkuehlwasser max)",
            "Reserve",
            "Exhaust temperature A max (Abgastemperatur A max)",
            "Exhaust temperature A min (Abgastemperatur A min)",
            "Exhaust temperature B max (Abgastemperatur B max)",
            "Exhaust temperature B min (Abgastemperatur B min)",
            "Exhaust temperature C max (Abgastemperatur C max)",
            "Exhaust temperature C min (Abgastemperatur C min)",
            "Exhaust temperature D max (Abgastemperatur D max)",
            "Exhaust temperature D min (Abgastemperatur D min)"});

    private final String[] errors;

    ErrorPossibilities(String[] errors) {
        this.errors = errors;
    }

    /**
     * Gets the error list.
     *
     * @return the error list.
     */
    public String[] getErrorList() {
        return this.errors;
    }


}
