package io.openems.edge.timer.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.HashMap;
import java.util.Map;

public class DummyTimer extends AbstractOpenemsComponent implements OpenemsComponent, Timer {

    private Map<String, ValueInitializedWrapper> identifierToValueWrapper;
    private TimerType type;

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
       switch (this.type){
           case CYCLES:
               ValueInitializedWrapper wrapper = this.getWrapper(id, identifier);
               if (wrapper.isInitialized()) {
                   return  wrapper.getCounter().getAndIncrement() > wrapper.getMaxValue();
               } else {
                   wrapper.setInitialized(true);
                   wrapper.getCounter().set(0);
               }
               return false;
       }
               break;
           case TIME:
               break;
       }




    @Override
    public void addComponentToTimer(String id, Map<String, Integer> identifierToTime) {
        Map<String, ValueInitializedWrapper> identifierToWrapper = new HashMap<>();
        identifierToTime.forEach((key, value) -> {
            identifierToWrapper.put(key, new ValueInitializedWrapper(value));
        });

        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)) {
            this.componentToIdentifierValueAndInitializedMap.get(id).forEach((key, value) -> {
                identifierToWrapper.remove(key);
            });
            if (identifierToWrapper.isEmpty() == false) {
                this.componentToIdentifierValueAndInitializedMap.get(id).putAll(identifierToWrapper);
            }
        } else {
            this.componentToIdentifierValueAndInitializedMap.put(id, identifierToWrapper);
        }
    }

    @Override
    public void removeComponent(String id) {
        this.componentToIdentifierValueAndInitializedMap.remove(id);
    }

    @Override
    public void reset(String id, String identifier) {
        ValueInitializedWrapper wrapper = this.getWrapper(id, identifier);
        if (wrapper != null) {
            wrapper.setInitialized(false);
        }
    }

    ValueInitializedWrapper getWrapper(String id, String identifier) {
        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)
                && this.componentToIdentifierValueAndInitializedMap.get(id).containsKey(identifier)) {
            return this.componentToIdentifierValueAndInitializedMap.get(id).get(identifier);
        }
        return null;
    }

    @Override
    public void addIdentifierToTimer(String id, String identifier, int maxValue) {
        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)) {
            if (this.componentToIdentifierValueAndInitializedMap.get(id).containsKey(identifier)) {
                this.getWrapper(id, identifier).setMaxValue(maxValue);
            } else {
                this.componentToIdentifierValueAndInitializedMap.get(id).put(identifier, new ValueInitializedWrapper(maxValue));
            }
        } else {
            Map<String, ValueInitializedWrapper> identifierToValueMap = new HashMap<>();
            identifierToValueMap.put(identifier, new ValueInitializedWrapper(maxValue));
            this.componentToIdentifierValueAndInitializedMap.put(id, identifierToValueMap);
        }
    }
}
