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
import java.util.List;
import java.util.Optional;

/**
 * This provides a Limiter for the EVCS.
 * This Limiter Checks: 1: the Power per phase and then limits the power of the appropriate EVCS to prevent an unbalanced load or apply a Phase Limit
 * 2:the overall Power Consumption and limits it based on the config
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "evcsLimiterImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class evcsLimiterImpl extends AbstractOpenemsComponent implements OpenemsComponent, powerLimitChannel, EventHandler {
    private final Logger log = LoggerFactory.getLogger(evcsLimiterImpl.class);
    private ManagedEvcs[] evcss;
    private Integer powerL1;
    private Integer powerL2;
    private Integer powerL3;
    //The Maximum Power Consumptions
    private int max;
    //The phases where the maximal Consumption is on
    private int maxIndex;
    private int max2Index;
    //The Minimum Power Consumption
    private int min;
    //The phases with the minimal Consumption
    private int minIndex;
    private int min2Index;
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
        super(OpenemsComponent.ChannelId.values(), powerLimitChannel.ChannelId.values());
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
        Optional<List<ManagedEvcs[]>> problem;
        problem = this.getRequestedPower();
        if (this.getPowerLimitValue() > 0) {
            this.powerLimit = getPowerLimitValue();
        }
        if (problem.isPresent()) {
            try {
                this.limitPower(problem.get());

            } catch (Exception e) {
                this.log.error("Unable to Limit Power!");
            }
        }
        this.updatePower(true);
        if (this.phaseLimit != 0 && this.getMaximumLoad() > this.phaseLimit) {
            this.log.info("Phase Limit has been exceeded. Rectifying in Process...");
            this.applyPhaseLimit();
        }
        this.updatePower(true);
        if (this.powerLimit != 0 && (this.powerL1 + this.powerL2 + this.powerL3 >= this.powerLimit / GRID_VOLTAGE)) {
            this.log.info("Power Limit has been exceeded. Rectifying in Process...");
            try {
                this.applyPowerLimit();
            } catch (Exception e) {
                this.log.error("Unable to apply Power Limit!");
            }
        }
    }

    /**
     * Evenly Reduces the power of all EVCS (if Possible).
     *
     * @throws Exception This should not happen
     */
    private void applyPowerLimit() throws Exception {
        int powerToReduce = ((this.powerL1 + this.powerL2 + this.powerL3) - (this.powerLimit / GRID_VOLTAGE)) + 1;
        int powerPerEvcs = powerToReduce / this.evcss.length;
        for (int i = 0; i < this.evcss.length; i++) {
            int newPower = (this.evcss[i].getChargePower().get() / GRID_VOLTAGE) - powerPerEvcs;
            if (newPower > 6) {
                this.evcss[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                powerToReduce -= powerPerEvcs;
                this.log.info(this.evcss[i].id() + " was reduced by " + powerPerEvcs * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        int previousPowerToReduce = powerToReduce;
        while (powerToReduce > 0) {

            powerToReduce = this.applyPowerLimit(powerToReduce);
            if (powerToReduce != previousPowerToReduce) {
                previousPowerToReduce = powerToReduce;
            } else {
                throw new Exception();
            }
        }

    }

    /**
     * This is a recursive Helper method of the above method. Should only be called from it and not externally!
     *
     * @param powerToReduce The remaining power that has to be reduced
     * @return modified PowerToReduce
     * @throws OpenemsError.OpenemsNamedException This should not happen
     */
    private int applyPowerLimit(int powerToReduce) throws OpenemsError.OpenemsNamedException {
        int powerPerEvcs = powerToReduce / this.evcss.length;
        for (int i = 0; i < this.evcss.length; i++) {
            int newPower = (this.evcss[i].getChargePower().get() / GRID_VOLTAGE) - powerPerEvcs;
            if (newPower > 6) {
                this.evcss[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                powerToReduce -= powerPerEvcs;
                this.log.info(this.evcss[i].id() + " was reduced by " + powerPerEvcs * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        return powerToReduce;
    }


    private void applyPhaseLimit() {
        //What phases are causing the problems
        List<Integer> problemPhases = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            switch (i) {
                case 1:
                    if (this.powerL1 > this.powerLimit) {
                        problemPhases.add(i);
                    }
                    break;
                case 2:
                    if (this.powerL2 > this.powerLimit) {
                        problemPhases.add(i);
                    }
                    break;
                case 3:
                    if (this.powerL3 > this.powerLimit) {
                        problemPhases.add(i);
                    }
                    break;
            }
        }
        //Get all evcs on that phase
        List<ManagedEvcs> onePhase = new ArrayList<>();
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        List<ManagedEvcs> threePhase = new ArrayList<>();
        //Get the Value that has to be reduced
        //Actual limiting
    }

    private void applyPhaseLimit(int powerToReduce) {

    }

    /**
     * Balances the Power if symmetry was enabled.
     *
     * @param problem A list of all EVCS that have to be limited
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private void limitPower(List<ManagedEvcs[]> problem) throws Exception {

        int powerDelta = this.max - this.min;
        //The Power that have to be reduced to create balance
        int amountLeft = powerDelta - MAXIMUM_LOAD_DELTA + 1;
        int amountLeft2 = powerDelta - MAXIMUM_LOAD_DELTA + 1;
        ManagedEvcs[] onePhase = problem.get(ONE_PHASE_INDEX);
        ManagedEvcs[] onePhase2;
        int onePhaseLength2;
        int onePhaseLength = onePhase.length;
        int amountToReduceOnePhase = amountLeft / onePhaseLength;
        ManagedEvcs[] twoPhase = problem.get(TWO_PHASE_INDEX);
        int twoPhaseLength = twoPhase.length;
        ManagedEvcs[] twoPhase2;
        int twoPhaseLength2 = 0;
        amountLeft = this.reduceOnePhaseEvcs(onePhase, onePhaseLength, amountToReduceOnePhase, amountLeft);
        if (amountLeft <= 0) {
            this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
        } else {
            int amountToReduceTwoPhase = amountLeft / twoPhaseLength;
            int[] amountsLeft = new int[2];
            amountsLeft[0] = amountLeft;
            amountsLeft[1] = amountLeft2;
            amountsLeft = this.reduceTwoPhaseEvcs(twoPhase, twoPhaseLength, amountToReduceTwoPhase, amountsLeft);
            amountLeft = amountsLeft[0];
            amountLeft2 = amountsLeft[1];
            if (amountLeft <= 0) {
                this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");

            } else {
                //If after reducing the one and two phase EVCS was not enough, this will reduce the one phase EVCS until its impossible to do it anymore
                int previousAmountLeft = amountLeft;
                while (amountLeft > 0) {
                    amountToReduceOnePhase = amountLeft / onePhaseLength;
                    amountLeft = this.reduceOnePhaseEvcs(onePhase, onePhaseLength, amountToReduceOnePhase, amountLeft);
                    if (amountLeft != previousAmountLeft) {
                        previousAmountLeft = amountLeft;
                    } else {
                        break;
                    }
                }
                if (amountLeft <= 0) {
                    this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
                }

            }
        }
        //If two phases are unbalanced
        if (problem.size() > 2) {
            onePhase2 = problem.get(ONE_PHASE_INDEX_2);
            onePhaseLength2 = onePhase2.length;
            twoPhase2 = problem.get(TWO_PHASE_INDEX_2);
            twoPhaseLength2 = twoPhase2.length;
            if (amountLeft2 > 0) {
                int amountToReduce2 = amountLeft2 / onePhaseLength2;

                amountLeft2 = this.reduceOnePhaseEvcs(onePhase2, onePhaseLength2, amountToReduce2, amountLeft2);
            }
            if (amountLeft2 <= 0) {
                this.log.info("Phase " + this.max2Index + " has been successfully Balanced.");

            } else {
                //If after reducing the one and two phase EVCS was not enough, this will reduce the one phase EVCS until its impossible to do it anymore
                int previousAmountLeft2 = amountLeft2;
                while (amountLeft2 > 0) {
                    int amountToReduce2 = amountLeft2 / onePhaseLength2;
                    amountLeft2 = this.reduceOnePhaseEvcs(onePhase2, onePhaseLength2, amountToReduce2, amountLeft2);
                    if (amountLeft2 != previousAmountLeft2) {
                        previousAmountLeft2 = amountLeft;
                    } else {
                        break;
                    }
                }
                if (amountLeft2 <= 0) {
                    this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
                }
            }
        }
        //If its still unbalanced this will reduce the two phase evcs until its not possible anymore
        if (amountLeft > 0 || amountLeft2 > 0) {
            int[] amountsLeft = new int[2];
            amountsLeft[0] = amountLeft;
            amountsLeft[1] = amountLeft2;
            int[] previousAmountsLeft = amountsLeft.clone();
            while (amountLeft > 0 || amountLeft2 > 0) {
                int amountToReduceTwoPhase = 0;
                if (amountLeft > 0) {
                    amountToReduceTwoPhase = amountLeft / twoPhaseLength;
                } else if (problem.size() > 2 && twoPhaseLength2 != 0) {
                    amountToReduceTwoPhase = amountLeft2 / twoPhaseLength2;
                }
                if (amountToReduceTwoPhase != 0) {
                    amountsLeft = this.reduceTwoPhaseEvcs(twoPhase, twoPhaseLength, amountToReduceTwoPhase, amountsLeft);
                    amountLeft = amountsLeft[0];
                    amountLeft2 = amountsLeft[1];
                }
                if (amountLeft != previousAmountsLeft[0] || amountLeft2 != previousAmountsLeft[1]) {
                    previousAmountsLeft = amountsLeft.clone();
                } else {
                    this.log.error("Phases can not be balanced!");
                    throw new Exception();
                }
            }
        }
        this.log.info("Balance has been successfully restored.");

    }

    /**
     * Reduces the Power of EVCS that charge with only one Phase by a amount given.
     *
     * @param onePhase       Array of all EVCS that have to be reduced
     * @param onePhaseLength Length of that Array
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @param amountLeft     The Sum of what has to be reduced by all EVCS
     * @return modified amountLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceOnePhaseEvcs(ManagedEvcs[] onePhase, int onePhaseLength, int amountToReduce, int amountLeft) throws OpenemsError.OpenemsNamedException {
        for (int i = 0; i < onePhaseLength; i++) {
            int newPower = (onePhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            if (newPower > 6) {
                onePhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountLeft -= amountToReduce;
                this.log.info(onePhase[i].id() + " was reduced by " + amountToReduce * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        return amountLeft;
    }

    /**
     * Reduces the Power of EVCS that charge with two Phases by a amount given.
     *
     * @param twoPhase       Array of all EVCS that have to be reduced
     * @param twoPhaseLength Length of that Array
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @param amountsLeft    A tuple of the Sums of what has to be reduced by all EVCS
     * @return modified amountsLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int[] reduceTwoPhaseEvcs(ManagedEvcs[] twoPhase, int twoPhaseLength, int amountToReduce, int[] amountsLeft) throws OpenemsError.OpenemsNamedException {
        for (int i = 0; i < twoPhaseLength; i++) {
            int[] phaseConfiguration = twoPhase[i].getPhaseConfiguration();
            if (this.min2Index == 0 && (phaseConfiguration[0] != this.minIndex && phaseConfiguration[1] != this.minIndex)) {
                int newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                if (newPower > 6) {
                    twoPhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.max2Index || phaseConfiguration[1] == this.max2Index) {
                        amountsLeft[1] -= amountToReduce;
                    }
                    this.log.info(twoPhase[i].id() + " was reduced by " + amountToReduce * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
                }
                //If there exists an unbalanced load and the other two phases are both the minimum
            } else if (this.min2Index != 0) {
                int newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                if (newPower > 6) {
                    twoPhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.max2Index || phaseConfiguration[1] == this.max2Index) {
                        amountsLeft[1] -= amountToReduce;
                    }
                    this.log.info(twoPhase[i].id() + " was reduced by " + amountToReduce * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
                }
            }
        }
        return amountsLeft;
    }

    /**
     * Updates the current Power consumption and returns an Array of problematic EVCS Arrays if an unbalanced load exists.
     *
     * @return Array of ManagedEvcs[]
     */
    private Optional<List<ManagedEvcs[]>> getRequestedPower() {
        this.updatePower(false);
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
    private Optional<List<ManagedEvcs[]>> unbalancedEvcsOnPhase() {
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
        return Optional.of(output);

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
            int[] phaseConfiguration = this.evcss[i].getPhaseConfiguration();
            if (this.evcss[i].getPhases().get() == 1 && phaseConfiguration[0] == problemPhase) {
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
            int[] phaseConfiguration = this.evcss[i].getPhaseConfiguration();
            if (this.evcss[i].getPhases().get() == 2 && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)) {
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
                this.max2Index = 2;
            }
        }
        if (max == this.powerL3) {
            if (this.max == 0) {
                this.max = this.powerL3;
                this.maxIndex = 3;
            } else {
                this.max2Index = 3;
            }
        }
        return this.max;
    }

    /**
     * Detects the minimum Power Consumption.
     *
     * @return The lowest Power Consumption of all Phases.
     */
    private int getMinimumLoad() {
        this.min = 0;
        this.minIndex = 0;
        this.min2Index = 0;
        int min = Math.min(Math.min(this.powerL1, this.powerL2), this.powerL3);
        if (min == this.powerL1) {
            this.min = this.powerL1;
            this.minIndex = 1;
        }
        if (min == this.powerL2) {
            if (this.min == 0) {
                this.min = this.powerL2;
                this.minIndex = 2;
            } else {
                this.min2Index = 2;
            }
        }
        if (min == this.powerL3) {
            if (this.min == 0) {
                this.min = this.powerL3;
                this.minIndex = 3;
            } else {
                this.min2Index = 3;
            }
        }
        return this.min;
    }

    /**
     * Updated the Stored Power Values of the Phases.
     *
     * @param tempered Was the power already changed in this cycle in some way?
     */
    private void updatePower(boolean tempered) {

        this.powerL1 = 0;
        this.powerL2 = 0;
        this.powerL3 = 0;
        //Updates current Power Consumption
        if (!tempered) {
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
        } else {
            for (int i = 0; i < this.evcss.length; i++) {
                ManagedEvcs target = this.evcss[i];
                int[] phases = target.getPhaseConfiguration();
                int phaseCount = target.getPhases().get();
                for (int n = 0; n < phaseCount; n++) {
                    switch (phases[n]) {
                        case 1:
                            this.powerL1 += (target.getSetChargePowerLimit().orElse(0) / GRID_VOLTAGE);
                            break;
                        case 2:
                            this.powerL2 += (target.getSetChargePowerLimit().orElse(0) / GRID_VOLTAGE);
                            break;
                        case 3:
                            this.powerL3 += (target.getSetChargePowerLimit().orElse(0) / GRID_VOLTAGE);
                            break;
                    }
                }
            }

        }
    }
}
