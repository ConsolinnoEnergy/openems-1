package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Standard Unit Table for Grundfos pumps. Does not have every entry, but the ones needed for pump operations.
 * Required to translate the data sent by the pump into the correct values.
 */
public enum UnitTable {

    Standard_Unit_Table(
            new int[]{
                    // temperature
                    20, 21, 57, 84, 110, 111,
                    // powerActive
                    7, 8, 9, 44, 45,
                    // current
                    1,
                    // pressure
                    51,27,28,29,61,55,60,
                    91,83,24,25,26,
                    // percentage
                    113, 107, 12, 30, 76,
                    // flow
                    22, 23, 41, 92,
                    // frequency
                    105, 11, 16, 38, 17

            },
            new String[]{
                    // temperature
                    "Celsius/10", "Celsius", "Fahrenheit", "Kelvin/100", "diff-Kelvin/100", "diff-Kelvin",
                    // powerActive
                    "Watt", "Watt*10", "Watt*100", "kW", "kW*10",
                    // current
                    "Ampere*0.1",
                    // pressure
                    "bar/1000", "bar/100", "bar/10", "bar", "kPa", "psi", "psi*10",
                    "m/10000", "m/100", "m/10", "m", "m*10",    // <- this is the pressure unit ’water head’, m = bar/10
                    // percentage
                    "ppm", "0.01%", "0.1%", "1%", "10%",
                    // flow
                    "0.1*m³/h", "m³/h", "5*m³/h", "10*m³/h",
                    // frequency
                    "0.01*Hz", "0.5*Hz", "Hz", "2*Hz", "2.5*Hz"
            });

    private final Map<Integer, String> informationData = new HashMap<>();

    UnitTable(int[] keys, String[] values) {
        AtomicInteger counter = new AtomicInteger();
        counter.set(0);
        Arrays.stream(keys).forEach(key -> {
            this.informationData.put(key, values[counter.get()]);
            counter.getAndIncrement();
        });

    }

    /**
     * Get the ’informationData’ map.
     * @return the ’informationData’ map.
     */
    public Map<Integer, String> getInformationData() {
        return this.informationData;
    }

    /**
     * Returns the genibusUnitFactor. If the unit is not implemented yet it will return 1.
     * The genibusUnitFactor is part of the unit conversion calculation.
     *
     * @param genibusUnitIndex the genibusUnitIndex
     * @return the genibusUnitFactor
     */
    public double getGenibusUnitFactor(int genibusUnitIndex) {
        double genibusUnitFactor = 1.0;
        String unitString = this.getInformationData().get(genibusUnitIndex);
        if (unitString != null) {
            switch (unitString) {
                // genibusUnitFactor is set to convert all pressure units to bar.

                case "Celsius/10":
                case "bar/10":
                case "m":   // <- this is the pressure unit ’water head’, m = bar/10
                case "Ampere*0.1":
                case "0.1*m³/h":
                case "0.1%":
                    genibusUnitFactor = 0.1;
                    break;
                case "Kelvin/100":
                case "diff-Kelvin/100":
                case "bar/100":
                case "m/10":
                case "kPa":
                case "0.01*Hz":
                case "0.01%":
                    genibusUnitFactor = 0.01;
                    break;
                case "bar/1000":
                case "m/100":
                    genibusUnitFactor = 0.001;
                    break;
                case "ppm":
                case "m/10000":
                    genibusUnitFactor = 0.000001;
                    break;
                case "psi":
                    genibusUnitFactor = 0.06895;
                    break;
                case "psi*10":
                    genibusUnitFactor = 0.6895;
                    break;
                case "2*Hz":
                    genibusUnitFactor = 2.0;
                    break;
                case "2.5*Hz":
                    genibusUnitFactor = 2.5;
                    break;
                case "5*m³/h":
                    genibusUnitFactor = 5.0;
                    break;
                case "Watt*10":
                case "10*m³/h":
                case "10%":
                    genibusUnitFactor = 10.0;
                    break;
                case "Watt*100":
                    genibusUnitFactor = 100.0;
                    break;
                case "kW":
                    genibusUnitFactor = 1000.0;
                    break;
                case "kW*10":
                    genibusUnitFactor = 10000.0;
                    break;
                case "Celsius":
                case "Fahrenheit":
                case "Kelvin":
                case "diff-Kelvin":
                case "Watt":
                case "bar":
                case "m*10":
                case "m³/h":
                case "Hz":
                case "1%":
                default:
                    genibusUnitFactor = 1.0;
            }
        }
        return genibusUnitFactor;
    }

    /**
     * Convert a value from Genibus unit to OpenEMS unit. Returns ’null’ if the unit provided by Genibus is not
     * compatible to the OpenEMS unit or one of the units is not yet supported.
     * Genibus has the units ’Kelvin’ (absolute temperature) and ’diff-Kelvin’ (temperature difference).
     * It was decided to exclusively link OpenEMS ’Kelvin’ to Genibus ’diff-Kelvin’ to avoid confusion. So Genibus
     * ’Kelvin’ will only convert if the target OpenEMS unit is ’Celsius’.
     *
     * @param value the value read from Genibus.
     * @param genibusUnitIndex the Genibus unit, conveyed as the number of the Genibus unit index.
     * @param openEmsUnit the target OpenEMS unit.
     * @return the value converted to the OpenEMS unit.
     */
    public OptionalDouble convertToOpenEmsUnit(double value, int genibusUnitIndex, Unit openEmsUnit) {

        String unitString = this.getInformationData().get(genibusUnitIndex);
        if (unitString != null) {

            double genibusUnitFactor = this.getGenibusUnitFactor(genibusUnitIndex);
            Unit openemsBaseUnit;
            int channelUnitScaleFactor = openEmsUnit.getScaleFactor();
            if (openEmsUnit.getBaseUnit() != null) {
                openemsBaseUnit = openEmsUnit.getBaseUnit();
            } else {
                openemsBaseUnit = openEmsUnit;
            }

            switch (openemsBaseUnit) {
                case DEGREE_CELSIUS:
                    switch (unitString) {
                        case "Celsius/10":
                        case "Celsius":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                        case "Kelvin/100":
                        case "Kelvin":
                            return OptionalDouble.of(((genibusUnitFactor * value) - 273.15) * Math.pow(10, -channelUnitScaleFactor));
                        case "Fahrenheit":
                            return OptionalDouble.of((((genibusUnitFactor * value) - 32) * (5.d / 9.d)) * Math.pow(10, -channelUnitScaleFactor));
                    }
                case DEGREE_KELVIN: // Assume this unit is used for temperature differences, not absolute temperatures.
                    switch (unitString) {
                        case "diff-Kelvin/100":
                        case "diff-Kelvin":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case BAR:
                    switch (unitString) {
                        case "bar/1000":
                        case "bar/100":
                        case "bar/10":
                        case "bar":
                        case "m/10000":
                        case "m/100":
                        case "m/10":
                        case "m":   // <- this is the pressure unit ’water head’, m = bar/10
                        case "m*10":
                        case "psi":
                        case "psi*10":
                        case "kPa":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case PASCAL:
                    switch (unitString) {
                        case "bar/1000":
                        case "bar/100":
                        case "bar/10":
                        case "bar":
                        case "m/10000":
                        case "m/100":
                        case "m/10":
                        case "m":
                        case "m*10":
                        case "psi":
                        case "psi*10":
                        case "kPa":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor + 5) * value);
                    }
                case AMPERE:
                    switch (unitString) {
                        case "Ampere*0.1":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case HERTZ:
                    switch (unitString) {
                        case "0.01*Hz":
                        case "0.5*Hz":
                        case "Hz":
                        case "2*Hz":
                        case "2.5*Hz":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case WATT:
                    switch (unitString) {
                        case "Watt":
                        case "Watt*10":
                        case "Watt*100":
                        case "kW":
                        case "kW*10":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case PERCENT:
                    switch (unitString) {
                        case "ppm":
                        case "0.01%":
                        case "0.1%":
                        case "1%":
                        case "10%":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case CUBICMETER_PER_HOUR:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of(genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case CUBICMETER_PER_SECOND:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of((1.0 / 3600.0) * genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
                case LITER_PER_MINUTE:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of((1000.0 / 60.0) * genibusUnitFactor * Math.pow(10, -channelUnitScaleFactor) * value);
                    }
            }
        }
        return OptionalDouble.empty();  // Code lands here if units don't match or units are not yet supported.
    }

    /**
     * Convert a value from an OpenEMS unit to Genibus unit. Returns ’null’ if the unit provided by OpenEMS is not
     * compatible to the Genibus unit or one of the units is not yet supported.
     * Genibus has the units ’Kelvin’ (absolute temperature) and ’diff-Kelvin’ (temperature difference).
     * It was decided to exclusively link OpenEMS ’Kelvin’ to Genibus ’diff-Kelvin’ to avoid confusion. To convert to
     * Genibus ’Kelvin’, the OpenEMS source unit must be ’Celsius’.’
     *
     * @param value the value in OpenEMS units.
     * @param openEmsUnit the OpenEMS unit.
     * @param genibusUnitIndex the target Genibus unit, conveyed as the number of the Genibus unit index.
     * @return the value converted to Genibus units.
     */
    public OptionalDouble convertToGenibusUnit(double value, Unit openEmsUnit, int genibusUnitIndex) {

        String unitString = this.getInformationData().get(genibusUnitIndex);
        if (unitString != null) {

            double genibusUnitFactor = this.getGenibusUnitFactor(genibusUnitIndex);
            Unit openemsBaseUnit;
            int channelUnitScaleFactor = openEmsUnit.getScaleFactor();
            if (openEmsUnit.getBaseUnit() != null) {
                openemsBaseUnit = openEmsUnit.getBaseUnit();
            } else {
                openemsBaseUnit = openEmsUnit;
            }

            switch (openemsBaseUnit) {
                case DEGREE_CELSIUS:
                    switch (unitString) {
                        case "Celsius/10":
                        case "Celsius":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                        case "Kelvin/100":
                        case "Kelvin":
                            return OptionalDouble.of(((value * Math.pow(10, channelUnitScaleFactor)) + 273.15) / genibusUnitFactor);
                        case "Fahrenheit":
                            return OptionalDouble.of(((value * Math.pow(10, channelUnitScaleFactor) * (9.d / 5.d)) + 32) / genibusUnitFactor);
                    }
                case DEGREE_KELVIN: // Assume this unit is used for temperature differences, not absolute temperatures.
                    switch (unitString) {
                        case "diff-Kelvin/100":
                        case "diff-Kelvin":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case BAR:
                    switch (unitString) {
                        case "bar/1000":
                        case "bar/100":
                        case "bar/10":
                        case "bar":
                        case "m/10000":
                        case "m/100":
                        case "m/10":
                        case "m":   // <- this is the pressure unit ’water head’, m = bar/10
                        case "m*10":
                        case "psi":
                        case "psi*10":
                        case "kPa":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case PASCAL:
                    switch (unitString) {
                        case "bar/1000":
                        case "bar/100":
                        case "bar/10":
                        case "bar":
                        case "m/10000":
                        case "m/100":
                        case "m/10":
                        case "m":
                        case "m*10":
                        case "psi":
                        case "psi*10":
                        case "kPa":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor - 5) / genibusUnitFactor) * value);
                    }
                case AMPERE:
                    switch (unitString) {
                        case "Ampere*0.1":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case HERTZ:
                    switch (unitString) {
                        case "0.01*Hz":
                        case "0.5*Hz":
                        case "Hz":
                        case "2*Hz":
                        case "2.5*Hz":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case WATT:
                    switch (unitString) {
                        case "Watt":
                        case "Watt*10":
                        case "Watt*100":
                        case "kW":
                        case "kW*10":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case PERCENT:
                    switch (unitString) {
                        case "ppm":
                        case "0.01%":
                        case "0.1%":
                        case "1%":
                        case "10%":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case CUBICMETER_PER_HOUR:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) / genibusUnitFactor) * value);
                    }
                case CUBICMETER_PER_SECOND:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) * 3600.0 / genibusUnitFactor) * value);
                    }
                case LITER_PER_MINUTE:
                    switch (unitString) {
                        case "0.1*m³/h":
                        case "m³/h":
                        case "5*m³/h":
                        case "10*m³/h":
                            return OptionalDouble.of((Math.pow(10, channelUnitScaleFactor) * (60.0 / 1000.0) / genibusUnitFactor) * value);
                    }
            }
        }
        return OptionalDouble.empty();  // Code lands here if units don't match or units are not yet supported.
    }
}

