package io.openems.edge.heater.cluster;

import io.openems.edge.heater.api.EnergyControlMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno ", description = ".")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ControlModeInput", description = "Are incoming setPoints, set to Percent or KW. Temperature is NOT supported")
    EnergyControlMode energyControlModeInput() default EnergyControlMode.PERCENT;

    @AttributeDefinition(name = "ControlModeOutput", description = "Converts the SetPointPower to the corresponding output e.g. if a heater expects Percent as SetPoint use PERCENT. Temperature is NOT supported")
    EnergyControlMode energyControlModeOutput() default EnergyControlMode.PERCENT;

    @AttributeDefinition(name = "Default Power Level", description = "When neither recommended or Overwrite is given, this PowerLevel will be set")
    float defaultPowerLevel() default 100.f;

    @AttributeDefinition(name = "Maximum possible Power for one Heater in KW", description = "It is recommended to cluster Heater with same performance -> what is the individual Heater power")
    int maxPowerOfaSingleHeaterInThisCluster() default 150;

    @AttributeDefinition(name = "Check Heater Maintenance", description = "When this is set "
            + "-> Check Heater Maintenance and and calculate the Heaters remaining running power, so everything is equally contributed.")
    boolean useMaintenanceInterval() default true;


    @AttributeDefinition(name = "High Prio Heater", description = "List here the Heater with the highest Priority (Order matters)")
    String[] highPrioHeater() default {"Heater0,Heater1,Heater2"};

    @AttributeDefinition(name = "Mid Prio Heater", description = "List here the Heater with middle Priority (Order matters)")
    String[] midPrioHeater() default {"Heater3,Heater4,Heater5"};

    @AttributeDefinition(name = "Low Prio Heater", description = "List here the Heater with low Priority (Order matters)")
    String[] lowPrioHeater() default {"Heater6,Heater7,Heater8"};

    /*
     * TODO DEFAULT PowerLevel
     *  HIGH MID LOW Prio Heater
     *   EnergyControlMode
     *   Default Active Power
     *   Boolean -> useMaintenance
     * */

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
