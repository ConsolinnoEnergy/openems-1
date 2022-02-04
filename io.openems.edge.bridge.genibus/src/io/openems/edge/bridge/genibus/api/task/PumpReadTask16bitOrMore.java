package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.taskmanager.Priority;
import java.util.OptionalDouble;

/**
 * PumpTask class for reading values with 8 or 16 bit precision.
 * Extended precision supports 8, 16, 24 and 32 bit.
 * Limitations: support for more than 8 bit data items is limited to head class 2 at the moment. Specifically, those
 * data items that have hi and lo bytes on subsequent addresses. For example t_2hour_hi (2, 24) and t_2hour_lo (2, 25).
 * You give this task the address of the hi byte and the total number of bytes. This task then automatically requests
 * data from the addresses following the hi byte and assumes these are the lo bytes. Once all bytes are collected, the
 * combined value is calculated and put in the channel associated with this task.
 */
public class PumpReadTask16bitOrMore extends AbstractPumpTask {

    private final Channel<Double> channel;
    private final Priority priority;
    protected final double channelMultiplier;
    private int refreshInfoCounter = 0;
    private int byteCounter = 0;
    private final byte[] dataArray = new byte[dataByteSize];

    /**
     * Constructor with channel multiplier. The channel multiplier is an additional multiplication factor that is applied
     * to the data value from Genibus before it is put in the channel.
     *
     * @param numberOfBytes the number of bytes of this task. 8 bit = 1, 16 bit = 2, etc.
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     * @param channelMultiplier the channel multiplier.
     */
    public PumpReadTask16bitOrMore(int numberOfBytes, int headerNumber, int address, Channel<Double> channel, String unitTable, Priority priority, double channelMultiplier) {
        super(headerNumber, address, unitTable, numberOfBytes);
        this.channel = channel;
        this.priority = priority;
        this.channelMultiplier = channelMultiplier;
    }

    /**
     * Constructor without channel multiplier.
     *
     * @param numberOfBytes the number of bytes of this task. 8 bit = 1, 16 bit = 2, etc.
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     */
    public PumpReadTask16bitOrMore(int numberOfBytes, int headerNumber, int address, Channel<Double> channel, String unitTable, Priority priority) {
        this(numberOfBytes, headerNumber, address, channel, unitTable, priority, 1);
    }

    /**
     * Allocate a byte from a response telegram to this task. For multi byte tasks, call this method for each byte in
     * the order hi to lo. Once all bytes are allocated, the data is processed and the result put in ’nextValue’ of the
     * associated OpenEMS channel.
     * This method handles the conversion of the value from Genibus into a number. If the channel associated with this
     * task has a unit compatible with the one transmitted by Genibus, the method will attempt to scale the value to the
     * unit of the channel.
     *
     * @param data the response byte from the Genibus device for this task.
     */
    @Override
    public void processResponse(byte data) {

        // Collect the bytes.
        this.dataArray[this.byteCounter] = data;
        if (this.byteCounter < this.dataByteSize - 1) {
            this.byteCounter++;
            return;
        }
        // Data is complete. Now calculate value and put it in the channel.
        this.byteCounter = 0;

        // ref_norm changes INFO if control mode is changed. If task is ref_norm (2, 49), regularly update INFO.
        final int refNormHeadClass = 2;
        final int refNormAddress = 49;
        if (getHeader() == refNormHeadClass && getAddress() == refNormAddress) {
            this.refreshInfoCounter++;
            final int updateInterval = 5;
            if (this.refreshInfoCounter >= updateInterval) {
                super.resetInfo();
                this.refreshInfoCounter = 0;
            }
        }

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
        if (super.vi) {
            range = 255;
        }

        // This part does the conversion of the Genibus value into a number.
        double tempValue;
        switch (super.sif) {
            case 2:
                // Formula working for both 8 and 16 bit
                double sumValue = 0;
                for (int i = 0; i < this.dataByteSize; i++) {
                    sumValue = sumValue +  actualDataArray[i] * ((double) super.rangeScaleFactor / (double) range * Math.pow(256, i));
                }
                // value w.o. considering units
                tempValue = (super.zeroScaleFactor + sumValue);

                /* 16bit formula, as per genispec manual.
                   tempValue = (super.zeroScaleFactor + (actualDataArray[0] * ((double) super.rangeScaleFactor / (double) range))
                            + (actualDataArray[1] * ((double) super.rangeScaleFactor / ((double) range * 256)))); */

                this.scaleValueToOpenEmsAndHandleError(tempValue);
                break;
            case 3:
                // Formula working for 8, 16, 24 and 32 bit.
                double highPrecisionValue = 0;
                for (int i = 0; i < dataByteSize; i++) {
                    highPrecisionValue = highPrecisionValue + actualDataArray[i] * Math.pow(256, (this.dataByteSize - i - 1));
                }
                int exponent = Math.max(0, dataByteSize - 2);
                // value w.o. considering units
                tempValue = (Math.pow(256, exponent) * (256 * super.zeroScaleFactorHighOrder + super.zeroScaleFactorLowOrder)
                        + highPrecisionValue);

                /* Extended precision, 8 bit formula as per genispec manual.
                   tempValue = ((256 * super.scaleFactorHighOrder + super.scaleFactorLowOrder) + actualData); */

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
                super.pumpDevice.setWarningMessage("Data from Genibus for channel " + this.channel.channelId() + " is "
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

    /**
     * Get the priority of this task. High, low or once.
     *
     * @return the priority.
     */
    @Override
    public Priority getPriority() {
        return this.priority;
    }
}
