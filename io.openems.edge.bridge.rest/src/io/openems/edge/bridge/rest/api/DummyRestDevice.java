package io.openems.edge.bridge.rest.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A Dummy Rest Device for Test purposes.
 */
public class DummyRestDevice extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteDevice {
    private final boolean isWrite;
    private final boolean disturbanceActive;

    public DummyRestDevice(String id, boolean isWrite, boolean disturbanceActive) {
        super(
                OpenemsComponent.ChannelId.values(),
                RestRemoteDevice.ChannelId.values()

        );
        if (isWrite) {
            this.getTypeSetChannel().setNextValue("Write");
        } else {
            this.getTypeSetChannel().setNextValue("Read");
        }
        this.isWrite = isWrite;
        this.disturbanceActive = disturbanceActive;
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }

    public DummyRestDevice(String id, boolean isWrite) {
        this(id, isWrite, false);
    }

    public DummyRestDevice(String id) {
        this(id, false);
    }

    /**
     * Sets the Value of the Device if it's type is Write /allows to write.
     *
     * @param value the Value that the Remote Device will be set to and therefore the Remote Device too.
     */
    @Override
    public void setValue(String value) {
        try {
            this.getWriteValueChannel().setNextWriteValue(value);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the Value as a String. Depending on the Write/Read Type.
     *
     * @return the ValueString.
     */

    @Override
    public String getValue() {
        if (this.getTypeSetChannel().value().get().equals("Write")) {
            if (this.getWriteValueChannel().value().isDefined()) {
                return this.getWriteValueChannel().value().get();
            } else {
                return "Write Value not available yet for " + super.id();
            }
        } else if (this.getReadValueChannel().value().isDefined()) {
            return this.getReadValueChannel().value().get();
        }
        return "Read Value not available yet";
    }

    /**
     * Get the Unique Id.
     *
     * @return the Id.
     */
    @Override
    public String getId() {
        return super.id();
    }

    /**
     * Check if this Device is a Write Remote Device.
     *
     * @return a boolean.
     */
    @Override
    public boolean isWrite() {
        return this.isWrite;
    }

    /**
     * Checks if this Device is a Read Remote Device.
     *
     * @return a boolean.
     */
    @Override
    public boolean isRead() {
        return !this.isWrite;
    }

    /**
     * Checks/Asks if the Connection via Rest is ok.
     *
     * @return a boolean.
     */
    @Override
    public boolean connectionOk() {
        return !this.disturbanceActive;
    }
}
