package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import io.openems.common.exceptions.OpenemsError;
import org.joda.time.DateTime;

public interface LineCooler {
    boolean startCooling() throws OpenemsError.OpenemsNamedException;
    boolean stopCooling(DateTime lifecycle) throws OpenemsError.OpenemsNamedException;

    DateTime getLifeCycle();

    void setLifeCycle(DateTime lifeCycle);

    void setMaxAndMin(Double max, Double min);

    void onlySetMaxMin();
}
