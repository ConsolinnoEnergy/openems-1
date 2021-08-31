package io.openems.edge.consolinno.leaflet.sensor.signal.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
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

/**
 * A Dummy Signal Sensor, it is used as a TestComponent.
 */

@Component(name = "DummySignalSensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class DummySignalSensor extends AbstractOpenemsComponent implements OpenemsComponent, SignalSensor {


    public DummySignalSensor(String id, boolean enabled) throws ConfigurationException {
        super(OpenemsComponent.ChannelId.values(), SignalSensor.ChannelId.values());
        super.activate(null, id, "", enabled);
    }


}
