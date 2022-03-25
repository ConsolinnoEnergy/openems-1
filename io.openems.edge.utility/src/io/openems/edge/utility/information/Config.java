package io.openems.edge.utility.information;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Information Component", description = "This component lists every channel of the given component")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Information component.")
    String id() default "InformationComponent0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";
    @AttributeDefinition(name = "Other Component Id", description = "Component Id of the Component you want to display.")
    String otherComponentId() default "OpenEmsComponent0";

    String[] allChannel();

    String [] writeChannel();

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Information Component {id}";
}
