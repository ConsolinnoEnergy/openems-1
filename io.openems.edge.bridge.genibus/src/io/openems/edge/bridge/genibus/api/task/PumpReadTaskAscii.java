package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for reading ASCII data.
 */
public class PumpReadTaskAscii extends AbstractPumpTask {

    private final Channel<String> channel;

    private final Priority priority;
    private StringBuilder charStorage = new StringBuilder();

    public PumpReadTaskAscii(int address, int headerNumber, Channel<String> channel, String unitString, Priority priority) {
        super(address, headerNumber, unitString, 1);
        this.channel = channel;
        this.priority = priority;
    }

    @Override
    public void processResponse(byte data) {
        if (data != 0) {
            char dataInAscii = (char) data;
            this.charStorage.append(dataInAscii);
        } else {
            this.channel.setNextValue(this.charStorage);
            this.charStorage.delete(0, this.charStorage.length()-1);
        }
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }
}
