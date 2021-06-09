package io.openems.edge.timer.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * The Abstract Timer. It provides basic methods that both Timer {@link TimerByTimeImpl} and {@link TimerByCycles} use.
 */
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


    /*
     * Adds a Component with it's corresponding identifier to Time Map to the Timer.
     * e.g. if a Component ControllerFoo needs 2 separate Timer with identifier Bar and a time of 10 and Bar2 and a time of 20
     * the Map will look something like Bar, 10
     * Bar2, 20
     *
     * @param id               the OpenemsComponent ID
     * @param identifierToTime the Map of the Identifier to a Time/CycleCount
     */
   /* public void addComponentToTimer(String id, Map<String, Integer> identifierToTime) {
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
    */

    /**
     * Removes the Component from the Timer.
     *
     * @param id of the Component you want to remove
     */
    @Override
    public void removeComponent(String id) {
        this.componentToIdentifierValueAndInitializedMap.remove(id);
    }

    /**
     * Resets the Timer for the Component calling this method. Multiple Timer per config are possible.
     *
     * @param id         the openemsComponent id
     * @param identifier the identifier the component uses
     */
    @Override
    public void reset(String id, String identifier) {
        ValueInitializedWrapper wrapper = this.getWrapper(id, identifier);
        if (wrapper != null) {
            wrapper.setInitialized(false);
        }
    }

    /**
     * Returns the Stored ValueInitializedWrapper determined by the component id and their identifier.
     * Usually used by inheriting Classes.
     *
     * @param id         the ComponentId.
     * @param identifier the identifier asked for.
     * @return the {@link ValueInitializedWrapper}
     */
    ValueInitializedWrapper getWrapper(String id, String identifier) {
        if (this.componentToIdentifierValueAndInitializedMap.containsKey(id)
                && this.componentToIdentifierValueAndInitializedMap.get(id).containsKey(identifier)) {
            return this.componentToIdentifierValueAndInitializedMap.get(id).get(identifier);
        }
        return null;
    }

    /**
     * Adds an Identifier to the Timer. An Identifier is a Unique Id within a Component.
     * This is important due to the fact, that a component may need multiple Timer, determining different results.
     *
     * @param id         the ComponentId the id of the component.
     * @param identifier one of the identifier the component has
     * @param maxValue   the maxValue (max CycleTime or maxTime to wait).
     */
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
