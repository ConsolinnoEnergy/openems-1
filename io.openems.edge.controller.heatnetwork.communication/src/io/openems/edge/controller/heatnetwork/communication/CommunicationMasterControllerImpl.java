package io.openems.edge.controller.heatnetwork.communication;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.rest.api.RestRemoteDevice;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationMasterController;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.Request;
import io.openems.edge.controller.heatnetwork.communication.api.RestLeafletCommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.request.api.ConfigPosition;
import io.openems.edge.controller.heatnetwork.communication.request.api.MasterResponseType;
import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.controller.heatnetwork.communication.request.rest.RestRequestImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ChannelAddressResponse;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ChannelAddressResponseImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.MethodResponse;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.MethodResponseImpl;
import io.openems.edge.controller.heatnetwork.communication.responsewrapper.ResponseWrapper;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineController;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

/**
 * The CommunicationMasterController.
 * It Responses to Remote Requests and determined by the RequestType of the Request, it calls methods or writes into Channels,
 * depending on the Configuration.
 * Example:
 * The MasterConfiguration awaits the RequestType HEAT. On HEAT Requests it starts a HeatPump.
 * If any of it's stored RemoteRequests are from the Type HEAT, and
 * the Remote Requests requesting heat, the MasterController enables the HeatPump.
 * Same goes for MoreHeat or any other configurable request.
 * See: {@link RequestType} {@link ConnectionType} for supported RequestTypes and Connection Types, as well as
 * {@link MasterResponseType} and {@link MethodTypes} for supported Responses.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.CommunicationMaster",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CommunicationMasterControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller,
        CommunicationMasterController, ExceptionalState {

    private final Logger log = LoggerFactory.getLogger(CommunicationMasterControllerImpl.class);
    private static final CommunicationControllerHelper HELPER = new CommunicationControllerHelper();
    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;
    //Configured communicationController handling one remoteCommunication
    CommunicationController communicationController;
    //The Optional HydraulicLineHeater
    private HydraulicLineController hydraulicLineController;
    //The Optional HeatPump
    private HydraulicComponent heatPump;
    //For Subclasses -> CommunicationController and manager
    private boolean forcing;
    private int maxAllowedRequests;
    //Current request size --> IF Size is empty -> deactivate extra component else activate them.
    //Is declared as an Integer bc. of future implementation : Do XYZ at Certain Size
    //resets every run
    private final AtomicInteger requestSizeThisRun = new AtomicInteger(0);
    private final AtomicInteger configFailCounter = new AtomicInteger(0);
    private static final int MAX_FAIL_COUNTER = 10;
    private Config config;
    private boolean configSucceed;
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
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        if (config.requestTypes().length != RequestType.values().length
                || config.methodTypes().length != MethodTypes.values().length
                || config.masterResponseTypes().length != MasterResponseType.values().length) {
            this.updateApacheConfig();
            return;
        }
        try {
            this.configureController(config);
            if (this.communicationController != null) {
                this.communicationController.enable();
            }
        } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply config, try again later " + super.id());
            this.configSucceed = false;
            this.clearReferences();
        }
        this.configSucceed = true;
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.clearReferences();
        try {
            this.configureController(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply modified Configuration : " + super.id());
            this.configSucceed = false;
        }
    }

    /**
     * Configures Controller after activation/modification/in run() method if configuration failed.
     *
     * @param config the config of this component
     * @throws ConfigurationException             if somethings wrong with the Config.
     * @throws OpenemsError.OpenemsNamedException if applied Id's aren't available.
     */
    private void configureController(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        //ForceHeating == Heat ALL remote Heater/handle ALL requests/Enable all requests even if they don't have a request
        this.configurationDone = config.configurationDone();
        if (this.configurationDone) {
            this.setForceHeating(config.forceHeating());
            this.setMaximumRequests(config.maxRequestAllowedAtOnce());
            this.forcing = this.getForceHeating();
            this.maxAllowedRequests = this.getMaximumRequests();
            this.communicationController = HELPER.createCommunicationControllerWithRequests(config, this.cpm);
            this.communicationController.setMaxWaitTime(config.maxWaitTimeAllowed());
            this.communicationController.setTimerTypeForManaging(config.timerForManager());
            this.communicationController.setMaxRequests(this.maxAllowedRequests);
            //Creates the Controller responsible for handling RemoteRequests (REST requests)
            OpenemsComponent optionalComponent;

            if (config.usePump()) {
                optionalComponent = this.cpm.getComponent(config.pumpId());
                if (optionalComponent instanceof HydraulicComponent) {
                    this.heatPump = (HydraulicComponent) optionalComponent;
                } else {
                    throw new ConfigurationException("CommunicationMaster - Activate - Pump", "PumpId Component - Not an instance of Pump; PumpId: " + config.pumpId());
                }
            }
            if (config.useHydraulicLineHeater()) {
                optionalComponent = this.cpm.getComponent(config.hydraulicLineHeaterId());
                if (optionalComponent instanceof HydraulicLineController) {
                    this.hydraulicLineController = (HydraulicLineController) optionalComponent;
                } else {
                    throw new ConfigurationException("CommunicationMaster - Activate - HydraulicLineHeater",
                            "HydraulicLineHeaterId Component - Not an Instance of HydraulicLineHeater : " + config.hydraulicLineHeaterId());
                }
            }
            this.setResponsesToRequests(Arrays.asList(config.requestTypeToResponse()));
            Arrays.asList(RequestType.values()).forEach(requestType -> this.requestTypeIsSet.put(requestType, new AtomicBoolean(false)));

            this.setForceHeating(config.forceHeating());
            this.timer = new TimerHandlerImpl(this.id(), this.cpm);
            this.timer.addOneIdentifier(KEEP_ALIVE_IDENTIFIER, config.timerId(), config.keepAlive());
            this.useExceptionalStateHandling = config.useExceptionalStateHandling();
            if (this.useExceptionalStateHandling) {
                this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerIdExceptionalState(), config.exceptionalStateTime());
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);
                this.getExceptionalStateValueChannel().setNextValue(100);
            }
            FallbackHandling fallback = config.fallback();

            this.setFallbackLogic(fallback);
            this.maxAllowedRequests = config.maxRequestAllowedAtOnce();
            this.getMaximumRequestChannel().setNextValue(this.maxAllowedRequests);
            this.configSucceed = true;
        }
    }

    /**
     * Sets up the Responses by the RequestType.
     * It splits up each entry of the requestType and gets depending on the {@link ConfigPosition} the corresponding configuration entries,
     * such as the {@link RequestType} or the {@link MasterResponseType}.
     *
     * @param requestTypeToResponse the List of the Configuration for responses, usually from {@link Config}
     * @throws ConfigurationException if any error occurred.
     */
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

    /**
     * Updates the Config in the Apache Web Config and fills the Request and MethodsTypes.
     */
    private void updateApacheConfig() {
        Configuration c;

        try {
            c = this.cm.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String types = Arrays.toString(RequestType.values());
            properties.put("requestTypes", this.propertyInput(types));

            types = Arrays.toString(MethodTypes.values());
            properties.put("methodTypes", this.propertyInput(types));
            types = Arrays.toString(MasterResponseType.values());
            properties.put("masterResponseTypes", this.propertyInput(types));

            c.update(properties);

        } catch (IOException e) {
            this.log.warn("Couldn't update Config, in CommunicationMaster: " + super.id() + " Reason: " + e.getCause());
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


    @Deactivate
    protected void deactivate() {
        this.deactivateAllResponses();
        this.communicationController.disable();
        this.timer.removeComponent();
        super.deactivate();
    }

    /**
     * If Config is ok (Happens on e.g. restart of OpenEms or if some Components aren't available yet)
     * And the Configuration is Done (Boolean in Config)
     * Check for ExceptionalStates -> What to Do
     * Otherwise -> Check if the Connection is ok and if that's the case, handle the Requests and the Responses.
     */
    @Override
    public void run() {
        if (this.configSucceed) {
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
                            this.activateAllResponses();
                        } else {
                            this.communicationController.disableAllRequests();
                            this.deactivateAllResponses();
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
        } else {
            try {
                this.configureController(this.config);
            } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
                this.clearReferences();
                if (this.configFailCounter.get() >= MAX_FAIL_COUNTER) {
                    this.log.error("Failed to assign config!");
                } else {
                    this.configFailCounter.getAndIncrement();
                }
            }
        }
    }


    /**
     * Fallback Logic of this controller, depending on the set FallbackLogic.
     * Default case is to activate the Components (Heatpump/Lineheater) and
     * set the ThresholdTemperature as well as enabling/activate it, set The Id to this Component.
     */
    private void fallbackLogic() {
        switch (this.getExecutionOnFallback()) {

            case UNDEFINED:
                break;
            case DEFAULT:
            default:
                this.communicationController.enableAllRequests();
                this.requestTypeIsSet.forEach((key, value) -> value.set(true));
                this.activateAllResponses();
                break;
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
        Arrays.asList(RequestType.values()).forEach(request -> cleanRequestTypeMap.put(request, new AtomicBoolean(false)));

        int currentRequestSize = this.communicationController.enableManagedRequestsAndReturnSizeOfManagedRequests(this.forcing, cleanRequestTypeMap);

        this.requestSizeThisRun.set(currentRequestSize);
        //Sets the current Values of the RequestTypes, set by the CommunicationController
        cleanRequestTypeMap.forEach((key, value) -> this.requestTypeIsSet.get(key).set(value.get()));
        if (this.requestSizeThisRun.get() > 0) {
            this.handleComponents();
        } else {
            this.deactivateAllResponses();
        }
        this.getCurrentRequestsChannel().setNextValue(this.requestSizeThisRun.get());
    }

    /**
     * Get all the Responses and activate/Deactivate them depending if the requestType is set.
     */
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
                        this.log.warn("Couldn't activate Components: " + super.id() + ". Reason: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Internal method, to deactivate all Responses.
     */
    private void deactivateAllResponses() {
        this.requestTypeIsSet.forEach((key, value) -> value.set(false));
        this.handleComponents();
    }

    /**
     * Internal method, to activate all Responses.
     */
    private void activateAllResponses() {
        this.requestTypeIsSet.forEach((key, value) -> value.set(true));
        this.handleComponents();
    }

    /**
     * Deactivates the Response corresponding to the Wrapper. Exact handling is determined by the {@link MethodResponse}.
     *
     * @param wrapper the Wrapper stored in the {@link #requestTypeAndResponses}
     * @throws OpenemsError.OpenemsNamedException if setNext(Write)Value fails.
     */
    private void deactivateTheWrapper(ResponseWrapper wrapper) throws OpenemsError.OpenemsNamedException {
        if (wrapper instanceof MethodResponse) {
            this.reactToMethodOfWrapper(((MethodResponse) wrapper).getMethod(), ((MethodResponse) wrapper).getPassiveValue());
        } else if (wrapper instanceof ChannelAddressResponse) {
            ((ChannelAddressResponse) wrapper).passiveResponse();
        }
    }

    /**
     * Activates the Response corresponding to the Wrapper. Exact handling is determined by the {@link MethodResponse}.
     *
     * @param wrapper the Wrapper stored in the {@link #requestTypeAndResponses}
     * @throws OpenemsError.OpenemsNamedException if setNext(Write)Value fails.
     */

    private void activateTheWrapper(ResponseWrapper wrapper) throws OpenemsError.OpenemsNamedException {
        if (wrapper instanceof MethodResponse) {
            this.reactToMethodOfWrapper(((MethodResponse) wrapper).getMethod(), ((MethodResponse) wrapper).getActiveValue());
        } else if (wrapper instanceof ChannelAddressResponse) {
            ((ChannelAddressResponse) wrapper).activateResponse();
        }
    }

    /**
     * If the Wrapper is of RequestType {@link MethodTypes}, this method will be called and handles the rest, depending on the value.
     *
     * @param type  the MethodType
     * @param value the value to write.
     * @throws OpenemsError.OpenemsNamedException if somethings wrong.
     */
    private void reactToMethodOfWrapper(MethodTypes type, String value) throws OpenemsError.OpenemsNamedException {
        switch (type) {
            case LOG_INFO:
                this.log.info(value);
                break;
            case ACTIVATE_PUMP:
                if (this.heatPump != null) {
                    this.heatPump.setPowerLevel(Double.parseDouble(value));
                } else {
                    this.log.warn("Wanted to set Heatpump to value: " + value + " But it is not instantiated! " + super.id());
                }
                break;
            case ACTIVATE_LINE_HEATER:
                if (this.hydraulicLineController != null) {
                    Boolean lineHeaterActivation = null;
                    if (value != null || value.equals("null") == false) {
                        lineHeaterActivation = Boolean.valueOf(value);
                    }
                    this.hydraulicLineController.enableSignalChannel().setNextWriteValueFromObject(lineHeaterActivation);
                } else {
                    this.log.warn("Wanted to set HydraulicLineHeater to : " + value + " But it is not instantiated! " + super.id());
                }
                break;
        }
    }

    /**
     * This Method is called, if an error while configuring Components.
     */
    private void clearReferences() {
        this.communicationController = null;
        this.hydraulicLineController = null;
        this.heatPump = null;
        this.requestTypeAndResponses.clear();
        this.requestTypeIsSet.clear();
    }

    /**
     * Checks if hydraulicHeater and Pump are still enabled (if Present and configured).
     */
    private void checkChangesAndApply() {
        //Check if Components are still enabled
        try {
            if (this.hydraulicLineController != null && this.hydraulicLineController.isEnabled() == false) {
                if (this.cpm.getComponent(this.hydraulicLineController.id()) instanceof HydraulicLineController) {
                    this.hydraulicLineController = this.cpm.getComponent(this.hydraulicLineController.id());
                }
            }
            if (this.heatPump != null && this.heatPump.isEnabled() == false) {
                if (this.cpm.getComponent(this.heatPump.id()) instanceof HydraulicComponent) {
                    this.heatPump = this.cpm.getComponent(this.heatPump.id());
                }
            }
            if (this.getSetMaximumRequestChannel().value().isDefined()) {
                this.setMaximumRequests(this.getSetMaximumRequestChannel().value().get());

                this.getSetMaximumRequestChannel().setNextWriteValue(null);

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't acquire all Components in " + super.id() + " Reason: " + e.getMessage());
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
     * This static class helps the CommunicationMaster to Create a correct CommunicationController.
     */
    private static class CommunicationControllerHelper {

        private enum ConfigPositionForRequests {
            REQUEST(0), CALLBACK(1), KEY_FOR_MAP(2), REQUEST_TYPE(3);
            int position;

            ConfigPositionForRequests(int position) {
                this.position = position;
            }
        }

        /**
         * Creates a CommunicationController and adds the the Requests to it.
         *
         * @param config the Config of the CommunicationMaster.
         * @param cpm    the ComponentManager of the CommunicationMaster
         * @return a CommunicationController
         * @throws ConfigurationException             if the Config is wrong
         * @throws OpenemsError.OpenemsNamedException if a ComponentId cannot be found at all.
         */
        public CommunicationController createCommunicationControllerWithRequests(Config config, ComponentManager cpm)
                throws ConfigurationException, OpenemsError.OpenemsNamedException {
            CommunicationController controller;
            if (config.connectionType() == ConnectionType.REST) {
                controller = new RestLeafletCommunicationControllerImpl(config.connectionType(),
                        config.manageType(), config.maxRequestAllowedAtOnce(),
                        config.forceHeating());
                controller.setMaxWaitTime(config.maxWaitTimeAllowed());
            } else {
                throw new ConfigurationException("CreateCommunicationControllerWithRequests",
                        "ConnectionType is not supported yet! " + config.connectionType());
            }
            this.createRemoteRequestsAndAddToCommunicationController(config, controller, cpm);
            return controller;
        }

        /**
         * Creates the RemoteRequests from Config. --> e.g. RestRemoteComponents will be handled bei a RestCommunicationController
         * get the RequestConfig, and split them.
         * Map the Configured Requests to the corresponding requestMaps.
         * add them to the CommunicationController if they are correct. Else throw Exception.
         *
         * @param config     config of the Component
         * @param controller the controller created before
         *                   at {@link #createCommunicationControllerWithRequests(Config, ComponentManager)}
         * @param cpm        the ComponentManager of the CommunicationMaster
         * @throws ConfigurationException             if there's an error within config
         * @throws OpenemsError.OpenemsNamedException if cpm couldn't find OpenemsComponent
         */

        private void createRemoteRequestsAndAddToCommunicationController(Config config,
                                                                         CommunicationController controller, ComponentManager cpm)
                throws ConfigurationException, OpenemsError.OpenemsNamedException {
            //ErrorHandling in Streams..you can't simply throw exceptions in Streams in Java 8
            ConfigurationException[] ex = {null};
            OpenemsError.OpenemsNamedException[] exceptions = {null};
            List<String> requestConfig = Arrays.asList(config.requestMap());
            //Collection of all Requests, can be any  (sub) Type of request, important for future impl. -> Different RequestTypes
            Map<Integer, List<? super Request>> requestMap = new HashMap<>();

            requestConfig.forEach(entry -> {
                try {
                    if (ex[0] == null) {
                        this.createRequestFromConfigEntry(entry, requestMap, controller.getConnectionType(), cpm);
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
            if (controller.getConnectionType().equals(ConnectionType.REST)) {
                Map<Integer, List<RestRequest>> createdRestRequests = new HashMap<>();
                requestMap.forEach((key, value) -> {
                    List<RestRequest> requestsOfKey = new ArrayList<>();
                    value.forEach(entry -> requestsOfKey.add((RestRequest) entry));
                    createdRestRequests.put(key, requestsOfKey);
                });
                ((RestLeafletCommunicationController) controller).addRestRequests(createdRestRequests);
            } else {
                throw new ConfigurationException(controller.getConnectionType().toString(), "Is not supported by this controller");
            }

        }

        /**
         * Creates a Requests from a configuration and puts it into the requestMap.
         * First Split the entries by ":", then check each split param if correct, if params are ok, create corresponding RestRequest.
         *
         * @param entry          the entry usually from Config or Channel.
         * @param requestMap     the requestMap coming from calling method (if config done it will be added to the controller)
         * @param connectionType the connection Type that is configured.
         * @param cpm            the ComponentManager of the CommunicationMaster
         * @throws ConfigurationException             if Somethings wrong with the configuration.
         * @throws OpenemsError.OpenemsNamedException if the cpm couldn't find the component.
         */
        private void createRequestFromConfigEntry(String entry, Map<Integer, List<? super Request>> requestMap,
                                                  ConnectionType connectionType, ComponentManager cpm)
                throws ConfigurationException, OpenemsError.OpenemsNamedException {
            String[] entries = entry.split(":");
            if (entries.length != ConfigPositionForRequests.values().length) {
                throw new ConfigurationException("" + entries.length, "Length not ok expected " + ConfigPositionForRequests.values().length);
            }
            //REQUEST (Pos 0), CALLBACK (Pos 1), KEY (Pos 2), REQUEST_TYPE(3)
            OpenemsComponent request = cpm.getComponent(entries[ConfigPositionForRequests.REQUEST.position]);
            OpenemsComponent callback = cpm.getComponent(entries[ConfigPositionForRequests.CALLBACK.position]);
            int keyForMap = Integer.parseInt(entries[ConfigPositionForRequests.KEY_FOR_MAP.position]);
            String requestTypeString = entries[ConfigPositionForRequests.REQUEST_TYPE.position].toUpperCase().trim();
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


    }

}
