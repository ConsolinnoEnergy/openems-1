package io.openems.edge.timer.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTimer extends AbstractOpenemsComponent implements Timer {


    public AbstractTimer(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                         io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    /**
     * This Map connects the ComponentId with it's identifier and a WrapperClass containing the maxCycle/Time
     * value as an Int.
     * and the Boolean if the identifier was initialized or not.
     * On reset set the boolean to false.
     */
    Map<String, Map<String, ValueInitializedWrapper>> componentToIdentifierValueAndInitializedMap = new HashMap<>();


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
