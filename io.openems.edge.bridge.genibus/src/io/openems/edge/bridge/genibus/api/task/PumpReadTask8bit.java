package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.taskmanager.Priority;

/**
 * PumpTask class for reading values with 8 bit precision.
 */
public class PumpReadTask8bit extends PumpReadTask16bitOrMore {

    /**
     * Constructor with channel multiplier. The channel multiplier is an additional multiplication factor that is applied
     * to the data value from Genibus before it is put in the channel.
     *
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param channel the channel associated with this task.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param priority the task priority. High, low or once.
     * @param channelMultiplier the channel multiplier.
     */
    public PumpReadTask8bit(int headerNumber, int address, Channel<Double> channel, String unitTable, Priority priority, double channelMultiplier) {
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
    public PumpReadTask8bit(int headerNumber, int address, Channel<Double> channel, String unitTable, Priority priority) {
        this(headerNumber, address, channel, unitTable, priority, 1);
    }
}
