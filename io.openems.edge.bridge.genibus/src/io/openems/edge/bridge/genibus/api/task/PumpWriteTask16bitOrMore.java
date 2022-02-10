package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;
import java.util.OptionalDouble;

/**
 * PumpTask class for writing values with 8 or 16 bit precision.
 * Extended precision supports 8, 16, 24 and 32 bit.
 * Limitations: support for more than 8 bit data items is limited to head class 4 at the moment. Specifically, those
 * data items that have hi and lo bytes on subsequent addresses. For example t_ramp_up_1_hi (4, 23) and t_ramp_up_1_lo
 * (4, 24), where (4, 23) is the hi byte and (4, 24) the corresponding lo byte.
 * You give this task the address of the hi byte and the total number of bytes. This task then automatically uses the
 * addresses following the hi byte and assumes these are the lo bytes. The hi and lo bytes are mapped to a single channel,
 * the one associated with this task. Combining of bytes for read and splitting of bytes for write is done automatically.
 * Example: for t_ramp_up_1_hi and t_ramp_up_1_lo, you put headerNumber 4, address 23 and numberOfBytes 2. You then get
 * the combined value of t_ramp_up_1_hi and t_ramp_up_1_lo in one channel.
 *
 * <p>Special treatment for task ’ref_rem (5, 1)’: It appears that INFO for ’ref_rem’ is always the same (unit %, range
 * [0, 100]). To speed this task up a bit, INFO is not requested from the Genibus device, but set by the code on startup.
 * If for whatever reason INFO of ’ref_rem’ can be something else, this bit of code needs to be deleted (line 37 - 47).</p>
 */
public class PumpWriteTask16bitOrMore extends PumpReadTask16bitOrMore implements HeadClass4and5 {

    private final WriteChannel<Double> channel;

    /* This task can do GET and SET. However, the data of this task only changes when a SET is executed. Thus, a GET
       only needs to be executed once at the beginning and then after a SET. This flag is used to control that.
       If this task is executed as GET, this flag is set to ’false’ to stop GET execution.
       If this task is executed as SET, this flag is set to ’true’, so a GET is executed. */
    private boolean executeGet = true;

    /**
     * Constructor with channel multiplier. The channel multiplier is an additional multiplication factor that is applied
     * to the data value from Genibus before it is put in the channel. For writes to the Genibus device, it is a divisor.
     *
     * @param numberOfBytes the number of bytes of this task. 8 bit = 1, 16 bit = 2, etc.
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     * @param channelMultiplier the channel multiplier.
     */
    public PumpWriteTask16bitOrMore(int numberOfBytes, int headerNumber, int address, WriteChannel<Double> channel, String unitTable, Priority priority, double channelMultiplier) {
        super(numberOfBytes, headerNumber, address, channel, unitTable, priority, channelMultiplier);
        this.channel = channel;
        /* Special treatment for ’ref_rem (5, 1)’:
           The usual way to read data from Genibus is to first call INFO, then GET or SET. The information transmitted by
           INFO is needed to interpret GET and calculate the right value for SET.
           For ’ref_rem’, INFO is always the same, so we can skip requesting it. This ’if’ checks if the task is ’ref_rem’,
           and if yes performs the operation that would result from an INFO call. The GenibusWorker then sees that INFO
           is already done for ’ref_rem’ and can immediately do a SET.
           The Genibus bridge would work just fine without this bit of code, it just make the often used ’ref_rem’ task
           about 1 second faster after startup. */
        if (headerNumber == 5 && address == 1) {
            super.setFourByteInformation(0,0,2, (byte)0b11110, (byte)0, (byte)0b1100100);
        }
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
    public PumpWriteTask16bitOrMore(int numberOfBytes, int headerNumber, int address, WriteChannel<Double> channel, String unitTable, Priority priority) {
        this(numberOfBytes, headerNumber, address, channel, unitTable, priority, 1);
    }

    /**
     * Returns if this task has a SET available. If a SET is available depends on the associated write channel.
     * For head class 3 tasks (commands), this is true if the ’nextWrite’ of the associated channel contains ’true’.
     * For head class 4 and 5 this is true if the ’nextWrite’ of the associated channel is not empty.
     *
     * @return if a SET is available.
     */
    @Override
    public boolean isSetAvailable() {
        /* informationDataAvailable() is a requirement, because INFO is needed to be able to convert the write value
           into the correct format. */
        return super.informationDataAvailable() && this.channel.getNextWriteValue().isPresent();
    }

    /**
     * Returns the byte value for a SET. For a multi byte task, the parameter byteNumber specifies which byte (0 is hi).
     * For 8 bit tasks, byteNumber is 0.
     * The return value is an int of value 0 to 255, or ’NO_SET_AVAILABLE’ if there is no SET available (or the
     * byteNumber is wrong). If a SET is available depends on the associated write channel. If ’nextWrite’ of the channel
     * is empty, then there is no SET available.
     * This method does not clear the ’nextWrite’ of the channel, because the method only returns one byte and needs to
     * be called multiple times for multi byte tasks. Instead, the calling code needs to clear the ’nextWrite’ manually
     * using ’clearNextWriteAndUpdateChannel()’ once all bytes have been collected.
     *
     * @param byteNumber which byte of SET to return.
     * @return the byte value of the SET if available, ’NO_SET_AVAILABLE’ otherwise.
     */
    @Override
    public int getByteIfSetAvailable(int byteNumber) {

        // A correct return value is a byte. Choose a return value that is outside the range of byte to indicate an error.
        int errorReturnValue = HeadClass4and5.NO_SET_AVAILABLE;
        if (byteNumber > dataByteSize - 1) {
            return errorReturnValue;
        }
        if (super.informationDataAvailable() && this.channel.getNextWriteValue().isPresent()) {
            int returnValue;

            /* For values of type scaled or extended precision:
               With INFO available, the value in nextWrite is automatically converted to the correct bytes for GENIbus. */
            double dataOfChannel = this.channel.getNextWriteValue().get() / super.channelMultiplier;
            switch (super.sif) {
                case 2:
                    double writeValue = this.scaleValueToGenibusUnitAndHandleError(dataOfChannel);

                    // Formula working for both 8 and 16 bit.
                    long combinedByteValueScaled = Math.round((-super.zeroScaleFactor + writeValue) * (254 * Math.pow(256, (dataByteSize - 1))) / super.rangeScaleFactor);
                    returnValue = this.convertToByte(byteNumber, combinedByteValueScaled);
                    break;
                case 3:
                    writeValue = this.scaleValueToGenibusUnitAndHandleError(dataOfChannel);

                    // Formula working for 8, 16, 24 and 32 bit.
                    long combinedByteValueExtended = Math.round(writeValue) - (256 * super.zeroScaleFactorHighOrder + super.zeroScaleFactorLowOrder);
                    returnValue = this.convertToByte(byteNumber, combinedByteValueExtended);
                    break;
                case 1:
                case 0:
                default:
                    long combinedByteValueDirect = Math.round(dataOfChannel);
                    returnValue = this.convertToByte(byteNumber, combinedByteValueDirect);
                    break;
            }
            return returnValue;
        }
        return errorReturnValue;
    }

    /**
     * Converts the data from the OpenEMS channel to the unit of the associated Genibus data item (if possible) and
     * return the result.
     * If a correct conversion is not possible, only the genibusUnitFactor is applied (is 1 if not available). In case
     * of a failed conversion, a warning message is logged.
     *
     * @param dataOfChannel the data from the OpenEMS channel, with the channel multiplier already applied.
     * @return the data converted to the Genibus unit (if possible).
     */
    private double scaleValueToGenibusUnitAndHandleError(double dataOfChannel) {
        OptionalDouble unitAdjustedValue = super.unitTable.convertToGenibusUnit(dataOfChannel, this.channel.channelDoc().getUnit(), super.genibusUnitIndex);
        double returnValue;
        if (unitAdjustedValue.isPresent()) {
            returnValue = unitAdjustedValue.getAsDouble();
        } else {
            // Error handling.
            if (super.unitString.equals("not yet supported")) {
                super.pumpDevice.setWarningMessage("Channel " + this.channel.channelId() + " is writing to Genibus, but "
                        + "the Genibus data field is using a unit that does not have an entry yet in ’UnitTable.java’. "
                        + "Not possible to apply correct scaling factor.");
            } else if (this.channel.channelDoc().getUnit() == Unit.NONE) {
                super.pumpDevice.setWarningMessage("Channel " + this.channel.channelId() + " is writing to Genibus, but "
                        + "the channel has no unit. The Genibus data field has the unit ’" + super.unitString + "’, "
                        + "channel value divided by scaling factor " + (super.genibusUnitFactor * this.channelMultiplier) + ".");
            } else {
                super.pumpDevice.setWarningMessage("Unit mismatch. Channel " + this.channel.channelId() + " with unit ’"
                        + this.channel.channelDoc().getUnit() + "’ is writing to Genibus data field with unit ’"
                        + super.unitString + "’. Data in channel is divided by scaling factor "
                        + (super.genibusUnitFactor * this.channelMultiplier) + ".");
            }
            // Fallback value, will be in a base unit (bar, °Celsius, watt, etc.) if unit is in UnitTable.
            returnValue = dataOfChannel / super.genibusUnitFactor;
        }
        return returnValue;
    }

    /**
     * Segments the value to be written to Genibus (’writeValue’) into bytes, according to the formula given in
     * ’genispec2016.pdf’. The parameter ’byteCounter’ chooses which byte to return (0 = hi). Returns the unsigned
     * byte (value between 0 and 255) as an int.
     * The value sent to Genibus needs to be an unsigned byte. However, Java does not have unsigned bytes. The workaround
     * is then to keep the value in an int, and convert to actual byte at the last step.
     *
     * <p>Note about unsigned bytes in Java and byte casting: see https://programming.guide/java/unsigned-byte.html
     * What happens when you cast an int to a byte is that it takes the last 8 bits of the int:
     * int i = 150;        // 00000000 00000000 00000000 10010110
     * byte b = (byte) i;  //                            10010110
     * System.out.println(b);                      // -106
     * System.out.println(Byte.toUnsignedInt(b));  //  150
     * int signed = b;                             // -106
     * int unsigned = b & 0xff;                    //  150</p>
     *
     * <p>When casting to byte, the bit pattern is not changed. You get the correct bit pattern of an unsigned byte. The
     * interpretation of the byte is were you need to da a workaround if you want an unsigned byte.</p>
     *
     * @param byteCounter the byte counter. 0 is hi.
     * @param writeValue the value to be written to Genibus.
     * @return the unsigned byte specified by ’byteCounter’ as an int.
     */
    private int convertToByte(int byteCounter, long writeValue) {
        long tempValue;
        if (byteCounter == 0) {
            tempValue = (writeValue / Math.round(Math.pow(256, (dataByteSize - 1))));
        } else {
            tempValue = ((writeValue % Math.round(Math.pow(256, (byteCounter)))) / Math.round(Math.pow(256, (dataByteSize - 1 - byteCounter))));
        }
        tempValue = Math.min(tempValue, 255);
        tempValue = Math.max(0, tempValue);
        return (int) tempValue;
    }

    /**
     * This method clears the ’nextWrite’ of the channel associated with this task. This will make ’isSetAvailable()’
     * return ’false’, until a new value has been written into ’nextWrite’ of the channel.
     * Should be called after this task was added as SET to an APDU, so that the SET is executed just once.
     * Also, if applicable, marks this task as ’get value from Genibus’, so the channel is updated to the new value.
     */
    @Override
    public void clearNextWriteAndUpdateChannel() {
        // If the write task is added to a telegram, reset channel write to null to send write just once.
        this.channel.getNextWriteValueAndReset();
        this.executeGet = true; // Do GET in the next telegram to update the channel value.
    }

    /**
     * Set the ’ExecuteGetState’ of the task. This tells the Genibus worker if this task should be executed as GET or
     * not.
     *
     * @param value the ’ExecuteGetState’.
     */
    @Override
    public void setExecuteGet(boolean value) {
        this.executeGet = value;
    }

    /**
     * Get the ’ExecuteGetState’ of the task. This tells the Genibus worker if this task should be executed as GET or
     * not.
     *
     * @return the ’ExecuteGetState’.
     */
    @Override
    public boolean getExecuteGet() {
        return this.executeGet;
    }

}
