package io.openems.edge.bridge.genibus.protocol;

import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class to represent the Application Program Data Unit (APDU) part of a Genibus telegram.
 */
public class ApplicationProgramDataUnit {
    byte apduHeadClass;
    byte apduHeadOsAck;
    // 0...63 Byte
    ByteArrayOutputStream apduDataFields = new ByteArrayOutputStream();

    /**
     * Set the APDU data fields.
     * @param apduDataFields the APDU data fields.
     */
    public void setDataFields(ByteArrayOutputStream apduDataFields) {
        this.apduDataFields = apduDataFields;
    }

    /**
     * Put a single ADPU data field.
     * @param apduDataFieldByte the APDU data field.
     */
    public void putDataField(int apduDataFieldByte) {
        this.apduDataFields.write((byte) apduDataFieldByte);
    }

    /**
     * Get the APDU head class.
     * @return the APDU head class.
     */
    public byte getHeadClass() {
        return this.apduHeadClass;
    }

    /**
     * Set the APDU head class.
     * @param apduHeadClass the APDU head class.
     */
    public void setHeadClass(int apduHeadClass) {
        this.apduHeadClass = (byte) apduHeadClass;
    }

    /**
     * Get the shifted APDU head OSACK.
     * @return the shifted APDU head OSACK.
     */
    public byte getHeadOsAckShifted() {
        return (byte) (this.apduHeadOsAck << 6);
    }

    /**
     * Sets the head OSACK.
     * If isOperationSpecifier (OS) 00 / 0: GET, to read the value of Data Items 10
     * / 2: SET, to write the value of Data Items 11 / 3: INFO, to read the scaling
     * info of Data Items, an Info Data Structure will be returned Else (ACK) 00 /
     * 0: OK 01 / 1: Data Class unknown, reply APDU data field will be empty 10 / 2:
     * Data Item ID unknown, reply APDU data field contains first unknown ID 11 / 3:
     * Operation illegal or Data Class write buffer full, APDU data field will be
     * empty
     *
     * @param apduHeadOsAck the APDU head OSACK.
     */
    public void setHeadOsAck(int apduHeadOsAck) {
        this.apduHeadOsAck = (byte) apduHeadOsAck;
    }

    /**
     * Get num bytes in apdu without header bytes.
     *
     * @return the APDU da fields size.
     */
    public byte getLength() {
        return (byte) this.apduDataFields.size();
    }

    /**
     * Get apdu num bytes including header bytes.
     *
     * @return the total length of the APDU.
     */
    public byte getApduLength() {
        return (byte) (this.getLength() + 2);
    }

    /**
     * Get the length of the head OSACK.
     * @return the length of the head OSACK.
     */
    public byte getHeadOsAckLength() {
        return (byte) (this.getHeadOsAckShifted() | this.getLength());
    }

    /**
     * Get the complete APDU as a byte array.
     *
     * @param log logger to log an error.
     * @return the complete APDU as a byte array.
     */
    public byte[] getCompleteApduAsByteArray(Logger log) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        // Head bytes
        bytes.write(this.getHeadClass()); // Set Class
        bytes.write(this.getHeadOsAckLength()); // Set OS/ACK, Length of APDU data field
        // Data bytes
        try {
            bytes.write(this.apduDataFields.toByteArray());
        } catch (IOException e) {
            log.warn("Error writing bytes of APDU: " + e.getMessage());
        }

        return bytes.toByteArray();
    }

    /**
     * Get the head OSACK for the request telegram.
     * @return the head OSACK byte.
     */
    public byte getHeadOsAckForRequest() {
        return this.apduHeadOsAck;
    }
}
