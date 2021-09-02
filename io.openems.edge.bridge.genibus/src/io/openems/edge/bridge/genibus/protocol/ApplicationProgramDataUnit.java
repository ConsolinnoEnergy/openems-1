package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ApplicationProgramDataUnit {
    short apduHead;
    byte apduHeadClass;
    byte apduHeadOsAck;
    boolean isOperationSpecifier;
    // 0...63 Byte
    ByteArrayOutputStream apduDataFields = new ByteArrayOutputStream();

    /**
     * Get the APDU head.
     * @return the APDU head.
     */
    public int getHead() {
        return this.apduHead;
    }

    /**
     * Set the APDU head.
     * @param apduHead the APDU head.
     */
    public void setHead(short apduHead) {
        this.apduHead = apduHead;
    }

    /**
     * Get the APDU data fields.
     * @return the APDU data fields
     */
    public byte[] getDataFields() {
        return this.apduDataFields.toByteArray();
    }

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
     * Set the APDU head class to 2, meaning ’measured data’.
     */
    public void setHeadClassMeasuredData() {
        this.apduHeadClass = 2;
    }

    /**
     * Set the APDU head class to 3, meaning ’commands’.
     */
    public void setHeadClassCommands() {
        this.apduHeadClass = 3;
    }

    /**
     * Set the APDU head class to 4, meaning ’configuration parameters’.
     */
    public void setHeadClassConfigurationParameters() {
        this.apduHeadClass = 4;
    }

    /**
     * Set the APDU head class to 5, meaning ’class reference values’.
     */
    public void setHeadClassReferenceValues() {
        this.apduHeadClass = 5;
    }

    /**
     * Set the APDU head class to 7, meaning ’ASCII sting’.
     */
    public void setHeadClassAsciiStrings() {
        this.apduHeadClass = 7;
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
     * Set the APDU head OSACK to 0, meaning 'get'.
     */
    public void setOsGet() {
        this.apduHeadOsAck = 0;
    }

    /**
     * Set the APDU head OSACK to 2, meaning 'set'.
     */
    public void setOsSet() {
        this.apduHeadOsAck = 2;
    }

    /**
     * Set the APDU head OSACK to 3, meaning ’info’.
     */
    public void setOsInfo() {
        this.apduHeadOsAck = 3;
    }

    public void setAckOk() {
        this.apduHeadOsAck = 0;
    }

    public void setAckDataClassUnknown() {
        this.apduHeadOsAck = (byte) 0x01;
    }

    public void setAckDataItemIdUnknown() {
        this.apduHeadOsAck = (byte) 0x10;
    }

    /**
     * Operation illegal or Data Class write buffer is full, APDU data field will be
     * empty.
     */
    public void setAckIllegalOrBuffFull() {
        this.apduHeadOsAck = (byte) 0x11;
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
     * Get the operation specifier boolean.
     * @return the operation specifier boolean.
     */
    public boolean isOperationSpecifier() {
        return this.isOperationSpecifier;
    }

    public void setOperationSpecifier(boolean isOperationSpecifier) {
        this.isOperationSpecifier = isOperationSpecifier;
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
     * @return the complete APDU as a byte array.
     */
    public byte[] getCompleteApduAsByteArray() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        // Head bytes
        bytes.write(this.getHeadClass()); // Set Class
        bytes.write(this.getHeadOsAckLength()); // Set OS/ACK, Length of APDU data field
        // Data bytes
        try {
            bytes.write(this.apduDataFields.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
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
