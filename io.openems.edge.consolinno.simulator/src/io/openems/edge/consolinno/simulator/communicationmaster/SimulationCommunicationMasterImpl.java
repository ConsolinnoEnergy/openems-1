package io.openems.edge.consolinno.simulator.communicationmaster;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineController;
import io.openems.edge.heater.decentralized.api.DecentralizedHeater;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulation.CommunicationMaster",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class SimulationCommunicationMasterImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    @Reference
    ComponentManager cpm;

    Random random = new Random();
    private int maxRequests;
    private final AtomicInteger currentRequests = new AtomicInteger();
    Map<DecentralizedHeater, Boolean> isManaged = new HashMap<>();
    List<DecentralizedHeater> managedHeaterList = new ArrayList<>();
    Map<DecentralizedHeater, DateTime> workMap = new HashMap<>();
    private static final int MIN_WORK_TIME_IN_SECONDS = 20;

    private boolean useHeater;
    private boolean useHydraulicLineHeater;
    private final List<DecentralizedHeater> decentralizedHeaterList = new ArrayList<>();
    private HydraulicLineController hydraulicLineController;

    public SimulationCommunicationMasterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        OpenemsError.OpenemsNamedException[] exNamed = {null};
        ConfigurationException[] exConfig = {null};
        if (config.useHeater()) {
            List<String> decentralHeaterStrings = Arrays.asList(config.decentralHeaterIds());
            decentralHeaterStrings.forEach(entry -> {
                if (exNamed[0] == null && exConfig[0] == null) {
                    OpenemsComponent component;
                    try {
                        component = cpm.getComponent(entry);
                        if (component instanceof DecentralizedHeater) {
                            this.decentralizedHeaterList.add((DecentralizedHeater) component);
                        } else {
                            exConfig[0] = new ConfigurationException("Activate: SimulationCommunicationMasterImpl", component.id() + " not a DecentralHeater!");
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        exNamed[0] = e;
                    }

                }
            });
        }
        if (config.useHydraulicLineHeater()) {
            OpenemsComponent lineHeater = cpm.getComponent(config.hydraulicLineHeaterId());
            if (lineHeater instanceof HydraulicLineController) {
                this.hydraulicLineController = (HydraulicLineController) lineHeater;
            } else {
                throw new ConfigurationException("ActivateMethod SimulationCommunicationMaster", "HydraulicLineHeaterId not correct" + lineHeater.id());
            }
        }
        if (exNamed[0] != null) {
            throw exNamed[0];
        }
        if (exConfig[0] != null) {
            throw exConfig[0];
        }
        this.maxRequests = config.maxSize();
        this.useHeater = config.useHeater();
        this.useHydraulicLineHeater = config.useHydraulicLineHeater();
    }


    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        checkMissingComponent();
        AtomicBoolean atLeastOneRequest = new AtomicBoolean(false);
        if (this.useHeater) {
            this.decentralizedHeaterList.forEach(decentralizedHeater -> {
                if (decentralizedHeater.getNeedHeat()) {
                    atLeastOneRequest.set(true);
                    currentRequests.getAndIncrement();
                    if (this.managedHeaterList.size() < this.maxRequests) {
                        //say with 99% probability it's ok to write true --> else false --> some randomness in tests
                        enableOrDisableHeater(decentralizedHeater, random.nextInt(100) < 99);
                    } else {
                        enableOrDisableHeater(decentralizedHeater, false);
                    }
                } else {
                    enableOrDisableHeater(decentralizedHeater, false);
                }

            });
            //More Requests than allowed ---> some are awaiting enabledSignal
            if (currentRequests.get() > this.maxRequests && random.nextBoolean()) {
                DateTime now = new DateTime();
                this.isManaged.forEach((key, value) -> {
                    if (value) {
                        if (now.isAfter(this.workMap.get(key).plusSeconds(MIN_WORK_TIME_IN_SECONDS))) {
                            this.isManaged.replace(key, random.nextBoolean());
                        }
                    }
                    if (value == false) {
                        this.managedHeaterList.remove(key);
                        try {
                            key.getNeedHeatEnableSignalChannel().setNextWriteValue(false);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        if (this.useHydraulicLineHeater) {
            boolean enableSignal = atLeastOneRequest.get() && random.nextInt(100) < 90;
            this.hydraulicLineController.enableSignalChannel().setNextWriteValue(enableSignal);
        }
    }

    private void enableOrDisableHeater(DecentralizedHeater decentralHeater, boolean enable) {
        try {
            this.isManaged.put(decentralHeater, enable);
            decentralHeater.getNeedHeatEnableSignalChannel().setNextWriteValue(enable);
            if (enable) {
                this.managedHeaterList.add(decentralHeater);
                this.workMap.put(decentralHeater, new DateTime());
            } else {
                this.managedHeaterList.remove(decentralHeater);
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {

        }
    }

    private void checkMissingComponent() {
        if (this.hydraulicLineController != null && this.hydraulicLineController.isEnabled() == false) {
            try {
                this.hydraulicLineController = cpm.getComponent(this.hydraulicLineController.id());
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
            List<DecentralizedHeater> missingHeater = this.decentralizedHeaterList.stream().filter(heater -> heater.isEnabled() == false).collect(Collectors.toList());
            missingHeater.forEach(missing -> {
                try {
                    this.decentralizedHeaterList.set(this.decentralizedHeaterList.indexOf(missing), cpm.getComponent(missing.id()));
                } catch (OpenemsError.OpenemsNamedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

}