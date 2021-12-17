package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolDataUnit {
    List<ApplicationProgramDataUnit> applicationProgramDataUnitList = new ArrayList<ApplicationProgramDataUnit>();
    char requestFromSlave;
    int pduLength = 0;
    private final Logger log = LoggerFactory.getLogger(ProtocolDataUnit.class);

    /**
     * Gets the APDU list.
     * @return the APDU list.
     */
    public List<ApplicationProgramDataUnit> getApplicationProgramDataUnitList() {
        return this.applicationProgramDataUnitList;
    }

    /**
     * Set the APDU list.
     * @param applicationProgramDataUnitList the APDU list.
     */
    public void setApplicationProgramDataUnitList(List<ApplicationProgramDataUnit> applicationProgramDataUnitList) {
        this.applicationProgramDataUnitList = applicationProgramDataUnitList;
    }

    /**
     * Get the request from slave parameter.
     * @return the request from slave parameter.
     */
    public char getRequestFromSlave() {
        return this.requestFromSlave;
    }

    /**
     * Set the request from slave parameter.
     * @param requestFromSlave the request from slave parameter.
     */
    public void setRequestFromSlave(char requestFromSlave) {
        this.requestFromSlave = requestFromSlave;
    }

    /**
     * Add an APDU to the APDU list.
     * @param applicationProgramDataUnit the APDU.
     */
    public void putApdu(ApplicationProgramDataUnit applicationProgramDataUnit) {
        this.applicationProgramDataUnitList.add(applicationProgramDataUnit);
        this.updatePduLength();
    }

    /**
     * Remove an APDU from the APDU list.
     * @param applicationProgramDataUnit the APDU.
     */
    public void pullApdu(ApplicationProgramDataUnit applicationProgramDataUnit) {
        this.applicationProgramDataUnitList.remove(applicationProgramDataUnit);
        this.updatePduLength();
    }

    /**
     * Update the PDU length parameter.
     */
    public void updatePduLength() {
        int length = 0;
        for (ApplicationProgramDataUnit applicationProgramDataUnit : this.getApplicationProgramDataUnitList()) {
            // Add apdu length + 2 for apdu head
            length += applicationProgramDataUnit.getApduLength();
        }
        this.pduLength = length;
    }

    /**
     * Get the PDU length. This is the amount of bytes of all APDUs, including APDU header. A possible ’Request From Slave’
     * byte is not included.
     *
     * @return the PDU length.
     */
    public int getPduLength() {
        return this.pduLength;
    }

    /**
     * Get the PDU as a byte array.
     * @return the PDU as a byte array.
     */
    public byte[] getPduAsByteArray() {
        ByteArrayOutputStream byteList = new ByteArrayOutputStream();

        // APDU units
        for (ApplicationProgramDataUnit applicationProgramDataUnit : this.getApplicationProgramDataUnitList()) {
            try {
                byteList.write(applicationProgramDataUnit.getCompleteApduAsByteArray());
            } catch (IOException e) {
                this.log.info(e.getMessage());
            }
        }
        // Request from slave (rfs), optional, ignored for now, used in multi master
        // networks

        return byteList.toByteArray();
    }

    /**
     * Create PDU from incoming bytes.
     *
     * @param bytes the incoming bytes as a byte array.
     * @return the PDU.
     */
    public static ProtocolDataUnit parseBytesToPdu(byte[] bytes) {
        ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
        try {
            // Parse APDU Blocks
            int apduStartIndex = 4;
            while (apduStartIndex < bytes.length - 3) {
                ApplicationProgramDataUnit applicationProgramDataUnit = new ApplicationProgramDataUnit();
                applicationProgramDataUnit.setHeadClass(bytes[apduStartIndex]);
                byte osasklength = bytes[apduStartIndex + 1]; // prepare block os/ack and length for splitting
                byte apduLength = (byte) (osasklength & 0x3F); // cast length
                byte osack = (byte) ((osasklength >> 6) & 0x03); // shift last two bytes to right corner, cast other
                // bytes
                // away
                applicationProgramDataUnit.setHeadOsAck(osack);

                ByteArrayOutputStream bytesStreamApduRelevant = new ByteArrayOutputStream();
                bytesStreamApduRelevant.write(bytes, apduStartIndex + 2, apduLength);
                applicationProgramDataUnit.setDataFields(bytesStreamApduRelevant);
                protocolDataUnit.putApdu(applicationProgramDataUnit);
                apduStartIndex += apduLength + 2;
            }
        } catch (Exception e) {
            System.out.println("Error in parseBytes of ProtocolDataUnit: " + e.getMessage());
        }

        return protocolDataUnit;
    }
}
