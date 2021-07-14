package io.openems.edge.timer.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class DummyTimer extends AbstractOpenemsComponent implements OpenemsComponent, Timer {

    private final Map<String, ValueInitializedWrapper> identifierToValueWrapper = new HashMap<>();
    private final TimerType type;

    public DummyTimer(String id, TimerType type) {
        super(
                OpenemsComponent.ChannelId.values()
        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
        this.type = type;
    }


    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        ValueInitializedWrapper wrapper;
        switch (this.type) {

            case CYCLES:
               wrapper = this.getWrapper(identifier);
                if (wrapper.isInitialized()) {
                    return wrapper.getCounter().getAndIncrement() >= wrapper.getMaxValue();
                } else {
                    wrapper.setInitialized(true);
                    wrapper.getCounter().set(1);
                }
                return false;
            case TIME:
                wrapper = this.getWrapper(identifier);
                if (wrapper.isInitialized()) {
                    return DateTime.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
                } else {
                    wrapper.setInitialized(true);
                    wrapper.getInitialDateTime().set(new DateTime());
                }
                return false;
        }
        return true;
    }


    public void addComponentToTimer(String id, Map<String, Integer> identifierToTime) {
        identifierToTime.forEach((key, value) -> {
            this.identifierToValueWrapper.put(key, new ValueInitializedWrapper(value));
        });
    }

    @Override
    public void removeComponent(String id) {
        //Not needed here
    }

    @Override
    public void reset(String id, String identifier) {
        ValueInitializedWrapper wrapper = this.getWrapper(identifier);
        if (wrapper != null) {
            wrapper.setInitialized(false);
        }
    }

    ValueInitializedWrapper getWrapper(String identifier) {
            return this.identifierToValueWrapper.get(identifier);
    }

    @Override
    public void addIdentifierToTimer(String id, String identifier, int maxValue) {
            this.identifierToValueWrapper.put(identifier, new ValueInitializedWrapper(maxValue));
        }
}
