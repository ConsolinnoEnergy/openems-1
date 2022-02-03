package io.openems.edge.controller.heatnetwork.communication;


import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.rest.api.RestRemoteDevice;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.heatnetwork.communication.api.RestLeafletCommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.api.RequestManager;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequestManager;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.controller.heatnetwork.communication.request.manager.RestRequestManagerImpl;
import io.openems.edge.timer.api.TimerType;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST Communication Controller handling multiple REST requests. Contains a RequestManager that organizes them by certain ManagingPattern(FIFO) etc.
 * Maximum Request given to Manager.
 * Possible to start/stop "Communication" or Autorun mode,
 * By executing Logic ---> Handle Manager --> Sort Requests etc.
 * Available: AllRequests/Current Requests
 */
public class RestLeafletCommunicationControllerImpl implements RestLeafletCommunicationController {

    private boolean enable;

    private final Map<Integer, List<RestRequest>> allRequests = new HashMap<>();
    private RestRequestManager requestManager;
    private final ConnectionType connectionType;

    public RestLeafletCommunicationControllerImpl(ConnectionType connectionType, ManageType manageType,
                                                  int maximumAllowedRequests, boolean forceHeating) throws ConfigurationException {
        this.connectionType = connectionType;
        if (connectionType.equals(ConnectionType.REST)) {
            this.requestManager = new RestRequestManagerImpl();
        }
        if (this.requestManager == null) {
            throw new ConfigurationException(connectionType.toString(), "Somethings wrong with ConnectionType, expected REST");
        }
        this.requestManager.setManageType(manageType);
        this.requestManager.setManageAllAtOnce(forceHeating);
        this.requestManager.setMaxManagedRequests(maximumAllowedRequests);
    }

    /**
     * Enables a CommunicationController initially.
     */
    @Override
    public void enable() {
        this.enable = true;
    }

    /**
     * Manages Request by ManageType (e.g. FIFO) and max Requests at once.
     */
    private void managerLogic() {
        this.requestManager.manageRequests(this.allRequests);
    }

    /**
     * Disables the Communicationcontroller and therefore ignores Requests.
     */
    @Override
    public void disable() {
        this.enable = false;
        this.requestManager.stop();
    }

    /**
     * True if it is not stopped.
     *
     * @return a boolean.
     */
    @Override
    public boolean isRunning() {
        return this.enable;
    }

    /**
     * Executes Logic of LeafletCommunicationController => Handle Requests by Manager.
     */
    @Override
    public void executeLogic() {
        if (this.isRunning()) {
            this.managerLogic();
        }
    }

    /**
     * Checks if there is something's somethings wrong with the connection / RemoteDevice.
     *
     * @return connectionOk boolean.
     */
    @Override
    public boolean communicationAvailable() {
        AtomicReference<Boolean> connectionAvailable = new AtomicReference<>(true);

        this.allRequests.forEach((key, value) -> {
            value.forEach(request -> {
                //Connection check only necessary if connection was ok before.
                if (connectionAvailable.get()) {
                    if (request.getRequest().connectionOk() == false || request.getCallbackRequest().connectionOk() == false) {
                        connectionAvailable.set(false);
                    }
                }
            });
        });
        return connectionAvailable.get();
    }

    /**
     * The ConnectionType of the Controller.
     *
     * @return the ConnectionType.
     */

    @Override
    public ConnectionType getConnectionType() {
        return this.connectionType;
    }
    /**
     * Map that stores all Request. It's Key is a simple int, grouping the collection of {@link RestRequest}s
     * @return the Map with all grouped Requests.
     */

    @Override
    public Map<Integer, List<RestRequest>> getAllRequests() {
        return this.allRequests;
    }

    /**
     * Gets the RequestManager of the CommunicationController.
     *
     * @return the {@link RequestManager}
     */
    @Override
    public RequestManager getRequestManager() {
        return this.requestManager;
    }

    /**
     * Sets the Maximum WaitTime for the Requests within a Manager.
     * See {@link RequestManager#setMaxWaitTime(int)}
     *
     * @param maxWaitTime the Maximum WaitTime in Cycles or Minutes.
     */
    @Override
    public void setMaxWaitTime(int maxWaitTime) {
        this.requestManager.setMaxWaitTime(maxWaitTime);
    }

    /**
     * Sets the TimerType for the Manager. E.g. Cycles or Time.
     * Note: Because it's hard and Time consuming to work with the later added {@link io.openems.edge.timer.api.TimerHandler}
     * The {@link RequestManager} has it's own TimeHandling.
     *
     * @param type the TimerType.
     */

    @Override
    public void setTimerTypeForManaging(TimerType type) {
        this.requestManager.setTimerType(type);
    }

    /**
     * Enables all Requests -> may happen on Exceptional State.
     */
    @Override
    public void enableAllRequests() {
        this.getAllRequests().forEach((key, value) -> {
            value.forEach(entry -> entry.getCallbackRequest().setValue("1"));
        });
    }

    /**
     * Disables all Requests -> may happen on Exceptional State.
     */
    @Override
    public void disableAllRequests() {
        this.getAllRequests().forEach((key, value) -> {
            value.forEach(entry -> entry.getCallbackRequest().setValue("0"));
        });
    }

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

    @Override
    public int enableManagedRequestsAndReturnSizeOfManagedRequests(boolean forcing, Map<RequestType, AtomicBoolean> cleanRequestTypeMap) {
        Map<Integer, List<RestRequest>> currentRestRequests =
                this.getRestManager().getManagedRequests();
        if (currentRestRequests.size() > 0) {
            currentRestRequests.forEach((key, value) -> {
                value.forEach(restRequest -> {
                    if (restRequest.getRequest().getValue().equals("1") || forcing) {
                        restRequest.getCallbackRequest().setValue("1");
                        cleanRequestTypeMap.get(restRequest.getRequestType()).set(true);
                    }
                });
            });
        }
        return currentRestRequests.size();
    }

    /**
     * Sets the Maximum Requests allowed, that can be handled at once.
     * @param maxAllowedRequests the new amount that can be handled
     */

    @Override
    public void setMaxRequests(int maxAllowedRequests) {
        this.requestManager.setMaxManagedRequests(maxAllowedRequests);
    }

    /**
     * This will check each Containing Component -> is the reference still ok? otherwise tell the Comm.Master
     *
     * @param cpm the ComponentManager
     * @return true if a Component Reference is old / not the same
     */

    @Override
    public boolean shouldRefreshReferences(ComponentManager cpm) {
        AtomicBoolean oldReferencesFound = new AtomicBoolean(false);
        this.allRequests.forEach((integer, listOfRestRequests) -> {
            if (!oldReferencesFound.get()) {
                listOfRestRequests.forEach(entry -> {
                    if (!oldReferencesFound.get()) {
                        try {
                            OpenemsComponent component = cpm.getComponent(entry.getRequest().id());
                            if (component instanceof RestRemoteDevice && !component.equals(entry.getRequest())) {
                                oldReferencesFound.set(true);
                            }
                            if (!oldReferencesFound.get()) {
                                component = cpm.getComponent(entry.getCallbackRequest().id());
                                if (component instanceof RestRemoteDevice && !component.equals(entry.getCallbackRequest())) {
                                    oldReferencesFound.set(true);
                                }
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            oldReferencesFound.set(true);
                        }
                    }
                });
            }
        });
        return oldReferencesFound.get();
    }

    /**
     * Gets the {@link RestRequestManager} of this CommunicationController.
     * @return the {@link RestRequestManager}.
     */

    @Override
    public RestRequestManager getRestManager() {
        return this.requestManager;
    }

    /**
     * Adds RestRequests to allRequests. This is important for the Manager handling the Requests.
     *
     * @param additionalRequests the additional Requests.
     */
    @Override
    public void addRestRequests(Map<Integer, List<RestRequest>> additionalRequests) {
        additionalRequests.keySet().forEach(key -> {
            if (this.allRequests.containsKey(key)) {
                additionalRequests.get(key).forEach(request -> {
                    if (!this.allRequests.get(key).contains(request)) {
                        this.allRequests.get(key).add(request);
                    }
                });
            } else {
                this.allRequests.put(key, additionalRequests.get(key));
            }
        });
    }
}
