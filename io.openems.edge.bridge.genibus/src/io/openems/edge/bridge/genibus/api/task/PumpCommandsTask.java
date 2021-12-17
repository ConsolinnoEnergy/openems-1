package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for command tasks.
 */
public class PumpCommandsTask extends AbstractPumpTask implements GenibusWriteTask {

    private final WriteChannel<Boolean> channel;

    public PumpCommandsTask(int address, int headerNumber, WriteChannel<Boolean> channel) {
        super(address, headerNumber, "", 1);
        this.channel = channel;
    }

    @Override
    public boolean isSetAvailable() {
        if (this.channel.getNextWriteValue().isPresent()) {
            return this.channel.getNextWriteValue().get();
        } else {
            return false;
        }
    }

    @Override
    public void clearNextWriteAndUpdateChannel() {
        // If the write task is added to a telegram, reset channel write to null to send write just once.
        this.channel.getNextWriteValueAndReset();
    }

    @Override
    public void processResponse(byte data) {
        // This method is for processing the response to GET or INFO. Commands do neither of those, so do nothing.
    }

    @Override
    public Priority getPriority() {
        // Commands task is always high priority.
        return Priority.HIGH;
    }
}
