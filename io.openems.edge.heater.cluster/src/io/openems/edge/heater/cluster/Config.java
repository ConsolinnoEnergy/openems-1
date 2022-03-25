package io.openems.edge.heater.cluster;

import io.openems.edge.heater.api.EnergyControlMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Heater Cluster", description = "A Heat Cluster. That handles Multiple Heater and turns them on depending on the demand and priority.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "HeaterCluster0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "ControlModeInput", description = "Are incoming setPoints, set to Percent or KW. Temperature is NOT supported.")
    EnergyControlMode energyControlModeInput() default EnergyControlMode.PERCENT;

    @AttributeDefinition(name = "ControlModeOutput", description = "Converts the SetPointPower to the corresponding output e.g. if a heater expects Percent as SetPoint use PERCENT. Temperature is NOT supported")
    EnergyControlMode energyControlModeOutput() default EnergyControlMode.PERCENT;

    @AttributeDefinition(name = "Default Power Level", description = "When neither recommended or Overwrite is given, this PowerLevel will be set")
    float defaultPowerLevel() default 100.f;

    @AttributeDefinition(name = "Maximum possible Power for one Heater in KW", description = "It is recommended to cluster Heater with same performance -> what is the individual Heater power")
    int maxPowerOfaSingleHeaterInThisCluster() default 150;

    @AttributeDefinition(name = "Sort Heater", description = "When this is set "
            + "-> Sort Heater within Priority by given Channel and SortType")
    boolean sortHeater() default false;

    @AttributeDefinition(name = "SortType", description = "Sort ascending oder descending.")
    SortType sortType() default SortType.ASCENDING;

    @AttributeDefinition(name = "channelId", description = "By this channel the heater will be sorted by.")
    String channelId() default "MaintenanceInterval";

    @AttributeDefinition(name = "DeltaTime", description = "How often do you want the heaters sorted.")
    int maxTimeInterval() default 600;

    @AttributeDefinition(name = "TimerId", description = "Timer that handles the DeltaTime.")
    String timerId() default "TimerByTime";

    @AttributeDefinition(name = "High Prio Heater", description = "List here the Heater with the highest Priority (Order matters)")
    String[] highPrioHeater() default {"Heater0","Heater1","Heater2"};

    @AttributeDefinition(name = "Mid Prio Heater", description = "List here the Heater with middle Priority (Order matters)")
    String[] midPrioHeater() default {"Heater3","Heater4","Heater5"};

    @AttributeDefinition(name = "Low Prio Heater", description = "List here the Heater with low Priority (Order matters)")
    String[] lowPrioHeater() default {"Heater6","Heater7","Heater8"};

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heater Cluster {id}";
}
