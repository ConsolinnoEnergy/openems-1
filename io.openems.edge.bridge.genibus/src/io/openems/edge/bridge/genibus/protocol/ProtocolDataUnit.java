package io.openems.edge.bridge.genibus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.bridge.genibus.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to represent the Protocol Data Unit (PDU) part of a Genibus telegram.
 */
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
     *
     * @return the PDU as a byte array.
     */
    public byte[] getPduAsByteArray() {
        ByteArrayOutputStream byteList = new ByteArrayOutputStream();

        // APDU units
        for (ApplicationProgramDataUnit applicationProgramDataUnit : this.getApplicationProgramDataUnitList()) {
            try {
                byteList.write(applicationProgramDataUnit.getCompleteApduAsByteArray(this.log));
            } catch (IOException e) {
                this.log.info(e.getMessage());
            }
        }
        // Request from slave (rfs), optional, ignored for now, used in multi master networks

        return byteList.toByteArray();
    }

    /**
     * Create a ProtocolDataUnit (PDU) from a telegram provided as a byte array.
     * Parses the bytes to build a ProtocolDataUnit object.
     *
     * @param bytes the telegram as a byte array.
     * @param caller the caller that can log an error.
     * @return the ProtocolDataUnit object.
     */
    public static ProtocolDataUnit parseBytesToPdu(byte[] bytes, ConnectionHandler caller) {
        ProtocolDataUnit protocolDataUnit = new ProtocolDataUnit();
        try {
            // The first 4 bytes are the telegram header. Skip those.
            int apduStartIndex = 4;

            /* apduStartIndex should now be at the start of an ApplicationProgramDataUnit (APDU). Test if the current
               byte is the start of an APDU by checking the remaining bytes. If it is the start of an APDU, there are at
               least 4 bytes left (this byte + another 3):
               2 bytes APDU header, and 2 bytes for the CRC. An APDU can be empty and consist of just the APDU header. */
            while (apduStartIndex < bytes.length - 3) {
                /* There are enough bytes left that the byte at ’apduStartIndex’ should be the start of an APDU. Create
                   an APDU object, parse the APDU header and save it to the object. */
                ApplicationProgramDataUnit applicationProgramDataUnit = new ApplicationProgramDataUnit();
                applicationProgramDataUnit.setHeadClass(bytes[apduStartIndex]); // First byte of the header.

                /* The second byte of the header contains two things, APDU length and osack. Store the byte, then use
                   bitwise operators to separate the two things. */
                byte osasklength = bytes[apduStartIndex + 1];
                byte apduLength = (byte) (osasklength & 0x3F); // This part of the 2nd header byte is the APDU length.
                byte osack = (byte) ((osasklength >> 6) & 0x03); // This part of the 2nd header byte is the osack.

                applicationProgramDataUnit.setHeadOsAck(osack);

                /* We got the length of the APDU from the header. Now read all the APDU data fields and store them in
                   the APDU object. */
                ByteArrayOutputStream bytesStreamApduRelevant = new ByteArrayOutputStream();
                bytesStreamApduRelevant.write(bytes, apduStartIndex + 2, apduLength);   // Start after header.
                applicationProgramDataUnit.setDataFields(bytesStreamApduRelevant);
                protocolDataUnit.putApdu(applicationProgramDataUnit);

                /* Add the length of this APDU to apduStartIndex, so that apduStartIndex points to the position where a
                   possible next ADPU would start. apduLength does not include the 2 header bytes, so add these. */
                apduStartIndex += apduLength + 2;
            }
        } catch (Exception e) {
            caller.logWarning("Error in parseBytes of ProtocolDataUnit: " + e.getMessage());
        }

        return protocolDataUnit;
    }
}
