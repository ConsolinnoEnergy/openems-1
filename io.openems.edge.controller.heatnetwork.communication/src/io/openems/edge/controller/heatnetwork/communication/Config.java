package io.openems.edge.controller.heatnetwork.communication;


import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.timer.api.TimerType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Communication Master",
        description = "Controller to react to External (Heat) Requests and react to them by activating it's devices."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "CommunicationMaster Id", description = "ID of the CommunicationMaster")
    String id() default "CommunicationMaster0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";

    @AttributeDefinition(name = "ConnectionType", description = "ConnectionTypes, that are supported by the Communicationmaster")
    ConnectionType connectionType() default ConnectionType.REST;

    @AttributeDefinition(name = "Maximum Requests", description = "Maximum requests handled at once, mapped to Connection type")
    int maxRequestAllowedAtOnce() default 3;

    @AttributeDefinition(name = "TimerType for Manager", description = "Should the Manager use Cycles or Time")
    TimerType timerForManager() default TimerType.TIME;

    @AttributeDefinition(name = "Maximum Waiting Time", description = "Maximum Time in Minutes an element is Allowed to wait "
            + "before it gets swapped by a member of the ActiveList. Or the number of cycles.")
    int maxWaitTimeAllowed() default 30;

    @AttributeDefinition(name = "ManagingType", description = "How To Manage Requests Each Entry will be Mapped to Connection type. "
            + "Available ManageRequests are: FIFO")
    ManageType manageType() default ManageType.FIFO;

    @AttributeDefinition(name = "Timer Id", description = "Timer by Cycles or Timer by Time for keepAlive Count till fallback activates")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "KeepAlive Time", description = "Time needs to past till fallback activates")
    int keepAlive() default 300;

    @AttributeDefinition(name = "Logic on Fallback", description = "What Logic to Execute on Fallback")
    FallbackHandling fallback() default FallbackHandling.DEFAULT;

    @AttributeDefinition(name = "RequestMapper", description = "Type in RequestComponent:CallbackComponent:Position:RequestType"
            + "Available RequestTypes will be listed below")
    String[] requestMap() default "RestRemoteComponent0:RestRemoteComponent1:1:HEAT";

    String[] requestTypes() default {};

    String[] methodTypes() default {};

    String[] masterResponseTypes() default {};


    @AttributeDefinition(name = "RequestType to Response", description = "Map an available RequestType to a Response, if no Request of this type is active -> set to default Value,"
            + "If supported you can either use a ChannelAddress or an implemented Method for response "
            + "The order is: RequestType:ChannelAddressOrMethod:ActualMethodNameOrChannelAddress:ValueIfPresent:ValueIfNotPresent")
    String[] requestTypeToResponse() default {
            "HEAT:CHANNEL_ADDRESS:Pump0/SetPowerLevel:100:0",
            "HEAT:CHANNEL_ADDRESS:VirtualThermometer0/VirtualTemperature:70:0",
            "MORE_HEAT:METHOD:ACTIVATE_LINEHEATER:true:null",
            "GENERIC:METHOD:LOG_INFO:GenericRequest:NoGenericRequest"
    };

    boolean useHydraulicLineHeater() default true;

    @AttributeDefinition(name = "Optional HydraulicLineHeater", description = "Optional Hydraulic LineHeater which activates if more Heat is requests")
    String hydraulicLineHeaterId() default "hydraulicLineHeater0";

    boolean usePump() default true;

    @AttributeDefinition(name = "Optional Pump", description = "An Optional Component which activates additionally")
    String pumpId() default "Pump0";

    boolean useExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer Id", description = "TimerId to use Exceptional StateHandling for")
    String timerIdExceptionalState() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "If ExceptionalState was active before, but is no missing -> ignore Exceptional State handling after given Time")
    int exceptionalStateTime() default 30;

    boolean enabled() default true;

    boolean forceHeating() default false;

    boolean configurationDone() default false;

    String webconsole_configurationFactory_nameHint() default "CommunicationMaster [{id}]";
}