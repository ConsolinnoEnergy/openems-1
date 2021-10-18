package io.openems.edge.apartmentmodule;

import io.openems.edge.apartmentmodule.api.ValveDirection;
import io.openems.edge.apartmentmodule.api.ValveType;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Miscellaneous Apartment Module",
        description = "A module to map Modbus calls to OpenEMS channels for a Apartment Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "ApartmentModule-Device ID", description = "Unique Id of the Apartment Module.")
    String id() default "ApartmentModule0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the Apartment Module.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component. Decides if the Apartment module is in top or bottom configuration.")
    ModbusId modbusUnitId() default ModbusId.ID_2;

    @AttributeDefinition(name = "Temperature sensor calibration", description = "Calibration value for the PT1000 temperature sensor.")
    int tempCal() default 70;

    ValveType valveType() default ValveType.ONE_OPEN_ONE_CLOSE;

    @AttributeDefinition(name = "Relay Open or Direction HydraulicMixer", description = "Define the Relays that opens/Direction the hydraulicMixer (1 or 2)")
    int turnOnOrDirectionRelay() default 1;

    @AttributeDefinition(name = "Direction Meaning", description = "IF your valve is of the OneDirection type, when the Relay is activated, what direction does it take")
    ValveDirection valveDirection() default ValveDirection.ACTIVATION_DIRECTIONAL_EQUALS_OPENING;

    @AttributeDefinition(name = "Control Manually", description = "If you set up the relays by hand, tick this box so the position won't change.")
    boolean manuallyControlled() default false;

    boolean useMaxTemperature() default true;

    @AttributeDefinition(name = "Max Temperature", description = "If this temperature is reached. Close Valve.")
    int maxTemperature() default 700;

    boolean useMinTemperature() default true;

    @AttributeDefinition(name = "Min Temperature", description = "If this temperature is reached. Close Valve.")
    int minTemperature() default 450;

    boolean useReferenceTemperature() default false;

    @AttributeDefinition(name = "Reference Thermometer Temperature", description = "Use a ReferenceTemperature to check if secondary Side of AM is ok."
            + "Set the min Temperature allowed for secondary side.")
    int minTemperatureThermometer() default 400;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Miscellaneous Apartment Module Device [{id}]";

}