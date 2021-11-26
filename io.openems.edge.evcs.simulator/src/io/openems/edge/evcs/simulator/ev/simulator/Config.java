package io.openems.edge.evcs.simulator.ev.simulator;

import io.openems.edge.evcs.api.GridVoltage;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Evcs Simulator Car Simulator", description = "This Simulates a generic E-Car.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Simulator.")
    String id() default "EvSimulator0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "SimulatedEvcsId", description = "Id of the Evcs Simulator, this Car is connected to.")
    String parentId() default "";

    @AttributeDefinition(name = "Phases", description = "Amount of Phases the Car charges with (1-3).", required = true)
    int phases() default 3;

    @AttributeDefinition(name = "ChargePower", description = "Charge Power (in A) the Car charges with.", required = true)
    int chargePower() default 16;

    @AttributeDefinition(name = "GridVoltage", description = "Voltage of the Grid.", required = true)
    GridVoltage gridVoltage() default GridVoltage.V_230_HZ_50;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
