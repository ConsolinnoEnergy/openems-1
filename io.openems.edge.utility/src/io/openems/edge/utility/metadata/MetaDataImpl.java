package io.openems.edge.utility.metadata;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.utility.api.MetaData;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MetaData class. This Stores the Information about
 */

@Designate(ocd = Config.class)
@Component(name = "Utility.MetaData", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class MetaDataImpl extends AbstractOpenemsComponent implements OpenemsComponent, MetaData {

    private Logger log = LoggerFactory.getLogger(MetaDataImpl.class);

    public MetaDataImpl() {
        super(OpenemsComponent.ChannelId.values(),
                MetaData.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
        this.getInstallationDate().setNextValue(config.installationDate());

    }

    private void activationOrModifiedRoutine(Config config) {
        this.getStreet().setNextValue(config.street());
        this.getHouseNumber().setNextValue(config.houseNumber());
        this.getPostalCode().setNextValue(config.postalCode());
        this.getPlaceOfResidence().setNextValue(config.placeOfResidence());
        this.getSerialNumber().setNextValue(config.serialNumber());
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

}
