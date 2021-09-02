package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.bridge.genibus.api.task.GenibusTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.snksoft.crc.CRC;

public class Telegram {
    byte startDelimiter;
    byte length;
    byte destinationAddress;
    byte sourceAddress;
    ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
    short crcHighOrder;
    short crcLowOrder;
    Map<Integer, ArrayList<GenibusTask>> telegramTaskList = new HashMap<>();
    private PumpDevice pumpDevice;

    // This variable is used to store the estimate, as well as the actual. Which one it is depends on the timing.
    private int answerTelegramLength = 0;

    private final Logger log = LoggerFactory.getLogger(Telegram.class);

    /**
     * Set the answer telegram length.
     * @param answerTelegramLength the answer telegram length.
     */
    public void setAnswerTelegramLength(int answerTelegramLength) {
        this.answerTelegramLength = answerTelegramLength;
    }

    /**
     * Get the answer telegram length.
     * @return the answer telegram length.
     */
    public int getAnswerTelegramLength() {
        return this.answerTelegramLength;
    }

    /**
     * Set the pump device.
     * @param pumpDevice the pump device.
     */
    public void setPumpDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    /**
     * Get the pump device.
     * @return the pump device.
     */
    public PumpDevice getPumpDevice() {
        return this.pumpDevice;
    }

    /**
     * Get the start delimiter byte.
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
     * Set StartDelimiter to 0x26.
     */
    public void setStartDelimiterDataMessage() {
        this.setStartDelimiter((byte) 0x26);
    }

    /**
     * Set StartDelimiter to 0x24.
     */
    public void setStartDelimiterDataReply() {
        this.setStartDelimiter((byte) 0x24);
    }

    /**
     * Get the telegram length.
     * @return the telegram length.
     */
    public byte getLength() {
        return this.updateLength();
    }

    /**
     * Set the length of the telegram from an int and return it a as a byte.
     * @param length the length of the telegram as an int.
     * @return the length of the telegram as a byte.
     */
    public byte setLength(int length) {
        this.length = (byte) length;
        return this.length;
    }

    /**
     * Get the PDU length, add 2 for dest and src address.
     * @return the PDU length increased by 2 as a byte.
     */
    public byte updateLength() {
        return this.setLength(this.protocolDataUnit.getPduLength() + 2);
    }

    /**
     * Get the destination address as a byte.
     * @return the destination address as a byte.
     */
    public byte getDestinationAddress() {
        return this.destinationAddress;
    }

    /**
     * Set the destination address.
     * @param destinationAddress the destination address.
     */
    public void setDestinationAddress(int destinationAddress) {
        this.destinationAddress = (byte) destinationAddress;
    }

    /**
     * Get the source address.
     * @return the source address.
     */
    public byte getSourceAddress() {
        return this.sourceAddress;
    }

    /**
     * Set the source address.
     * @param sourceAddress the source address.
     */
    public void setSourceAddress(int sourceAddress) {
        this.sourceAddress = (byte) sourceAddress;
    }

    /**
     * Get the PDU.
     * @return the PDU.
     */
    public ProtocolDataUnit getProtocolDataUnit() {
        return this.protocolDataUnit;
    }

    /**
     * Set the PDU.
     * @param protocolDataUnit the PDU.
     */
    public void setProtocolDataUnit(ProtocolDataUnit protocolDataUnit) {
        this.protocolDataUnit = protocolDataUnit;
    }

    /**
     * Set the telegram task list.
     * @param telegramTaskList the telegram task list.
     */
    public void setTelegramTaskList(Map<Integer, ArrayList<GenibusTask>> telegramTaskList) {
        this.telegramTaskList = telegramTaskList;
    }

    /**
     * Get the telegram task list.
     * @return the telegram task list.
     */
    public Map<Integer, ArrayList<GenibusTask>> getTelegramTaskList() {
        return this.telegramTaskList;
    }

    /**
     * Get the high order CRC.
     * @return the high order CRC.
     */
    public short getCrcHighOrder() {
        return this.crcHighOrder;
    }

    /**
     * Set the high order CRC.
     * @param crcHighOrder the high order CRC.
     */
    public void setCrcHighOrder(short crcHighOrder) {
        this.crcHighOrder = crcHighOrder;
    }

    /**
     * Get the low order CRC.
     * @return the low order CRC.
     */
    public short getCrcLowOrder() {
        return this.crcLowOrder;
    }

    /**
     * Set the low order CRC.
     * @param crcLowOrder the low order CRC.
     */
    public void setCrcLowOrder(short crcLowOrder) {
        this.crcLowOrder = crcLowOrder;
    }

    /**
     * Get the bytes for the CRC calculation.
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
            this.log.info(e.getMessage());
        }

        return byteList.toByteArray();
    }

    /**
     * Get the telegram as a byte array.
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
            this.log.info(e.getMessage());
        }
        // Calc crchigh and crclow, create complete telegram
        try {
            byteList.write(getCrc(byteListForCrc));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return byteList.toByteArray();
    }

    /**
     * Create a new telegram from a byte array assuming correct crc.
     *
     * @param bytes the byte array.
     * @return the telegram.
     */
    public static Telegram parseEventStream(byte[] bytes) {
        try {
            Telegram telegram = new Telegram();
            telegram.setStartDelimiter(bytes[0]);    //Start Delimiter (SD)
            telegram.setLength(bytes[1]);            //Length (LE)
            telegram.setDestinationAddress(bytes[2]);//Destination Address (DA)
            telegram.setSourceAddress(bytes[3]);    //Source Address (SA)
            telegram.setProtocolDataUnit(ProtocolDataUnit.parseBytesToPdu(bytes));//Protocol Data Unit (PDU)
            return telegram;
        } catch (Exception e) {
            System.out.println("Error parsing bytes for telegram: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calculate the CRC from the provided byte array.
     * @param bytes the provided byte array.
     * @return the CRC as a byte array.
     */
    public static byte[] getCrc(byte[] bytes) {
        long crc = 0;

        try {
            crc = CRC.calculateCRC(CRC.Parameters.CCITT, bytes) ^ 0xFFFF;
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] ret = new byte[2];
        ret[1] = (byte) (crc & 0xff);
        ret[0] = (byte) ((crc >> 8) & 0xff);

        return ret;
    }
}
