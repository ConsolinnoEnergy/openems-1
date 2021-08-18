package io.openems.edge.kbr4f96.commands;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Kbr 4F96 Commands", description = "Commands of the Kbr 4F96 Unit.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for the Kbr Unit.")
    String id() default "KbrCommands";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Kbr Unit.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Kbr", description = "Id of Kbr Unit")
    String kbr() default "Kbr";

    @AttributeDefinition(name = "ResetDevice", description = "Reset the Device")
    boolean reset() default false;

    @AttributeDefinition(name = "ResetMaxValue", description = "Reset the Max Values")
    boolean max() default false;

    @AttributeDefinition(name = "ResetMinValue", description = "Reset the Min Values")
    boolean min() default false;

    @AttributeDefinition(name = "SwitchToHT", description = "Switch the Tariff to HT")
    boolean hT() default false;

    @AttributeDefinition(name = "HTEnergyType", description = "New Energy Type for HT (0-63)")
    short newHT() default 0;

    @AttributeDefinition(name = "SwitchToNT", description = "Switch the Tariff to NT")
    boolean nT() default false;

    @AttributeDefinition(name = "NTEnergyType", description = "New Energy Type for NT (0-63)")
    short newNT() default 0;


    @AttributeDefinition(name = "EraseFail", description = "Erase the Error Status")
    boolean error() default false;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
