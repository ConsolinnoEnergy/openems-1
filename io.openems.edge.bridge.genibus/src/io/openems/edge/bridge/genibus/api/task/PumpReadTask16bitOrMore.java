package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;
import java.util.OptionalDouble;

/**
 * PumpTask class for reading values with 8 or 16 bit precision.
 * Extended precision supports 8, 16, 24 and 32 bit.
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
            this.refreshInfoCounter++;
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
                    // value w.o. considering units
                    tempValue = (super.zeroScaleFactor + sumValue);

                    /* 16bit formula
                    tempValue = (super.zeroScaleFactor + (actualDataArray[0] * ((double) super.rangeScaleFactor / (double) range))
                            + (actualDataArray[1] * ((double) super.rangeScaleFactor / ((double) range * 256))));
                    */

                    this.scaleValueToOpenEmsAndHandleError(tempValue);
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
                    // value w.o. considering units
                    tempValue = (Math.pow(256, exponent) * (256 * super.zeroScaleFactorHighOrder + super.zeroScaleFactorLowOrder)
                            + highPrecisionValue);

                    // Extended precision, 8 bit formula.
                    //tempValue = ((256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder) + actualData);

                    this.scaleValueToOpenEmsAndHandleError(tempValue);
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

    /**
     * Converts the data from Genibus to the unit of the associated OpenEMS channel (if possible). The result is put in
     * the channel.
     * If a correct conversion is not possible, only the genibusUnitFactor is applied (is 1 if not available) and the
     * channelMultiplier. In case of a failed conversion, a warning message is logged.
     *
     * @param genibusData the data from Genibus, without the genibusUnitFactor applied.
     */
    private void scaleValueToOpenEmsAndHandleError(double genibusData) {
        OptionalDouble unitAdjustedValue = super.unitTable.convertToOpenEmsUnit(genibusData, super.genibusUnitIndex, this.channel.channelDoc().getUnit());
        if (unitAdjustedValue.isPresent()) {
            this.channel.setNextValue(unitAdjustedValue.getAsDouble() * this.channelMultiplier);
        } else {
            // Error handling.
            if (super.unitString.equals("not yet supported")) {
                super.pumpDevice.setWarningMessage("Data for channel " + this.channel.channelId() + " is "
                        + "transferred in a unit that does not have an entry yet in ’UnitTable.java’. Not possible to apply correct scaling factor.");
            } else if (this.channel.channelDoc().getUnit() == Unit.NONE) {
                super.pumpDevice.setWarningMessage("Channel " + this.channel.channelId() + " has no unit. "
                        + "Data from Genibus has unit ’" + super.unitString + "’, scaling factor "
                        + (super.genibusUnitFactor * this.channelMultiplier) + " applied.");
            } else {
                super.pumpDevice.setWarningMessage("Unit mismatch. Channel " + this.channel.channelId()
                        + " has unit ’" + this.channel.channelDoc().getUnit() + "’, data from Genibus has unit ’"
                        + super.unitString + "’. Data in channel has unit from Genibus with scaling factor "
                        + (super.genibusUnitFactor * this.channelMultiplier) + " applied.");
            }
            // Fallback value, will be in a base unit (bar, °Celsius, watt, etc.) if unit is in UnitTable.
            this.channel.setNextValue(genibusData * super.genibusUnitFactor * this.channelMultiplier);
        }
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }
}
