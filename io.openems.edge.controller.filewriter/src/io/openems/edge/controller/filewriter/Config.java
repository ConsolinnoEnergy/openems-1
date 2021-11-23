package io.openems.edge.controller.filewriter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import sun.security.x509.SerialNumber;

@ObjectClassDefinition(name = "Controller FileWriter JSON ", description = ".")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "FileWriterMetaData";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "The other Component", description = "The Component which Channel/Information you want to Write into")
    String otherComponentId() default "MetaData";

    @AttributeDefinition(name = "Channels of the other Component", description = "Human readable name of this Configurator.")
    String[] channels();

    @AttributeDefinition(name = "Configuration of KeyValue Pairs", description = "Configure the Key:Value pair that will be written into the File")
    String[] keyValuePairs() default {"Straße:Street", "Hausnummer:HouseNumber", "Postleitzahl:PostalCode", "Ort:PlaceOfResidence", "SerienNummer:SerialNumber", "Inbetriebnahme:InstallationDate"};

    @AttributeDefinition(name = "FileLocation", description = "Write into the (non existend) file ")
    String fileLocation() default "home/sshconsolinno/data/leaflet/network-settings.json";

    @AttributeDefinition(name = "Configuration done", description = "Is your configuration done? Usually tick this, if you configured the keyValuePairs")
    boolean configurationDone() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
