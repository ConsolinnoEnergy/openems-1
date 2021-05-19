package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ManagedEvcs;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This provides a Limiter for the EVCS.
 * This Limiter Checks: -the Power per phase and then limits the power of the appropriate EVCS to prevent an unbalanced load or apply a Phase Limit
 *                      -the overall Power Consumption and limits it based on the config
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "evcsLimiterImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class evcsLimiterImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {
    private final Logger log = LoggerFactory.getLogger(evcsLimiterImpl.class);
    private ManagedEvcs[] evcss;
    private Integer powerL1;
    private Integer powerL2;
    private Integer powerL3;
    //The Maximum Power Consumptions
    private int max;
    private int max2;
    //The phases, the Maximum Consumption is on
    private int maxIndex;
    private int max2Index;
    private static final int GRID_VOLTAGE = 230;
    private static final int MAXIMUM_LOAD_DELTA = 20;
    private static final int ONE_PHASE_INDEX = 0;
    private static final int TWO_PHASE_INDEX = 1;
    private static final int ONE_PHASE_INDEX_2 = 2;
    private static final int TWO_PHASE_INDEX_2 = 3;
    private int phaseLimit;
    private int powerLimit;
    private boolean symmetry;
    @Reference
    ComponentManager cpm;

    public evcsLimiterImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        String[] ids = config.evcss();
        this.evcss = new ManagedEvcs[ids.length];
        this.symmetry = config.symmetry();
        this.phaseLimit = config.phaseLimit();
        this.powerLimit = config.powerLimit();
        for (int i = 0; i < ids.length; i++) {
            if (this.cpm.getComponent(ids[i]) instanceof ManagedEvcs) {
                this.evcss[i] = this.cpm.getComponent(ids[0]);
            } else {
                throw new ConfigurationException("The EvcssId list contains a wrong ID: ", ids[1] + " is not a evcs");
            }
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        String[] ids = config.evcss();
        this.evcss = new ManagedEvcs[ids.length];
        this.symmetry = config.symmetry();
        this.phaseLimit = config.phaseLimit();
        this.powerLimit = config.powerLimit();
        for (int i = 0; i < ids.length; i++) {
            if (this.cpm.getComponent(ids[i]) instanceof ManagedEvcs) {
                this.evcss[i] = this.cpm.getComponent(ids[0]);
            } else {
                throw new ConfigurationException("The EvcssId list contains a wrong ID: ", ids[1] + " is not a evcs");
            }
        }

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    @Override
    public void handleEvent(Event event) {
        Optional<ManagedEvcs[]> problem;
        problem = this.getRequestedPower();
        if (problem.isPresent()) {
            this.limitPower(problem.get());
        }
        if (this.phaseLimit != 0 && this.getMaximumLoad() > this.phaseLimit) {
            this.log.info("Phase Limit has been exceeded. Rectifying in Process...");
            this.applyPhaseLimit();
        }
        if (this.powerLimit != 0 && (this.powerL1 + this.powerL2 + this.powerL3 >= this.powerLimit)) {
            this.log.info("Power Limit has been exceeded. Rectifying in Process...");
            this.applyPowerLimit();
        }
    }

    private void applyPowerLimit() {

    }

    private void applyPhaseLimit() {

    }

    private void limitPower(ManagedEvcs[] problem) {


    }

    /**
     * Updates the current Power consumption and returns an Array of problematic EVCS Arrays if an unbalanced load exists.
     *
     * @return Array of ManagedEvcs[]
     */
    private Optional<ManagedEvcs[]> getRequestedPower() {
        //Updates current Power Consumption
        for (int i = 0; i < this.evcss.length; i++) {
            ManagedEvcs target = this.evcss[i];
            int[] phases = target.getPhaseConfiguration();
            int phaseCount = target.getPhases().get();
            for (int n = 0; n < phaseCount; n++) {
                switch (phases[n]) {
                    case 1:
                        this.powerL1 += (target.getChargePower().orElse(0) / GRID_VOLTAGE);
                        break;
                    case 2:
                        this.powerL2 += (target.getChargePower().orElse(0) / GRID_VOLTAGE);
                        break;
                    case 3:
                        this.powerL3 += (target.getChargePower().orElse(0) / GRID_VOLTAGE);
                        break;
                }
            }
        }

        //If the load should be checked this will calculate if there is a load Delta >= the maximum allowed Delta
        int min = this.getMinimumLoad();
        int max = this.getMaximumLoad();
        if (max - min >= MAXIMUM_LOAD_DELTA) {
            if (this.symmetry) {
                return this.unbalancedEvcsOnPhase();
            }
        }
        return Optional.empty();
    }

    /**
     * Creates Arrays of problematic EVCS and wraps them into another Array.
     *
     * @return Array (either 2 or 4 in length) of EVCS Arrays
     */
    private Optional<ManagedEvcs[]> unbalancedEvcsOnPhase() {
        if (this.max2Index != 0) {
            this.log.info("There exists an unbalanced load on Phases " + this.maxIndex + " and " + this.max2Index);
        } else {
            this.log.info("There exists an unbalanced load on Phase " + this.maxIndex);
        }
        ManagedEvcs[] onePhase = this.getOnePhaseEvcs(this.maxIndex);
        ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(this.maxIndex);
        List<ManagedEvcs[]> output = new ArrayList<>();
        output.add(ONE_PHASE_INDEX, onePhase);
        output.add(TWO_PHASE_INDEX, twoPhase);
        //If there is an unbalanced load on 2 Phases
        if (this.max2Index != 0) {
            ManagedEvcs[] onePhase2 = this.getOnePhaseEvcs(this.max2Index);
            ManagedEvcs[] twoPhase2 = this.getTwoPhaseEvcs(this.max2Index);
            output.add(ONE_PHASE_INDEX_2, onePhase2);
            output.add(TWO_PHASE_INDEX_2, twoPhase2);
        }
        return Optional.of((ManagedEvcs[]) output.toArray());

    }

    /**
     * Puts all EVCS that charge with one Phase on the unbalanced Phase.
     *
     * @param problemPhase Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getOnePhaseEvcs(int problemPhase) {
        List<ManagedEvcs> onePhase = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            if (this.evcss[i].getPhases().get() == 1 && Arrays.toString(this.evcss[i].getPhaseConfiguration()).contains(problemPhase + "")) {
                onePhase.add(this.evcss[i]);
            }
        }
        return (ManagedEvcs[]) onePhase.toArray();
    }

    /**
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase.
     *
     * @param problemPhase Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(int problemPhase) {
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            if (this.evcss[i].getPhases().get() == 2 && Arrays.toString(this.evcss[i].getPhaseConfiguration()).contains(problemPhase + "")) {
                twoPhase.add(this.evcss[i]);
            }
        }
        return (ManagedEvcs[]) twoPhase.toArray();
    }

    /**
     * Detects the Maximum Phase/s and stores the information in this Object ( max,max2,maxIndex,max2Index ).
     *
     * @return The highest Power Consumption of all Phases.
     */
    private int getMaximumLoad() {
        this.max = 0;
        this.maxIndex = 0;
        this.max2 = 0;
        this.max2Index = 0;
        int max = Math.max(Math.max(this.powerL1, this.powerL2), this.powerL3);
        if (max == this.powerL1) {
            this.max = this.powerL1;
            this.maxIndex = 1;
        }
        if (max == this.powerL2) {
            if (this.max == 0) {
                this.max = this.powerL2;
                this.maxIndex = 2;
            } else {
                this.max2 = this.powerL2;
                this.max2Index = 2;
            }
        }
        if (max == this.powerL3) {
            if (this.max == 0) {
                this.max = this.powerL3;
                this.maxIndex = 3;
            } else {
                this.max2 = this.powerL3;
                this.max2Index = 3;
            }
        }
        return this.max;
    }

    /**
     * Detects the minimum Power Consumption.
     * @return The lowest Power Consumption of all Phases.
     */
    private int getMinimumLoad() {
        int min = Math.min(Math.min(this.powerL1, this.powerL2), this.powerL3);
        if (min == this.powerL1) {
            return this.powerL1;
        } else if (min == this.powerL2) {
            return this.powerL2;
        } else {
            return this.powerL3;
        }
    }
}
