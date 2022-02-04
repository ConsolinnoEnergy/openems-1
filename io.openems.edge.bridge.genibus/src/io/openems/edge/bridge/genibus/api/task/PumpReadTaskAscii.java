package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for reading ASCII data. The Genibus uses this data type to transmit a string, for example a serial number.
 */
public class PumpReadTaskAscii extends AbstractPumpTask {

    private final Channel<String> channel;
    private final Priority priority;

    private final StringBuilder charStorage = new StringBuilder();

    /**
     * Constructor. The Genibus uses this data type to transmit a string, for example a serial number.
     * There is no parameter for the headerNumber, since an ASCII task is always head class 7.
     *
     * @param address the Genibus data item address.
     * @param channel the string channel associated with this task.
     * @param priority the task priority. High, low or once.
     */
    public PumpReadTaskAscii(int address, Channel<String> channel, Priority priority) {
        super(7, address, "", 1);
        this.channel = channel;
        this.priority = priority;
    }

    /**
     * Allocate a byte from a response telegram to this ASCII task. This ASCII task should be the only thing in the
     * response APDU. Call this method for each byte of the APDU (excluding the header) in the order in which they
     * appear. An ASCII response is terminated by 0, so the last byte in the APDU should be a 0.
     * This method converts a byte to a char and appends it to a string, until a byte of value 0 is read. Then the
     * string is put in the OpenEMS channel associated with this task.
     *
     * @param data the response byte from the Genibus device for this task.
     */
    @Override
    public void processResponse(byte data) {
        if (data != 0) {
            char dataInAscii = (char) data;
            this.charStorage.append(dataInAscii);
        } else {
            this.channel.setNextValue(this.charStorage);
            this.charStorage.delete(0, this.charStorage.length() - 1);
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
