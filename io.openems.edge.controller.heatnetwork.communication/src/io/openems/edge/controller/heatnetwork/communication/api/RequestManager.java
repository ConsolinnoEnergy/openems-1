package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.edge.timer.api.TimerType;

/**
 * The Interface of a RequestManager.
 * Provides basic Methods any (future) RequestManager might need.
 */
public interface RequestManager {

    /**
     * Sets the Maximum Requests allowed, that can be handled at once.
     *
     * @param requestNo the new amount that can be handled (at least 0)
     */

    void setMaxManagedRequests(int requestNo);

    /**
     * Get the maximum Requests that are currently allowed.
     * Might be set before by the {@link #setMaxManagedRequests(int)} method
     *
     * @return the maximum Managed Requests at once.
     */
    int getMaxRequestsAtOnce();

    /**
     * Sets if all Requests can be managed at once.
     *
     * @param manageAllAtOnce true or false
     */
    void setManageAllAtOnce(boolean manageAllAtOnce);

    /**
     * Sets the ManageType, the Manager should manage the requests.
     *
     * @param type the {@link ManageType}
     */
    void setManageType(ManageType type);

    /**
     * Gets the current ManageType of the Manager.
     *
     * @return the {@link ManageType}
     */
    ManageType getManageType();

    /**
     * Disables the Manager and set all Callbacks to 0.
     */
    void stop();

    /**
     * Sets the maximum waitTime for the Requests in the WaitList until the Request that's in the workingQueue for the longest time
     * is swapped.
     *
     * @param maxWaitTime the maxWaitTime for the Requests in the WaitList
     */
    void setMaxWaitTime(int maxWaitTime);

    /**
     * Sets the TimerType for the Manager.
     * Note: Currently the Manager won't use the {@link io.openems.edge.timer.api.Timer} but it's on implementation, due to special cases.
     * It would be too TimeConsuming at the moment to make the Manager compatible with the Timer.
     *
     * @param type sets the {@link TimerType}
     */
    void setTimerType(TimerType type);
}
