package io.openems.edge.timer.api;

import io.openems.common.exceptions.OpenemsError;
import org.osgi.service.cm.ConfigurationException;

public interface TimerHandler {

    void addOneIdentifier(String identifier, String timer, int maxTime) throws OpenemsError.OpenemsNamedException, ConfigurationException;
    void resetTimer(String identifier);
    void removeComponent();
    boolean checkTimeIsUp(String identifier);

    void resetAllTimer();
}
