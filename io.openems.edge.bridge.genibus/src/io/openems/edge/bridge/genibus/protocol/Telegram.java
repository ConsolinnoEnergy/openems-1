package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.genibus.ConnectionHandler;
import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.snksoft.crc.CRC;

/**
 * Class to represent a Genibus telegram.
 */
public class Telegram {
    byte startDelimiter;
    byte length;
    byte destinationAddress;
    byte sourceAddress;
    ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
    Map<Integer, ArrayList<GenibusTask>> telegramTaskList = new HashMap<>();
    private PumpDevice pumpDevice;
    private int answerTelegramPduLengthEstimate = 0;
    private int answerTelegramPduLength = 0;

    private final Logger log = LoggerFactory.getLogger(Telegram.class);

    /**
     * Set the answer telegram PDU length. This value is used together with the transmission time to calculate
     * ’millisecondsPerByte’ in PumpDevice.java.
     *
     * @param answerTelegramPduLength the answer telegram PDU length.
     */
    public void setAnswerTelegramPduLength(int answerTelegramPduLength) {
        this.answerTelegramPduLength = answerTelegramPduLength;
    }

    /**
     * Get the answer telegram PDU length. This value is used together with the transmission time to calculate
     * ’millisecondsPerByte’ in PumpDevice.java.
     *
     * @return the answer telegram PDU length.
     */
    public int getAnswerTelegramPduLength() {
        return this.answerTelegramPduLength;
    }

    /**
     * Set the answer telegram PDU length estimate. This is used to estimate the time it takes to send and receive a
     * telegram.
     *
     * @param answerTelegramPduLengthEstimate the answer telegram PDU length estimate.
     */
    public void setAnswerTelegramPduLengthEstimate(int answerTelegramPduLengthEstimate) {
        this.answerTelegramPduLengthEstimate = answerTelegramPduLengthEstimate;
    }

    /**
     * Set the answer telegram PDU length estimate. This is used to estimate the time it takes to send and receive a
     * telegram.
     * This value is close to the upper limit, assuming the response to an INFO is always 4 byte. ASCII response is
     * estimated to be 30 bytes.
     *
     * @return the answer telegram PDU length estimate.
     */
    public int getAnswerTelegramPduLengthEstimate() {
        return this.answerTelegramPduLengthEstimate;
    }

    /**
     * Set the pump device.
     *
     * @param pumpDevice the pump device.
     */
    public void setPumpDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    /**
     * Get the pump device.
     *
     * @return the pump device.
     */
    public PumpDevice getPumpDevice() {
        return this.pumpDevice;
    }

    /**
     * Get the start delimiter byte.
     *
     * @return the start delimiter byte.
     */
    public byte getStartDelimiter() {
        return this.startDelimiter;
    }

    /**
     * Set the start delimiter byte.
     *
     * @param startDelimiter the start delimiter byte.
     */
    public void setStartDelimiter(byte startDelimiter) {
        this.startDelimiter = startDelimiter;
    }

    /**
     * Set StartDelimiter to 0x27.
     */
    public void setStartDelimiterDataRequest() {
        this.setStartDelimiter((byte) 0x27);
    }

    /**
     * Get the telegram length. This length is the one used in the second byte of the telegram header.
     * The number means ’all bytes after Length, excluding crc’, which is is the PDU byte count + 2 bytes for the
     * destination and source address.
     * The actual telegram size is 4 bytes larger. The 4 extra bytes are the Start Delimiter, the Length, and 2 bytes CRC.
     *
     * @return the telegram length.
     */
    public byte getLength() {
        return this.updateLength();
    }

    /**
     * Set the length of the telegram from an int and return it a as a byte.
     *
     * @param length the length of the telegram as an int.
     * @return the length of the telegram as a byte.
     */
    public byte setLength(int length) {
        this.length = (byte) length;
        return this.length;
    }

    /**
     * Get the PDU length, add 2 for dest and src address.
     *
     * @return the PDU length increased by 2 as a byte.
     */
    public byte updateLength() {
        return this.setLength(this.protocolDataUnit.getPduLength() + 2);
    }

    /**
     * Get the destination address as a byte.
     *
     * @return the destination address as a byte.
     */
    public byte getDestinationAddress() {
        return this.destinationAddress;
    }

    /**
     * Set the destination address.
     *
     * @param destinationAddress the destination address.
     */
    public void setDestinationAddress(int destinationAddress) {
        this.destinationAddress = (byte) destinationAddress;
    }

    /**
     * Get the source address.
     *
     * @return the source address.
     */
    public byte getSourceAddress() {
        return this.sourceAddress;
    }

    /**
     * Set the source address.
     *
     * @param sourceAddress the source address.
     */
    public void setSourceAddress(int sourceAddress) {
        this.sourceAddress = (byte) sourceAddress;
    }

    /**
     * Get the PDU.
     *
     * @return the PDU.
     */
    public ProtocolDataUnit getProtocolDataUnit() {
        return this.protocolDataUnit;
    }

    /**
     * Set the PDU.
     *
     * @param protocolDataUnit the PDU.
     */
    public void setProtocolDataUnit(ProtocolDataUnit protocolDataUnit) {
        this.protocolDataUnit = protocolDataUnit;
    }

    /**
     * Set the telegram task list.
     *
     * @param telegramTaskList the telegram task list.
     */
    public void setTelegramTaskList(Map<Integer, ArrayList<GenibusTask>> telegramTaskList) {
        this.telegramTaskList = telegramTaskList;
    }

    /**
     * Get the telegram task list.
     *
     * @return the telegram task list.
     */
    public Map<Integer, ArrayList<GenibusTask>> getTelegramTaskList() {
        return this.telegramTaskList;
    }

    /**
     * Get the bytes for the CRC calculation.
     *
     * @return the bytes for CRC calculation.
     */
    public byte[] getBytesForCrc() {
        ByteArrayOutputStream byteList = new ByteArrayOutputStream();

        // Add length
        byteList.write(this.getLength());
        // Add destination address
        byteList.write(this.getDestinationAddress());
        byteList.write(this.getSourceAddress());
        // Add pdu
        try {
            byteList.write(this.getProtocolDataUnit().getPduAsByteArray());
        } catch (IOException e) {
            this.log.error("Error collecting PDU data: " + e.getMessage());
        }

        return byteList.toByteArray();
    }

    /**
     * Get the telegram as a byte array.
     *
     * @return the telegram as a byte array.
     */
    public byte[] getTelegramAsByteArray() {

        byte[] byteListForCrc = this.getBytesForCrc();

        ByteArrayOutputStream byteList = new ByteArrayOutputStream();
        // Add start delimiter (sd)
        byteList.write(this.getStartDelimiter());
        try {
            byteList.write(byteListForCrc);
        } catch (IOException e) {
            this.log.error("Error collecting telegram bytes: " + e.getMessage());
        }
        // Calc crchigh and crclow, create complete telegram
        try {
            byteList.write(getCrc(byteListForCrc));
        } catch (IOException e) {
            this.log.error("Error collecting CRC bytes: " + e.getMessage());
        }

        return byteList.toByteArray();
    }

    /**
     * Create a new telegram from a byte array assuming correct crc.
     *
     * @param bytes the byte array.
     * @param caller the caller of the method.
     * @return the telegram.
     */
    public static Telegram parseEventStream(byte[] bytes, ConnectionHandler caller) {
        try {
            Telegram telegram = new Telegram();
            telegram.setStartDelimiter(bytes[0]);    //Start Delimiter (SD)
            telegram.setLength(bytes[1]);            //Length (LE)
            telegram.setDestinationAddress(bytes[2]);//Destination Address (DA)
            telegram.setSourceAddress(bytes[3]);    //Source Address (SA)
            telegram.setProtocolDataUnit(ProtocolDataUnit.parseBytesToPdu(bytes, caller));//Protocol Data Unit (PDU)
            return telegram;
        } catch (Exception e) {
            caller.logWarning("Error parsing bytes for telegram: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calculate the CRC from the provided byte array.
     *
     * @param bytes the provided byte array.
     * @return the CRC as a byte array.
     */
    public static byte[] getCrc(byte[] bytes) {
        long crc = CRC.calculateCRC(CRC.Parameters.CCITT, bytes) ^ 0xFFFF;

        byte[] ret = new byte[2];
        ret[1] = (byte) (crc & 0xff);
        ret[0] = (byte) ((crc >> 8) & 0xff);

        return ret;
    }
}
