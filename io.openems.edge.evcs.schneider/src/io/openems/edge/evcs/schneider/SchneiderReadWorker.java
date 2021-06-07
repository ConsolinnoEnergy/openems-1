package io.openems.edge.evcs.schneider;


import io.openems.common.worker.AbstractWorker;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
public class SchneiderReadWorker extends AbstractWorker {

    @Reference
    protected ConfigurationAdmin cm;



    public SchneiderReadWorker(SchneiderImpl parent) {

    }


    @Override
    protected void forever() throws Throwable {

    }

    @Override
    protected int getCycleTime() {
        return 0;
    }
}
