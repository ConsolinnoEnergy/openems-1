package io.openems.edge.remote.rest.device.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyRestDevice extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteDevice {
    private boolean isWrite;

    public DummyRestDevice(String id, boolean isWrite) {
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
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }

    @Override
    public void setValue(String value) {
        try {
            this.getWriteValueChannel().setNextWriteValue(value);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

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

    @Override
    public String getId() {
        return super.id();
    }

    @Override
    public boolean isWrite() {
        return this.isWrite;
    }

    @Override
    public boolean isRead() {
        return !this.isWrite;
    }

    @Override
    public boolean connectionOk() {
        return true;
    }
}
