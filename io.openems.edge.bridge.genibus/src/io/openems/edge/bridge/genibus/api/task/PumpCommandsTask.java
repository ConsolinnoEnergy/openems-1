package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for command tasks. Command tasks are head class 3 and can only do SET.
 * Command task work like a button that you push or don't push. Sending the command to the device equals pushing the
 * button, while not sending the command equals not pushing the button. Cancelling a command is not done by not sending
 * the command, but by sending a different command. For example, ’start (3, 6)’ is cancelled by ’stop (3, 5)’.
 * Command task are associated with a boolean channel, where ’true’ means send the command and ’false’ or ’null’ means
 * don't send the command.
 */
public class PumpCommandsTask extends AbstractPumpTask implements GenibusWriteTask {

    private final WriteChannel<Boolean> channel;

    /**
     * Constructor. Command tasks are head class 3 and can only do SET. Examples are ’start (3, 6)’ and ’stop (3, 5)’.
     * Command task are associated with a boolean channel, where ’true’ means send the command and ’false’ or ’null’
     * means don't send the command.
     * There is no parameter for the headerNumber, since a command task is always head class 3.
     *
     * @param address the Genibus data item address.
     * @param channel the boolean write channel associated with this task.
     */
    public PumpCommandsTask(int address, WriteChannel<Boolean> channel) {
        super(3, address, "", 1);
        this.channel = channel;
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
        if (this.channel.getNextWriteValue().isPresent()) {
            return this.channel.getNextWriteValue().get();
        } else {
            return false;
        }
    }

    /**
     * This method clears the ’nextWrite’ of the channel associated with this task. This will make ’isSetAvailable()’
     * return ’false’, until a new value has been written into ’nextWrite’ of the channel.
     * Should be called after this task was added as SET to an APDU, so that the SET is executed just once.
     * Also, if applicable, marks this task as ’get value from Genibus’, so the channel is updated to the new value.
     */
    @Override
    public void clearNextWriteAndUpdateChannel() {
        this.channel.getNextWriteValueAndReset();
    }

    /**
     * Allocate a byte from a response telegram to this task. There is no response to a command task, since command
     * tasks only do SET (a response APDU exists, but it is empty). This method is not used by command tasks.
     *
     * @param data the response byte from the Genibus device for this task.
     */
    @Override
    public void processResponse(byte data) {
        // This method is for processing the response to GET or INFO. Commands do neither of those, so do nothing.
    }

    /**
     * Get the priority of this task. Command tasks are always high priority.
     *
     * @return the priority.
     */
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}
