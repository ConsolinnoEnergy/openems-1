package io.openems.edge.manager.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.manager.valve.api.ManagerValve;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Manager.Valve",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ManagerValveImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ManagerValve {

    private Map<String, Valve> valves = new ConcurrentHashMap<>();

    //private final static int PERCENT_TOLERANCE_VALVE = 5;

    public ManagerValveImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    private static final int BUFFER = 5;

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void addValve(String id, Valve valve) {
        this.valves.put(id, valve);

    }

    @Override
    public void removeValve(String id) {
        this.valves.remove(id);
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        valves.values().forEach(valve -> {
            boolean maxMin = false;
            if (valve.maxValueChannel().getNextWriteValue().isPresent() && valve.maxValueChannel().getNextWriteValue().get() + BUFFER < valve.getPowerLevelValue()) {
                valve.changeByPercentage(valve.maxValueChannel().getNextWriteValue().get() - valve.getPowerLevelValue());
                if (maxMinValid(valve)) {
                    maxMin = true;
                } else {
                    maxMin = false;
                }

            } else if (valve.minValueChannel().getNextWriteValue().isPresent() && valve.minValueChannel().getNextWriteValue().get() + BUFFER > valve.getPowerLevelValue()) {
                valve.changeByPercentage(valve.minValueChannel().getNextWriteValue().get() - valve.getPowerLevelValue());
                if (maxMinValid(valve)) {
                    maxMin = true;
                } else {
                    maxMin = false;
                }
            }


            //next Value bc on short scheduler the value.get() is not quick enough updated
            //Should valve be Reset?
            if (valve.shouldReset()) {
                valve.reset();
                valve.shouldForceClose().setNextValue(false);
                valve.updatePowerLevel();
            } else {
                //Reacting to SetPowerLevelPercent by REST Request
                if (maxMin == false && valve.setPointPowerLevelChannel().value().isDefined() && valve.setPointPowerLevelChannel().value().get() >= 0) {

                    int changeByPercent = valve.setPointPowerLevelChannel().value().get();
                    //getNextPowerLevel Bc it's the true current state that's been calculated
                    if (valve.getPowerLevelChannel().getNextValue().isDefined()) {
                        changeByPercent -= valve.getPowerLevelChannel().getNextValue().get();
                    }
                    if (valve.changeByPercentage(changeByPercent)) {
                        valve.setPointPowerLevelChannel().setNextValue(-1);
                    }
                }
                //Calculate current % State of Valve
                if (valve.powerLevelReached()) {
                 /*  double valvePowerLevel = valve.setGoalPowerLevel().getNextValue().get() - valve.getPowerLevel().value().get();
                if (Math.abs(valvePowerLevel) > PERCENT_TOLERANCE_VALVE) {
                    valve.changeByPercentage(valvePowerLevel);
                 }*/
                } else {
                    valve.updatePowerLevel();
                }
            }
        });
        //TODO REWORK VALVEMANAGER!!!!
        //  i2cBridge.getMcpList().forEach(McpChannelRegister::shift);
    }

    private boolean maxMinValid(Valve valve) {
        double maximum = valve.maxValueChannel().getNextWriteValue().get();
        double minimum = valve.maxValueChannel().getNextWriteValue().get();
        return (maximum >= minimum && maximum > 0 && minimum >= 0);
    }


}
