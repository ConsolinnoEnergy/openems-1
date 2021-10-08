package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.timer.api.TimerType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An Interface for all CommunicationController. E.g. Communicating with other Leaflets via REST.
 */
public interface CommunicationController {
    /**
     * Enables a CommunicationController initially.
     */
    void enable();

    /**
     * Disables the CommunicationController and therefore ignores Requests.
     */
    void disable();

    /**
     * True if it is not stopped.
     *
     * @return a boolean.
     */
    boolean isRunning();

    /**
     * Executes the Specific controller Logic.
     */
    void executeLogic();

    /**
     * Connection ok.?
     *
     * @return connectionState.
     */

    boolean communicationAvailable();

    /**
     * The ConnectionType of the Controller.
     *
     * @return the ConnectionType.
     */
    ConnectionType getConnectionType();

    /**
     * Gets the RequestManager of the CommunicationController.
     *
     * @return the {@link RequestManager}
     */
    RequestManager getRequestManager();

    /**
     * Sets the Maximum WaitTime for the Requests within a Manager.
     * See {@link RequestManager#setMaxWaitTime(int)}
     *
     * @param maxWaitTime the Maximum WaitTime in Cycles or Minutes.
     */
    void setMaxWaitTime(int maxWaitTime);

    /**
     * Sets the TimerType for the Manager. E.g. Cycles or Time.
     * Note: Bc it's hard and TimeConsuming to work with the later added {@link io.openems.edge.timer.api.TimerHandler}
     * The {@link RequestManager} has it's own TimeHandling.
     *
     * @param type the TimerType.
     */
    void setTimerTypeForManaging(TimerType type);

    /**
     * Enables all Requests -> may happen on Exceptional State.
     */
    void enableAllRequests();

    /**
     * Disables all Requests -> may happen on Exceptional State.
     */
    void disableAllRequests();

    /**
     * Called by CommunicationMaster, managed Requests will be handled/enabled.
     * The CommunicationController stores the true/false value within the map, instantiated by the CommunicationMaster.
     * The CommunicationMaster will handle them later and reacts to them, like configured.
     *
     * @param forcing             if this is true -> all responses and callbacks will be set to true.
     * @param cleanRequestTypeMap the Map created by the CommunicationMaster.
     *                            Important to check if the Responses of the Controller should be activated or deactivated
     * @return the size of the managedRequests.
     */
    int enableManagedRequestsAndReturnSizeOfManagedRequests(boolean forcing, Map<RequestType, AtomicBoolean> cleanRequestTypeMap);

    /**
     * Sets the Maximum Requests allowed, that can be handled at once.
     * @param maxAllowedRequests the new amount that can be handled
     */
    void setMaxRequests(int maxAllowedRequests);
}
