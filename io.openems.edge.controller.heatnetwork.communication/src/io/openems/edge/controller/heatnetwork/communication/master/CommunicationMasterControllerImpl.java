package io.openems.edge.controller.heatnetwork.communication.master;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.communication.RestLeafletCommunicationControllerImpl;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationMasterController;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.api.Request;
import io.openems.edge.controller.heatnetwork.communication.api.RequestType;
import io.openems.edge.controller.heatnetwork.communication.api.RestLeafletCommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.request.api.ConfigPosition;
import io.openems.edge.controller.heatnetwork.communication.request.api.MasterResponseType;
import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;
import io.openems.edge.controller.heatnetwork.communication.request.rest.RestRequestImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ChannelAddressResponse;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ChannelAddressResponseImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.MethodResponse;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.MethodResponseImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ResponseWrapper;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.joda.time.DateTime;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.CommunicationMaster",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CommunicationMasterControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller,
        CommunicationMasterController, ExceptionalState {

    private final Logger logger = LoggerFactory.getLogger(CommunicationMasterControllerImpl.class);
    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;
    //Configured communicationController handling one remoteCommunication
    CommunicationController communicationController;
    //ThresholdTemperature to Write into so another Component can react in response correctly
    private ChannelAddress answer;
    //The Optional HydraulicLineHeater
    private HydraulicLineHeater hydraulicLineHeater;
    //The Optional HeatPump
    private Pump heatPump;
    //Will be Set if Connection not ok
    private DateTime initalTimeStampFallback;
    //For Subclasses -> CommunicationController and manager
    private boolean forcing;
    private static final int REMOTE_REQUEST_CONFIGURATION_SIZE = 4;
    private int maxAllowedRequests;
    private int maxWaitTime;
    //Current request size --> IF Size is empty -> deactivate extra component else activate them.
    //Is declared as an Integer bc. of future implementation : Do XYZ at Certain Size
    //resets every run
    private final AtomicInteger requestSizeThisRun = new AtomicInteger(0);
    private TimerHandler timer;
    private boolean useExceptionalStateHandling;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String KEEP_ALIVE_IDENTIFIER = "COMMUNICATION_MASTER_CONTROLLER_KEEP_ALIVE_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "COMMUNICATION_MASTER_CONTROLLER_EXCEPTIONAL_STATE_IDENTIFIER";

    private final Map<RequestType, List<ResponseWrapper>> requestTypeAndResponses = new HashMap<>();
    private final Map<RequestType, AtomicBoolean> requestTypeIsSet = new HashMap<>();

    private boolean configurationDone;


    public CommunicationMasterControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                CommunicationMasterController.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws Exception {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.requestTypes().length != RequestType.values().length) {
            this.updateConfig();
            return;
        }
        //ForceHeating == Heat ALL remote Heater/handle ALL requests/Enable all requests even if they don't have a request
        this.configurationDone = config.configurationDone();
        if (this.configurationDone) {
            this.setForceHeating(config.forceHeating());
            this.setAutoRun(true);
            this.maxWaitTime = config.maxWaitTimeAllowed();
            this.forcing = this.getForceHeating();
            //Creates the Controller responsible for handling RemoteRequests (REST requests)
            this.createCommunicationController(config);
            this.createRemoteRequestsAndAddToCommunicationController(config);
            OpenemsComponent optionalComponent;

            if (config.usePump()) {
                optionalComponent = this.cpm.getComponent(config.pumpId());
                if (optionalComponent instanceof Pump) {
                    this.heatPump = (Pump) optionalComponent;
                } else {
                    throw new ConfigurationException("CommunicationMaster - Activate - Pump", "PumpId Component - Not an instance of Pump; PumpId: " + config.pumpId());
                }
            }
            if (config.useHydraulicLineHeater()) {
                optionalComponent = this.cpm.getComponent(config.hydraulicLineHeaterId());
                if (optionalComponent instanceof HydraulicLineHeater) {
                    this.hydraulicLineHeater = (HydraulicLineHeater) optionalComponent;
                } else {
                    throw new ConfigurationException("CommunicationMaster - Activate - HydraulicLineHeater",
                            "HydraulicLineHeaterId Component - Not an Instance of HydraulicLineHeater : " + config.hydraulicLineHeaterId());
                }
            }
            this.setResponsesToRequests(Arrays.asList(config.requestTypeToResponse()));
            Arrays.asList(RequestType.values()).forEach(requestType -> {
                this.requestTypeIsSet.put(requestType, new AtomicBoolean(false));
            });

            this.setForceHeating(config.forceHeating());
            this.setAutoRun(true);
            this.timer = new TimerHandlerImpl(this.id(), this.cpm);
            this.addTimer(config.timerId(), config.keepAlive(), KEEP_ALIVE_IDENTIFIER);
            this.useExceptionalStateHandling = config.useExceptionalStateHandling();
            if (this.useExceptionalStateHandling) {
                this.addTimer(config.timerIdExceptionalState(), config.exceptionalStateTime(), EXCEPTIONAL_STATE_IDENTIFIER);
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);
                this.getExceptionalStateValueChannel().setNextValue(100);
            }
            this.setKeepAlive(config.keepAlive());
            FallbackHandling fallback = config.fallback();

            this.setFallbackLogic(fallback);
            this.maxAllowedRequests = config.maxRequestAllowedAtOnce();
            this.getMaximumRequestChannel().setNextValue(this.maxAllowedRequests);
            this.communicationController.setTimerTypeForManaging(config.timerForManager());
        }
    }

    private void setResponsesToRequests(List<String> requestTypeToResponse) throws ConfigurationException {
        ConfigurationException[] ex = {null};
        requestTypeToResponse.forEach(entry -> {
            if (ex[0] == null) {
                String[] entries = entry.split(":");
                RequestType requestType = null;
                MasterResponseType masterResponseType = null;
                if (RequestType.contains(entries[ConfigPosition.REQUEST_TYPE_POSITION.getValue()])) {
                    requestType = RequestType.valueOf(entries[ConfigPosition.REQUEST_TYPE_POSITION.getValue()]);
                } else {
                    ex[0] = new ConfigurationException("CommunicationMasterController: setResponseToRequest",
                            "RequestType not supported: " + entries[ConfigPosition.REQUEST_TYPE_POSITION.getValue()]);
                }
                if (MasterResponseType.contains(entries[ConfigPosition.MASTER_RESPONSE_TYPE_POSITION.getValue()])) {
                    masterResponseType = MasterResponseType.valueOf(entries[ConfigPosition.MASTER_RESPONSE_TYPE_POSITION.getValue()]);
                } else {
                    ex[0] = new ConfigurationException("CommunicationMasterController: setResponseToRequest",
                            "MasterResponseType not supported: " + entries[ConfigPosition.MASTER_RESPONSE_TYPE_POSITION.getValue()]);
                }
                if (requestType != null && masterResponseType != null) {
                    ResponseWrapper wrapper = null;
                    switch (masterResponseType) {
                        case METHOD:
                            wrapper = new MethodResponseImpl(
                                    entries[ConfigPosition.METHOD_OR_CHANNEL_ADDRESS_POSITION.getValue()],
                                    entries[ConfigPosition.REQUEST_ACTIVE_VALUE_POSITION.getValue()],
                                    entries[ConfigPosition.REQUEST_NOT_ACTIVE_VALUE_POSITION.getValue()]);

                            break;
                        case CHANNEL_ADDRESS:
                            try {
                                wrapper = new ChannelAddressResponseImpl(entries[ConfigPosition.METHOD_OR_CHANNEL_ADDRESS_POSITION.getValue()],
                                        entries[ConfigPosition.REQUEST_ACTIVE_VALUE_POSITION.getValue()],
                                        entries[ConfigPosition.REQUEST_NOT_ACTIVE_VALUE_POSITION.getValue()], this.cpm);
                            } catch (OpenemsError.OpenemsNamedException e) {
                                ex[0] = new ConfigurationException("CommunicationMasterController: setResponseToRequest",
                                        "Wrong ChannelAddress: " + entries[ConfigPosition.METHOD_OR_CHANNEL_ADDRESS_POSITION.getValue()]);
                            }
                            break;
                    }
                    if (this.requestTypeAndResponses.containsKey(requestType)) {
                        this.requestTypeAndResponses.get(requestType).add(wrapper);
                    } else {
                        List<ResponseWrapper> wrapperList = new ArrayList<>();
                        wrapperList.add(wrapper);
                        this.requestTypeAndResponses.put(requestType, wrapperList);
                    }
                }
            }

        });

        if (ex[0] != null) {
            throw ex[0];
        }
    }

    private void updateConfig() {
        Configuration c;

        try {
            c = this.cm.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String types = Arrays.toString(RequestType.values());
            properties.put("requestTypes", this.propertyInput(types));

            types = Arrays.toString(MethodTypes.values());
            properties.put("methodTypes", this.propertyInput(types));
            c.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Due to the fact that the inputs are arrays this needs to be done...it's weird but it's working.
     *
     * @param types either mqttTypes or Priorities
     * @return the String[] for update in OSGi
     */
    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        types = types.replaceAll(" ", "");
        return types.split(",");
    }

    private void addTimer(String timerId, int keepAlive, String identifier) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer.addOneIdentifier(identifier, timerId, keepAlive);
    }

    /**
     * Creates the RemoteRequests from Config. --> e.g. RestRemoteComponents will be handled bei a RestCommunicationController
     * get the RequestConfig, and split them.
     * Map the Configured Requests to the corresponding requestMaps.
     * add them to the CommunicationController if they are correct. Else throw Exception.
     *
     * @param config config of the Component
     * @throws ConfigurationException             if there's an error within config
     * @throws OpenemsError.OpenemsNamedException if cpm couldn't find OpenemsComponent
     */
    private void createRemoteRequestsAndAddToCommunicationController(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        //ErrorHandling in Streams..you can't simply throw exceptions in Streams in Java 8
        ConfigurationException[] ex = {null};
        OpenemsError.OpenemsNamedException[] exceptions = {null};
        List<String> requestConfig = Arrays.asList(config.requestMap());
        //Collection of all Requests, can be any  (sub) Type of request, important for future impl. -> Different RequestTypes
        Map<Integer, List<? super Request>> requestMap = new HashMap<>();

        requestConfig.forEach(entry -> {
            try {
                if (ex[0] == null) {
                    this.createRequestFromConfigEntry(entry, requestMap, this.communicationController.getConnectionType());
                }
            } catch (ConfigurationException e) {
                ex[0] = e;
            } catch (OpenemsError.OpenemsNamedException e) {
                exceptions[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
        //Add RestRequests to RestCommunicationController
        if (this.communicationController.getConnectionType().equals(ConnectionType.REST)) {
            Map<Integer, List<RestRequest>> createdRestRequests = new HashMap<>();
            requestMap.forEach((key, value) -> {
                List<RestRequest> requestsOfKey = new ArrayList<>();
                value.forEach(entry -> requestsOfKey.add((RestRequest) entry));
                createdRestRequests.put(key, requestsOfKey);
            });
            ((RestLeafletCommunicationController) this.communicationController).addRestRequests(createdRestRequests);
        } else {
            throw new ConfigurationException(this.communicationController.getConnectionType().toString(), "Is not supported by this controller");
        }

    }

    /**
     * Creates a Requests from a configuration and puts it into the requestMap.
     * First Split the entires by ":", then check each split param if correct, if params are ok, create corresponding RestRequest.
     *
     * @param entry          the entry usually from Config or Channel.
     * @param requestMap     the requestMap coming from calling method (if config done it will be added to the controller)
     * @param connectionType the connection Type that is configured.
     * @throws ConfigurationException             if Somethings wrong with the configuration.
     * @throws OpenemsError.OpenemsNamedException if the cpm couldn't find the component.
     */
    private void createRequestFromConfigEntry(String entry, Map<Integer, List<? super Request>> requestMap, ConnectionType connectionType)
            throws ConfigurationException, OpenemsError.OpenemsNamedException {
        String[] entries = entry.split(":");
        if (entries.length != REMOTE_REQUEST_CONFIGURATION_SIZE) {
            throw new ConfigurationException("" + entries.length, "Length not ok expected " + REMOTE_REQUEST_CONFIGURATION_SIZE);
        }
        AtomicInteger configurationCounter = new AtomicInteger(0);
        //REQUEST (Pos 0), CALLBACK (Pos 1), KEY (Pos 2)
        OpenemsComponent request = this.cpm.getComponent(entries[configurationCounter.getAndIncrement()]);
        OpenemsComponent callback = this.cpm.getComponent(entries[configurationCounter.getAndIncrement()]);
        int keyForMap = Integer.parseInt(entries[configurationCounter.getAndIncrement()]);
        String requestTypeString = entries[configurationCounter.getAndIncrement()].toUpperCase().trim();
        RequestType type;
        if (RequestType.contains(requestTypeString)) {
            type = RequestType.valueOf(requestTypeString);
        } else {
            throw new ConfigurationException(requestTypeString, "Wrong request Type, allowed Request types are: " + Arrays.toString(RequestType.values()));
        }
        if (connectionType == ConnectionType.REST) {
            if (request instanceof RestRemoteDevice && callback instanceof RestRemoteDevice) {
                RestRequest requestToAdd = new RestRequestImpl((RestRemoteDevice) request, (RestRemoteDevice) callback, type);
                if (requestMap.containsKey(keyForMap)) {
                    requestMap.get(keyForMap).add(requestToAdd);
                } else {
                    List<Request> requestListForMap = new ArrayList<>();
                    requestListForMap.add(requestToAdd);
                    requestMap.put(keyForMap, requestListForMap);
                }
            } else {
                throw new ConfigurationException("ConfigurationError",
                        "Request and Callback have to be from the same type");
            }
        }

    }


    /**
     * Creates The CommunicationController by Config.
     * First get the Connection Type and Manage type as well as the maximumRequest
     * (how many Requests are allowed at once-->Integer count NOT Listentry; Integer represents a Heatstorage/Heater etc)
     * (Map<Integer, List< ? super Request>).
     *
     * @param config config of this component
     * @throws Exception thrown if somethings wrong with
     */
    private void createCommunicationController(Config config) throws Exception {
        String connectionTypeString = config.connectionType().trim().toUpperCase();
        String manageTypeString = config.manageType().toUpperCase().trim();
        int maxRequestsAllowedAtOnce = config.maxRequestAllowedAtOnce();
        if (ConnectionType.contains(connectionTypeString) && ManageType.contains(manageTypeString)) {
            ConnectionType connectionType = ConnectionType.valueOf(connectionTypeString);
            ManageType manageType = ManageType.valueOf(manageTypeString);
            switch (connectionType) {
                case REST:
                default:
                    this.communicationController = new RestLeafletCommunicationControllerImpl(connectionType,
                            manageType, maxRequestsAllowedAtOnce,
                            this.forcing, true);
                    this.communicationController.setMaxWaitTime(this.maxWaitTime);
            }
        }

    }

    @Deactivate
    protected void deactivate() {
        this.deactivateAllComponents();
        this.communicationController.setAutoRun(false);
        this.communicationController.stop();
        this.timer.removeComponent();
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.configurationDone) {
            //Check if Requests have to be added/removed and if components are still enabled
            this.checkChangesAndApply();
            //Connections ok?
            AtomicBoolean connectionOk = new AtomicBoolean(true);

            connectionOk.getAndSet(this.communicationController.communicationAvailable());
            if (this.useExceptionalStateHandling) {
                boolean useExceptionalState = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (useExceptionalState) {
                    int exceptionalStateValue = this.getExceptionalStateValue();
                    if (exceptionalStateValue > 0) {
                        this.communicationController.enableAllRequests();
                        this.activateAllComponents();
                    } else {
                        this.communicationController.disableAllRequests();
                        this.deactivateAllComponents();
                    }
                    return;
                }
            }
            if (connectionOk.get()) {
                this.timer.resetTimer(KEEP_ALIVE_IDENTIFIER);
                this.checkRequestSizeAndEnableCallbackActivateComponents();
            } else if (this.timer.checkTimeIsUp(KEEP_ALIVE_IDENTIFIER)) {
                this.fallbackLogic();
            }
            this.requestSizeThisRun.set(0);
        }
    }

    /**
     * Checks if hydraulicHeater and Pump are still enabled (if Present).
     */
    private void checkChangesAndApply() {
        //Check if Components are still enabled
        try {
            if (this.hydraulicLineHeater != null && this.hydraulicLineHeater.isEnabled() == false) {
                if (this.cpm.getComponent(this.hydraulicLineHeater.id()) instanceof HydraulicLineHeater) {
                    this.hydraulicLineHeater = this.cpm.getComponent(this.hydraulicLineHeater.id());
                }
            }
            if (this.heatPump != null && this.heatPump.isEnabled() == false) {
                if (this.cpm.getComponent(this.heatPump.id()) instanceof Pump) {
                    this.heatPump = this.cpm.getComponent(this.heatPump.id());
                }
            }
            if (this.getSetMaximumRequestChannel().value().isDefined()) {
                this.setMaximumRequests(this.getSetMaximumRequestChannel().value().get());

                this.getSetMaximumRequestChannel().setNextWriteValue(null);

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.logger.warn("Couldn't acquire all Components in " + super.id() + " Reason: " + e.getMessage());
        }

        Optional<Integer> isSetMaximumWritten = this.getSetMaximumRequestChannel().getNextWriteValueAndReset();
        isSetMaximumWritten.ifPresent(integer -> this.getMaximumRequestChannel().setNextValue(integer));
        //Sets the Maximum allowed Requests at once in Master and Manager.
        this.maxAllowedRequests = this.getMaximumRequests();
        if (this.maxAllowedRequests != this.communicationController.getRequestManager().getMaxRequestsAtOnce()) {
            this.communicationController.getRequestManager().setMaxManagedRequests(this.maxAllowedRequests);
        }
    }

    /**
     * Fallback Logic of this controller, depending on the set FallbackLogic.
     * Default case is to activate the Components (Heatpump/Lineheater) and
     * set the ThresholdTemperature as well as enabling/activate it, set The Id to this Component.
     */
    private void fallbackLogic() {
        FallbackHandling fallback = this.getExecutionOnFallback();

        if (fallback == null || fallback.equals(FallbackHandling.UNDEFINED)) {
            fallback = FallbackHandling.DEFAULT;
        }
        switch (fallback) {

            case HEAT:
                break;
            case OPEN:
                break;
            case CLOSE:
                break;
            case DEFAULT:
            default:
                this.communicationController.enableAllRequests();
                this.requestTypeIsSet.forEach((key, value) -> {
                    value.set(true);
                });
                this.activateAllComponents();
        }
    }

    /**
     * Standard Logic of the Controller.
     * Get the communicationController and execute their Logic.
     * Get all the currentManagedRequests and check for "true" Requests. Set Callback to true,
     * so RemoteComponents are allowed to react.
     * If extra Heat is requested set to true for later handling.
     */
    private void checkRequestSizeAndEnableCallbackActivateComponents() {
        //Handle Requests
        this.communicationController.executeLogic();
        Map<RequestType, AtomicBoolean> cleanRequestTypeMap = new HashMap<>();
        Arrays.asList(RequestType.values()).forEach(request -> {
            cleanRequestTypeMap.put(request, new AtomicBoolean(false));
        });

        if (this.communicationController instanceof RestLeafletCommunicationController) {
            Map<Integer, List<RestRequest>> currentRestRequests =
                    ((RestLeafletCommunicationController) this.communicationController).getRestManager().getManagedRequests();
            if (currentRestRequests.size() > 0) {
                currentRestRequests.forEach((key, value) -> {
                    value.forEach(restRequest -> {
                        if (restRequest.getRequest().getValue().equals("1") || this.forcing) {
                            restRequest.getCallbackRequest().setValue("1");
                            cleanRequestTypeMap.get(restRequest.getRequestType()).set(true);
                        }
                    });
                });
            }
            this.requestSizeThisRun.getAndAdd(currentRestRequests.size());
        }
        cleanRequestTypeMap.forEach((key, value) -> this.requestTypeIsSet.get(key).set(value.get()));
        if (this.requestSizeThisRun.get() > 0) {
            this.handleComponents();
        } else {
            this.deactivateAllComponents();
        }
        this.getCurrentRequestsChannel().setNextValue(this.requestSizeThisRun.get());

    }


    private void handleComponents() {

        this.requestTypeIsSet.forEach((key, value) -> {
            List<ResponseWrapper> responses = this.requestTypeAndResponses.get(key);
            if (responses != null) {
                responses.forEach(wrapper -> {
                    try {
                        if (value.get()) {
                            this.activateTheWrapper(wrapper);
                        } else {
                            this.deactivateTheWrapper(wrapper);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logger.warn("Couldn't activate Components: " + super.id() + ". Reason: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void deactivateAllComponents() {
        this.requestTypeIsSet.forEach((key, value) -> {
            value.set(false);
        });
        this.handleComponents();
    }

    private void activateAllComponents() {
        this.requestTypeIsSet.forEach((key, value) -> {
            value.set(true);
        });
        this.handleComponents();
    }

    private void deactivateTheWrapper(ResponseWrapper wrapper) throws OpenemsError.OpenemsNamedException {
        if (wrapper instanceof MethodResponse) {
            this.reactToMethodOfWrapper(((MethodResponse) wrapper).getMethod(), ((MethodResponse) wrapper).getPassiveValue());
        } else if (wrapper instanceof ChannelAddressResponse) {
            ((ChannelAddressResponse) wrapper).passiveResponse();
        }
    }

    private void activateTheWrapper(ResponseWrapper wrapper) throws OpenemsError.OpenemsNamedException {
        if (wrapper instanceof MethodResponse) {
            this.reactToMethodOfWrapper(((MethodResponse) wrapper).getMethod(), ((MethodResponse) wrapper).getActiveValue());
        } else if (wrapper instanceof ChannelAddressResponse) {
            ((ChannelAddressResponse) wrapper).activateResponse();
        }
    }

    private void reactToMethodOfWrapper(MethodTypes type, String value) throws OpenemsError.OpenemsNamedException {
        switch (type) {
            case LOG_INFO:
                this.logger.info(value);
                break;
            case ACTIVATE_PUMP:
                if (this.heatPump != null) {
                    this.heatPump.setPowerLevel(Double.parseDouble(value));
                } else {
                    this.logger.warn("Wanted to set Heatpump to value: " + value + " But it is not instantiated! " + super.id());
                }
                break;
            case ACTIVATE_LINEHEATER:
                if (this.hydraulicLineHeater != null) {
                    Boolean lineHeaterActivation = null;
                    if (value != null || value.equals("null") == false) {
                        lineHeaterActivation = Boolean.valueOf(value);
                    }
                    this.hydraulicLineHeater.enableSignal().setNextWriteValueFromObject(lineHeaterActivation);
                } else {
                    this.logger.warn("Wanted to set HydraulicLineHeater to : " + value + " But it is not instantiated! " + super.id());
                }
                break;
        }
    }


    @Override
    public CommunicationController getCommunicationController() {
        return this.communicationController;
    }
}