package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for reading values with 16bit precision.
 */
public class PumpReadTask16bitOrMore extends AbstractPumpTask {

    private final Channel<Double> channel;
    private final Priority priority;
    protected final double channelMultiplier;
    private int refreshInfoCounter = 0;
    private int byteCounter = 0;
    private byte[] dataArray = new byte[dataByteSize];

    public PumpReadTask16bitOrMore(int numberOfBytes, int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(address, headerNumber, unitString, numberOfBytes);
        this.channel = channel;
        this.priority = priority;
        this.channelMultiplier = channelMultiplier;
    }

    public PumpReadTask16bitOrMore(int numberOfBytes, int address, int headerNumber, Channel<Double> channel, String unitString, Priority priority) {
        this(numberOfBytes, address, headerNumber, channel, unitString, priority, 1);
    }

    @Override
    public void processResponse(byte data) {

        // Collect the bytes.
        this.dataArray[this.byteCounter] = data;
        if (this.byteCounter != this.dataByteSize - 1) {
            this.byteCounter++;
            return;
        }

        // ref_norm changes INFO if control mode is changed. If task is ref_norm (2, 49), regularly update INFO.
        if (getHeader() == 2 && getAddress() == 49) {
            this. refreshInfoCounter++;
            if (this.refreshInfoCounter >= 5) {
                super.resetInfo();
                this.refreshInfoCounter = 0;
            }
        }

        // Data is complete, now calculate value and put it in the channel.
        if (this.byteCounter >= this.dataByteSize - 1) {
            this.byteCounter = 0;

            // When vi == 0 (false), then 0xFF means "data not available".
            if (super.vi == false) {
                if ((this.dataArray[0] & 0xFF) == 0xFF) {
                    this.channel.setNextValue(null);
                    return;
                }
            }

            int[] actualDataArray = new int[this.dataByteSize];
            for (int i = 0; i < this.dataByteSize; i++) {
                actualDataArray[i] = Byte.toUnsignedInt(this.dataArray[i]);
            }

            int range = 254;
            double tempValue;
            if (super.vi) {
                range = 255;
            }

            switch (super.sif) {
                case 2:
                    // Formula working for both 8 and 16 bit
                    double sumValue = 0;
                    for (int i = 0; i < this.dataByteSize; i++) {
                        sumValue = sumValue +  actualDataArray[i] * ((double) super.rangeScaleFactor / (double) range * Math.pow(256, i));
                    }
                    // value w.o considering Channel
                    tempValue = (super.zeroScaleFactor + sumValue) * super.unitCalc;

                    /* 16bit formula
                    tempValue = (super.zeroScaleFactor + (actualDataArray[0] * ((double) super.rangeScaleFactor / (double) range))
                            + (actualDataArray[1] * ((double) super.rangeScaleFactor / ((double) range * 256)))) * super.unitCalc;
                    */

                    this.channel.setNextValue(this.correctValueForChannel(tempValue) * this.channelMultiplier);
                    break;
                case 3:
                    // Formula working for 8, 16, 24 and 32 bit.
                    double highPrecisionValue = 0;
                    for (int i = 0; i < dataByteSize; i++) {
                        highPrecisionValue = highPrecisionValue + actualDataArray[i] * Math.pow(256, (this.dataByteSize - i - 1));
                    }
                    int exponent = dataByteSize - 2;
                    if (exponent < 0) {
                        exponent = 0;
                    }
                    tempValue = (Math.pow(256, exponent) * (256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder)
                            + highPrecisionValue) * super.unitCalc;

                    // Extended precision, 8 bit formula.
                    //tempValue = ((256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder) + actualData) * super.unitCalc;

                    this.channel.setNextValue(this.correctValueForChannel(tempValue) * this.channelMultiplier);
                    break;
                case 1:
                case 0:
                default:
                    // Formula works for 8 to 32 bit.
                    double unscaledMultiByte = 0;
                    for (int i = 0; i < this.dataByteSize; i++) {
                        unscaledMultiByte = unscaledMultiByte + actualDataArray[i] * Math.pow(256, (this.dataByteSize - i - 1));
                    }
                    this.channel.setNextValue(unscaledMultiByte * this.channelMultiplier);
                    break;

            }
        }


    }

    private double correctValueForChannel(double tempValue) {
        //unitString
        if (super.unitString != null) {
            Unit openemsBaseUnit = this.channel.channelDoc().getUnit();
            int scaleFactor = openemsBaseUnit.getScaleFactor();
            if (this.channel.channelDoc().getUnit().getBaseUnit() != null) {
                openemsBaseUnit = openemsBaseUnit.getBaseUnit();
            }

            switch (openemsBaseUnit) {
                case DEGREE_CELSIUS:
                    switch (super.unitString) {
                        case "Celsius/10":
                        case "Celsius":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * tempValue;
                        case "Kelvin/100":
                        case "Kelvin":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * (tempValue - 273.15);
                        case "Fahrenheit":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * ((tempValue - 32) * (5.d / 9.d));
                    }
                case DEGREE_KELVIN:
                    switch (super.unitString) {
                        case "Celsius/10":
                        case "Celsius":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * (tempValue + 273.15);
                        case "Kelvin/100":
                        case "Kelvin":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * tempValue;
                        case "Fahrenheit":
                            return super.unitCalc * Math.pow(10, -scaleFactor) * (((tempValue - 32) * (5.d / 9.d)) + 273.15);
                    }
                case BAR:
                    switch (super.unitString) {
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
                            return super.unitCalc * Math.pow(10, -scaleFactor) * tempValue;
                    }
                case PASCAL:
                    switch (super.unitString) {
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
                            return super.unitCalc * Math.pow(10, -scaleFactor + 5) * tempValue;
                    }
            }
        }

        return tempValue;
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }
}
