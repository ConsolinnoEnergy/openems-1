package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for writing with 8bit precision.
 */
public class PumpWriteTask8bit extends PumpWriteTask16bitOrMore {

    /**
     * Constructor with channel multiplier. The channel multiplier is an additional multiplication factor that is applied
     * to the data value from Genibus before it is put in the channel. For writes to the Genibus device, it is a divisor.
     *
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     * @param channelMultiplier the channel multiplier.
     */
    public PumpWriteTask8bit(int headerNumber, int address, WriteChannel<Double> channel, String unitTable, Priority priority, double channelMultiplier) {
        super(1, headerNumber, address, channel, unitTable, priority, channelMultiplier);
    }

    /**
     * Constructor without channel multiplier.
     *
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     */
    public PumpWriteTask8bit(int headerNumber, int address, WriteChannel<Double> channel, String unitTable, Priority priority) {
        this(headerNumber, address, channel, unitTable, priority, 1);
    }

}
