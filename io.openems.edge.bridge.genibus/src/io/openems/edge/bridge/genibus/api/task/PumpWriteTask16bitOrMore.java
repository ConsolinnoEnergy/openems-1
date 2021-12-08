package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;
import java.util.OptionalDouble;

/**
 * PumpTask class for writing values with 8 or 16 bit precision.
 * Extended precision supports 8, 16, 24 and 32 bit.
 */
public class PumpWriteTask16bitOrMore extends PumpReadTask16bitOrMore implements HeadClass4and5 {

    private final WriteChannel<Double> channel;

    /* This task can do GET and SET. However, the data of this task only changes when a SET is executed. Thus, a GET
       only needs to be executed once at the beginning and then after a SET. This flag is used to control that.
       If this task is executed as GET, this flag is set to ’false’ to stop GET execution.
       If this task is executed as SET, this flag is set to ’true’, so a GET is executed. */
    private boolean executeGet = true;

    public PumpWriteTask16bitOrMore(int numberOfBytes, int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority, double channelMultiplier) {
        super(numberOfBytes, address, headerNumber, channel, unitString, priority, channelMultiplier);
        this.channel = channel;
        /* The method "getRequest()" does not work without first calling "setFourByteInformation()". Normally
           "setFourByteInformation()" is executed when an info response APDU is processed. For ref_rem (5, 1) we can call
           "setFourByteInformation()" manually since the parameters are fixed and don't change. This way we do not need
           to send an info APDU for ref_rem to be able to use "getRequest()". */
        if (headerNumber == 5 && address == 1) {
            super.setFourByteInformation(0,0,2, (byte)0b11110, (byte)0, (byte)0b1100100);
        }
    }

    public PumpWriteTask16bitOrMore(int numberOfBytes, int address, int headerNumber, WriteChannel<Double> channel, String unitString, Priority priority) {
        this(numberOfBytes, address, headerNumber, channel, unitString, priority, 1);
    }

    @Override
    public boolean isSetAvailable() {
        return super.informationDataAvailable() && this.channel.getNextWriteValue().isPresent();
    }

    @Override
    public int getByteIfSetAvailable(int byteCounter) {

        // A correct return value is a byte. Choose a return value that is outside the range of byte to indicate an error.
        int errorReturnValue = GenibusTask.NO_SET_AVAILABLE;
        if (byteCounter > dataByteSize - 1) {
            return errorReturnValue;
        }
        if (this.isSetAvailable()) {
            int returnValue;

            /* For values of type scaled or extended precision:
               With INFO available, the value in nextWrite is automatically converted to the correct bytes for GENIbus. */
            double dataOfChannel = this.channel.getNextWriteValue().get() / super.channelMultiplier;
            switch (super.sif) {
                case 2:
                    double writeValue = this.scaleValueToGenibusUnitAndHandleError(dataOfChannel);

                    // Formula working for both 8 and 16 bit.
                    long combinedByteValueScaled = Math.round((-super.zeroScaleFactor + writeValue) * (254 * Math.pow(256, (dataByteSize - 1))) / super.rangeScaleFactor);
                    returnValue = this.convertToByte(byteCounter, combinedByteValueScaled);
                    break;
                case 3:
                    writeValue = this.scaleValueToGenibusUnitAndHandleError(dataOfChannel);

                    // Formula working for 8, 16, 24 and 32 bit.
                    long combinedByteValueExtended = Math.round(writeValue) - (256 * super.zeroScaleFactorHighOrder + super.zeroScaleFactorLowOrder);
                    returnValue = this.convertToByte(byteCounter, combinedByteValueExtended);
                    break;
                case 1:
                case 0:
                default:
                    long combinedByteValueDirect = Math.round(dataOfChannel);
                    returnValue = this.convertToByte(byteCounter, combinedByteValueDirect);
                    break;
            }
            return returnValue;
        }
        return errorReturnValue;
    }

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

    @Override
    public void clearNextWriteAndUpdateChannel() {
        // If the write task is added to a telegram, reset channel write to null to send write just once.
        this.channel.getNextWriteValueAndReset();
        this.executeGet = true; // Do GET in the next telegram to update the channel value.
    }

    @Override
    public void setExecuteGet(boolean value) {
        this.executeGet = value;
    }

    @Override
    public boolean getExecuteGet() {
        return this.executeGet;
    }

}
