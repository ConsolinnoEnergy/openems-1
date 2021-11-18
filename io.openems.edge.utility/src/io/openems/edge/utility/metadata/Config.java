package io.openems.edge.utility.metadata;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "MetaData ", description = "Enter MetaData, such")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "MetaData";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Street", description = "StreetName")
    String street() default "Musterstrasse";

    @AttributeDefinition(name = "HouseNumber", description = "HouseNumber")
    String houseNumber() default "1a";

    @AttributeDefinition(name = "PostalCode", description = "PostalCode")
    String postalCode() default "59494";

    @AttributeDefinition(name = "PlaceOfResidence", description = "PlaceOfResidence")
    String placeOfResidence() default "Soest";

    @AttributeDefinition(name = "SerialNumber - Leaflet - BaseModule", description = "The SerialNumber of the Leaflet - Base Module")
    String serialNumber() default "123456789";

    @AttributeDefinition(name = "InstallationDate", description = "Date of Installation: DD.MM.YYYY")
    String installationDate() default "10.12.2021";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";

}
