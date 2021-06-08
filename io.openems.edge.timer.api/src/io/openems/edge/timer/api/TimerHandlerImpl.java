package io.openems.edge.timer.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

public class TimerHandlerImpl implements TimerHandler {
    private final Map<String, Timer> identifierToTimerMap = new HashMap<>();
    private final ComponentManager cpm;
    private final String id;

    public TimerHandlerImpl(String id, ComponentManager cpm, Map<String, String> identifierToTimerIdStringMap, Map<String, Integer> identifierToMaxTime) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.id = id;
        this.cpm = cpm;
        OpenemsError.OpenemsNamedException[] ex = {null};
        ConfigurationException[] exConfig = {null};
        identifierToTimerIdStringMap.forEach((identifier, timer) -> {
            if (ex[0] == null && exConfig[0] == null) {
                try {
                    this.identifierToTimerMap.put(identifier, this.getValidTimer(timer));
                } catch (OpenemsError.OpenemsNamedException e) {
                    ex[0] = e;
                } catch (ConfigurationException e) {
                    exConfig[0] = e;
                }
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        if (exConfig[0] != null) {
            throw exConfig[0];
        }
        this.identifierToTimerMap.forEach((identifier, timer) -> {
            timer.addIdentifierToTimer(this.id, id, identifierToMaxTime.get(identifier));
        });


    }

    public TimerHandlerImpl(String id, ComponentManager cpm) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this(id, cpm, new HashMap<>(), new HashMap<>());
    }

    @Override
    public void addOneIdentifier(String identifier, String timer, int maxTime) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        Timer timerToAdd = this.getValidTimer(timer);
        this.identifierToTimerMap.put(identifier, timerToAdd);
        timerToAdd.addIdentifierToTimer(this.id, identifier, maxTime);
    }

    private Timer getValidTimer(String timer) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent component = this.cpm.getComponent(timer);
        if (component instanceof Timer) {
            return ((Timer) component);
        } else {
            throw new ConfigurationException("GetValidTimer: " + this.id, "TimerID not an instance of Timer " + timer);
        }
    }

    @Override
    public void resetTimer(String identifier) {
        this.identifierToTimerMap.get(identifier).reset(this.id, identifier);
    }

    @Override
    public void removeComponent() {
        this.identifierToTimerMap.forEach((identifier, timer) -> {
            timer.removeComponent(this.id);
        });
    }

    @Override
    public boolean checkTimeIsUp(String identifier) {
        return this.identifierToTimerMap.get(identifier).checkIsTimeUp(this.id, identifier);
    }

    @Override
    public void resetAllTimer() {
        this.identifierToTimerMap.forEach((identifier, timer) -> {
            timer.reset(this.id, identifier);
        });
    }
}
