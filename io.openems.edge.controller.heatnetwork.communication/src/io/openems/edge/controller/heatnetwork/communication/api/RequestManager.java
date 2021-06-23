package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.edge.timer.api.TimerType;

public interface RequestManager {

    void setMaxManagedRequests(int requestNo);

    int getMaxRequestsAtOnce();

    void setManageAllAtOnce(boolean manageAllAtOnce);

    void setManageType(ManageType type);

    ManageType getManageType();

    void stop();

    void setMaxWaitTime(int maxWaitTime);

    void setTimerType(TimerType type);
}
