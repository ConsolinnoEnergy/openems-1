package io.openems.edge.bridge.genibus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.bridge.genibus.api.PumpDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
*/
import com.fazecast.jSerialComm.SerialPort;

import io.openems.edge.bridge.genibus.protocol.Telegram;

/**
 * Handles the serial connection for communicating with Genibus devices.
 */
public class ConnectionHandler {

    private SerialPort serialPort;
    protected String portName;
    LocalDateTime errorTimestamp;
    LocalDateTime errorTimestamp2;

    OutputStream os;
    InputStream is;

    private final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);
    protected final GenibusImpl parent;

    /**
     * Constructor.
     *
     * @param parent the GenibusImpl creating the ConnectionHandler.
     */
    public ConnectionHandler(GenibusImpl parent) {
        this.errorTimestamp = LocalDateTime.now();
        this.parent = parent;
    }

    /**
     * Start the serial connection to send and receive data to/from the devices.
     * Will return false if nothing is plugged in at the specified port or the port does not exist.
     *
     * @param portName name of the serial port to use.
     * @return true if the connection could be opened, false if something went wrong.
     */
    public boolean startConnection(String portName) {   // Trying fazecast library for serial port. jrxrx library could not find port sc1 on leaflet.
        this.portName = portName;
        SerialPort[] serialPortsOfSystem = SerialPort.getCommPorts();
        boolean serialPortFound = false;
        if (serialPortsOfSystem.length == 0) {
            // So that error message is only sent once.
            if (this.parent.connectionOk || ChronoUnit.SECONDS.between(this.errorTimestamp, LocalDateTime.now()) >= 5) {
                this.errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Couldn't start serial connection. No serial ports found or nothing plugged in.");
            }
            return false;
        }
        StringBuilder portList = new StringBuilder();
        for (SerialPort tmpSerialPort : serialPortsOfSystem) {
            String systemPortName = "/dev/" + tmpSerialPort.getSystemPortName();
            portList.append(tmpSerialPort).append(", ");
            if (systemPortName.equals(portName)) {
                this.serialPort = tmpSerialPort;
                serialPortFound = true;
            }
        }
        // Delete ", " at end
        if (portList.length() > 2) {
            portList.delete(portList.length() - 2, portList.length());
        }

        if (serialPortFound) {
            this.parent.logInfo(this.log, "--Starting serial connection--");
            this.parent.logInfo(this.log, "Ports found: " + portList);
            this.serialPort.setNumDataBits(8);
            this.serialPort.setParity(SerialPort.NO_PARITY);
            this.serialPort.setNumStopBits(1);
            this.serialPort.setBaudRate(9600);
            this.serialPort.openPort();

            this.is = this.serialPort.getInputStream();
            this.os = this.serialPort.getOutputStream();

            this.parent.logInfo(this.log, "Connection opened on port " + portName);
            return true;
        } else {
            if (this.parent.connectionOk || ChronoUnit.SECONDS.between(this.errorTimestamp, LocalDateTime.now()) >= 5) {
                this.errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Configuration error: The specified serial port " + portName
                        + " does not match any of the available ports or nothing is plugged in. "
                        + "Please check configuration and/or make sure the connector is plugged in.");
                this.parent.logError(this.log, "Ports found: " + portList);
            }
            return false;
        }
    }

    /* // Old serial port handling, uses jrxtx libraries
    public boolean start(String portName) {
        this.portName = portName;
        String[] serialPortsOfSystem =  SerialPortBuilder.getSerialPortNames();
        boolean portFound = false;
        if (serialPortsOfSystem.length == 0) {
            // So that error message is only sent once.
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Couldn't start serial connection. No serial ports found or nothing plugged in.");
            }
            return false;
        }
        StringBuilder portList = new StringBuilder();
        for (String entry : serialPortsOfSystem) {
            portList.append(entry).append(", ");
            if (entry.contains(this.portName)) {
                portFound = true;
            }
        }
        // Delete ", " at end
        if (portList.length() > 2) {
            portList.delete(portList.length() - 2, portList.length());
        }

        if (portFound) {
            this.parent.logInfo(this.log, "--Starting serial connection--");
            this.parent.logInfo(this.log, "Ports found: " + portList);
            try {
                serialPort = SerialPortBuilder.newBuilder(portName).setBaudRate(9600)
                        .setDataBits(DataBits.DATABITS_8).setParity(Parity.NONE).setStopBits(StopBits.STOPBITS_1).build();
                is = serialPort.getInputStream();
                os = serialPort.getOutputStream();
            } catch (IOException e) {
                // So that error message is only sent once.
                if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                    errorTimestamp = LocalDateTime.now();
                    this.parent.logError(this.log, "Failed to open connection on port " + portName);
                }
                e.printStackTrace();
                return false;
            }
            this.parent.logInfo(this.log, "Connection opened on port " + portName);
            return true;
        } else {
            if (parent.connectionOk || ChronoUnit.SECONDS.between(errorTimestamp, LocalDateTime.now()) >= 5) {
                errorTimestamp = LocalDateTime.now();
                this.parent.logError(this.log, "Configuration error: The specified serial port " + portName
                        + " does not match any of the available ports or nothing is plugged in. " +
                        "Please check configuration and/or make sure the connector is plugged in.");
                this.parent.logError(this.log, "Ports found: " + portList);
            }
            return false;
        }

    }
    */


    /**
     * Checks the status of the serial connection by testing if the outgoing stream still exists and can be used.
     *
     * @return true if the connection is ok of false if not.
     */
    public boolean checkConnectionStatus() {
        if (this.os != null) {
            // os in not null when a connection was established at some point by the start() method.
            try {
                /* Test the connection by trying to write something to the output stream os. Writes a single 0, should
                   not interfere with anything. */
                this.os.write(0);
            } catch (IOException e) {

                // So that error message is only sent once.
                if (this.parent.connectionOk || ChronoUnit.SECONDS.between(this.errorTimestamp, LocalDateTime.now()) >= 5) {
                    this.errorTimestamp2 = LocalDateTime.now();
                    this.parent.logError(this.log, "Serial connection lost on port " + this.portName + ". Attempting to reconnect...");
                }

                if (this.serialPort != null) {
                    this.serialPort.closePort();
                }
                return false;
            }
            return true;
        } else {
            // If os is null, there has not been a connection yet.
            return false;
        }
    }


    /**
     * Checks the received bytes to test if they form a complete telegram.
     *
     * @param bytesCurrentPackage the bytes to check.
     * @param verbose print info to log or not.
     * @return true if it is a complete telegram or false if not.
     */
    public boolean packageOK(byte[] bytesCurrentPackage, boolean verbose) {
        // Look for Start Delimiter (SD).
        boolean sdOK = false;
        if (bytesCurrentPackage.length >= 1) {
            switch (bytesCurrentPackage[0]) {
                case 0x27:
                case 0x26:
                case 0x24:
                    sdOK = true;
                    break;
                default:
                    sdOK = false;
            }
        }
        if (!sdOK) { // Wrong package start, reset.
            if (verbose) {
                this.parent.logWarn(this.log, "SD not OK");
            }
            return false;
        }
        // Look for Length (LE), Check package length match.
        boolean lengthOK = false;
        if (bytesCurrentPackage.length >= 2 && bytesCurrentPackage[1] == bytesCurrentPackage.length - 4) {
            lengthOK = true;
        }
        if (!lengthOK) { // Collect more data.
            if (verbose) {
                this.parent.logWarn(this.log, "Length not OK");
            }
            return false;
        }
        // Check crc from relevant message part.

        ByteArrayOutputStream bytesCrcRelevant = new ByteArrayOutputStream();
        bytesCrcRelevant.write(bytesCurrentPackage, 1, bytesCurrentPackage.length - 3);
        byte[] crc = Telegram.getCrc(bytesCrcRelevant.toByteArray());
        int length = bytesCurrentPackage.length;

        if (bytesCurrentPackage[length - 2] != crc[0] || bytesCurrentPackage[length - 1] != crc[1]) {
            this.parent.logWarn(this.log, "CRC compare not OK");
            return false; // Cancel operation.
        }
        return true;
    }

    /**
     * Terminates the serial connection.
     */
    public void stop() {
        if (this.os != null) {
            try {
                this.os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.os = null;
        this.is = null;
        if (this.serialPort == null) {
            // Happens when no valid serial port was selected when starting the module.
            return;
        }

        if (this.serialPort.isOpen() == false) {
            return;
        }
        this.serialPort.closePort();
    }


    /**
     * Sends the telegram and returns the response telegram.
     *
     * @param timeout how long to wait for the response telegram before aborting.
     * @param telegram the telegram to send.
     * @param debug print info to log or not.
     * @return the response telegram if available, otherwise null.
     */
    public Telegram writeTelegram(long timeout, Telegram telegram, boolean debug) {

        // Make sure the input is empty before sending a new telegram.
        try {
            if (this.is.available() > 0) {
                this.parent.logWarn(this.log, "Input buffer should be empty, but it is not. Trying to clear it.");
                long startTime = System.currentTimeMillis();
                int numRead;
                byte[] readBuffer = new byte[1024];
                // Timeout here is not actually related to Genibus timeout at all. But the value fits the requirement.
                while (this.is.available() > 0 || (System.currentTimeMillis() - startTime) < GenibusImpl.GENIBUS_TIMEOUT_MS) {
                    numRead = Math.min(this.is.available(), 1024);  // readBuffer is size 1024.
                    this.is.read(readBuffer, 0, numRead);   // Clear input stream.
                }
                if (this.is.available() > 0) {
                    this.parent.logError(this.log, "Can't send telegram, something is flooding the input buffer!");
                    return null;
                }
            }
        } catch (Exception e) {
            this.parent.logError(this.log, "Error while receiving data: " + e.getMessage());
            e.printStackTrace();
        }

        // Send telegram and handle response.
        try {
            // Send request.
            byte[] bytes = telegram.getTelegramAsByteArray();
            this.os.write(bytes);
            if (debug) {
                //this.parent.logInfo(this.log, "Bytes send: " + bytesToHex(bytes));    // Hex values.
                this.parent.logInfo(this.log, "Bytes send: " + bytesToInt(bytes));  // Int values.
            }

            // Method to handle response telegram.
            return this.handleResponse(timeout, debug, telegram.getPumpDevice());
        } catch (Exception e) {
            this.parent.logError(this.log, "Error while sending data: " + e.getMessage());
        }
        return null;
    }

    /**
     * Listens for a response telegram and returns it.
     * The timeout parameter is the timeout until the first bytes of a response, and then again the timeout for the
     * transmission of the answer. Tests have shown that the time until the first byte of the answer is detected is much
     * longer than the time from then to the end of the transmission, probably because of buffering. For example a
     * telegram where the handleTelegram() method took 109 ms took 94 ms until the first answer byte was detected
     * (= answer time clock) and then 10 ms until the transmission was completed.
     * The GenibusBridge measures the execution time of handleTelegram() and tries to estimate it (in the
     * estimateTelegramAnswerTime() method). Since the answer time clock is about 90% of the handleTelegram() execution
     * time, the handleTelegram() time estimate is used for the baseline timeout of the answer time clock. Added to that
     * is the Genibus timeout (default 60 ms as per spec), and the configurable timeoutIncreaseMs.
     * The same timeout value is then also used for the transmission time. Not accurate, but good enough. Just to have a
     * not too long timer that scales with the telegram length. Transmission timeout is a very rare event, in contrast
     * to answer timeout that happens whenever a device is switched off.
     *
     * @param timeout how long to wait for the response telegram before aborting, and for the transmission to finish before aborting.
     * @param debug print info to log or not.
     * @param pumpDevice the pumpDevice the telegram is coming from.
     * @return the received telegram.
     */
    private Telegram handleResponse(long timeout, boolean debug, PumpDevice pumpDevice) {
        try {
            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < timeout + GenibusImpl.GENIBUS_TIMEOUT_MS) {
                // This "while" only tests for timeout as long as is.available() <= 0, since there is a break at the end.
                if (this.is.available() <= 0) {
                    continue;
                }
                if (debug) {
                    this.parent.logInfo(this.log, "Telegram answer time: " + (System.currentTimeMillis() - startTime)
                            + ", timeout: " + (timeout + GenibusImpl.GENIBUS_TIMEOUT_MS));
                }
                int numRead;
                byte[] readBuffer = new byte[1024];
                List<Byte> completeInput = new ArrayList<>();
                boolean transferOk = false;
                /* Reset timer. This timeout exits the loop in case the telegram is corrupted and transferOk will not
                   become true. */
                startTime = System.currentTimeMillis();
                while (transferOk == false && (System.currentTimeMillis() - startTime) < timeout + GenibusImpl.GENIBUS_TIMEOUT_MS) {
                    if (this.is.available() <= 0) {
                        continue;
                    }
                    numRead = this.is.available();
                    if (numRead > 1024) {
                        numRead = 1024; // readBuffer is size 1024.
                    }
                    this.is.read(readBuffer, 0, numRead);
                    for (int counter1 = 0; counter1 < numRead; counter1++) {
                        completeInput.add(readBuffer[counter1]);
                    }
                    byte[] receivedDataTemp = new byte[completeInput.size()];
                    int counter2 = 0;
                    for (byte entry:completeInput) {
                        receivedDataTemp[counter2] = entry;
                        counter2++;
                    }
                    transferOk = this.packageOK(receivedDataTemp, false);
                    if (debug && transferOk) {
                        this.parent.logInfo(this.log, "Telegram answer transmit time: " + (System.currentTimeMillis() - startTime)
                                + ", timeout: " + (timeout + GenibusImpl.GENIBUS_TIMEOUT_MS));
                    }
                }
                byte[] receivedData = new byte[completeInput.size()];
                int counter2 = 0;
                for (byte entry:completeInput) {
                    receivedData[counter2] = entry;
                    counter2++;
                }

                if (debug) {
                    //this.parent.logInfo(this.log, "Data received: " + bytesToHex(receivedData));  // Hex values.
                    this.parent.logInfo(this.log, "Data received: " + bytesToInt(receivedData));    // Int values.
                }

                if (this.packageOK(receivedData, true)) {
                    // GENIbus requirement: wait 3 ms after reception of a telegram before sending the next one.
                    Thread.sleep(3);
                    if (debug) {
                        this.parent.logInfo(this.log, "CRC Check ok.");
                    }
                    // If all done, return the response telegram.
                    return Telegram.parseEventStream(receivedData);
                }
                break;
            }

        } catch (Exception e) {
            this.parent.logError(this.log, "Error while receiving data: " + e.getMessage());
            e.printStackTrace();
        }
        this.parent.logWarn(this.log, "Telegram response timeout");
        int timeoutCounter = pumpDevice.getConnectionTimeoutCounter();
        if (timeoutCounter < 3) {
            timeoutCounter++;
        }
        pumpDevice.setConnectionTimeoutCounter(timeoutCounter);
        return null;
    }

    /**
     * Convert a byte array to a hex string, with a space between each byte.
     *
     * @param data the byte array.
     * @return the hex string.
     */
    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("0x%02x ", b));
        }
        return sb.toString();
    }

    /**
     * Convert a byte array to an int string, with a space between each byte.
     *
     * @param data the byte array.
     * @return the int string.
     */
    private static String bytesToInt(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int convert = Byte.toUnsignedInt(b);
            sb.append(String.format("%d ", convert));
        }
        return sb.toString();
    }
}
