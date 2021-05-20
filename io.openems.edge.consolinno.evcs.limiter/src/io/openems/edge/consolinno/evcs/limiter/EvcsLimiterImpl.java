package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ManagedEvcs;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
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

public class EvcsLimiterImpl extends AbstractOpenemsComponent implements OpenemsComponent, PowerLimitChannel, EventHandler {
    private final Logger log = LoggerFactory.getLogger(EvcsLimiterImpl.class);
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

    public EvcsLimiterImpl() {
        super(OpenemsComponent.ChannelId.values(), PowerLimitChannel.ChannelId.values());
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
    public String debugLog() {
        return "EVCS Power: L1: " + this.powerL1 + " | L2: " + this.powerL2 + " | L3: " + this.powerL3;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.getPowerLimitValue() > 0) {
            this.powerLimit = getPowerLimitValue();
        }
        Optional<List<ManagedEvcs[]>> problem;
        problem = this.getRequestedPower();
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
            try {
                this.applyPhaseLimit();
            } catch (Exception e) {
                this.log.error("Unable to apply Phase Limit without turning an EVCS off!");
            }
        }
        this.updatePower(true);
        if (this.powerLimit != 0 && (this.powerL1 + this.powerL2 + this.powerL3 >= this.powerLimit / GRID_VOLTAGE)) {
            this.log.info("Power Limit has been exceeded. Rectifying in Process...");
            try {
                this.applyPowerLimit();
            } catch (Exception e) {
                this.log.error("Unable to apply Power Limit without turning an EVCS off!");
            }
        }
        this.updatePower(true);
    }
    //----------------------Limit Methods------------------------\\


    //-------------Methods for Power Limiting------------\\

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

    //---------------Methods for Phase Limiting--------------\\

    /**
     * Applies the Limit for the Phases, specified in the Config.
     *
     * @throws Exception If its unable to reduce the phases without Turning an EVCS off
     */
    private void applyPhaseLimit() throws Exception {
        //What phases are causing the problems
        List<Integer> problemPhases = new ArrayList<>();
        int afterOnePhaseReduction;
        int afterTwoPhaseReduction;
        int afterThreePhaseReduction;

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
        // One Phase and two phase are sorted by Phase
        List<ManagedEvcs[]> onePhase = new ArrayList<>();
        //0=All that are on phase 1 and 2;1= All that are on Phase 2 and Three;2= All that are on phase 3 and 1
        List<ManagedEvcs[]> twoPhase = new ArrayList<>();
        //Three phases are already on all of them so there is only one Array
        ManagedEvcs[] threePhase;
        threePhase = this.getThreePhaseEvcs();
        for (int i = 0; i < problemPhases.size(); i++) {
            onePhase.add(i, this.getOnePhaseEvcs(problemPhases.get(i)));
            if (i != 3) {
                twoPhase.add(i, this.getTwoPhaseEvcs(problemPhases.get(i), i - 1));
            }
        }

        //Get the Value that has to be reduced
        int reduceL1 = this.powerL1 - this.phaseLimit;
        int reduceL2 = this.powerL2 - this.phaseLimit;
        int reduceL3 = this.powerL3 - this.phaseLimit;
        int amountPerEvcs;
        int minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
        //Actual limiting
        int previousReduceAmount = minReduce;
        if (threePhase.length > 1) {
            while (this.threePhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {


                //The Three Phase EVCS will be reduced until at least one is under the limit
                afterThreePhaseReduction = this.reduceThreePhaseEvcs(threePhase, threePhase.length, minReduce);
                reduceL1 -= afterThreePhaseReduction;
                reduceL2 -= afterThreePhaseReduction;
                reduceL3 -= afterThreePhaseReduction;
                minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
                if (minReduce != previousReduceAmount) {
                    previousReduceAmount = minReduce;
                } else {
                    //if the three Phasers cant be reduced
                    throw new Exception();
                }
            }
        } else {
            int phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce);
            //reduce at least one phase
            while (minReduce > 0) {
                amountPerEvcs = minReduce / onePhase.get(phaseIndex - 1).length;
                minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex - 1), onePhase.get(phaseIndex - 1).length, amountPerEvcs, minReduce);

                if (minReduce != previousReduceAmount) {
                    previousReduceAmount = minReduce;
                } else {
                    throw new Exception();
                }
            }
        }
        //The Two Phase EVCS will be reduced until at least one is under the limit
        if (this.twoPhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {
            //Get what phase is ok
            int phaseOkIndex = this.getOnePhaseUnderPhaseLimit(reduceL1, reduceL2, reduceL3);
            int phaseIndex;
            switch (phaseOkIndex) {
                case 1:
                    minReduce = Math.min(reduceL2, reduceL3);
                    break;
                case 2:
                    minReduce = Math.min(reduceL1, reduceL3);
                    break;
                case 3:
                    minReduce = Math.min(reduceL1, reduceL2);
                    break;
            }
            ManagedEvcs[] twoPhaseOverLimit;
            if (phaseOkIndex == 3) {
                twoPhaseOverLimit = twoPhase.get(0);
            } else {
                twoPhaseOverLimit = twoPhase.get(phaseOkIndex);
            }
            if (twoPhaseOverLimit.length > 1) {
                //reduce until on phase is under the limit
                previousReduceAmount = minReduce;
                while (minReduce > 0) {
                    afterTwoPhaseReduction = this.reduceTwoPhaseEvcs(twoPhaseOverLimit, twoPhaseOverLimit.length, minReduce);
                    switch (phaseOkIndex) {
                        case 1:
                            reduceL2 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;
                            minReduce -= afterTwoPhaseReduction;
                            break;
                        case 2:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;
                            minReduce -= afterTwoPhaseReduction;
                            break;
                        case 3:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL2 -= afterTwoPhaseReduction;
                            minReduce -= afterTwoPhaseReduction;
                            break;
                    }
                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        throw new Exception();
                    }
                }
            } else {
                phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce);
                //reduce at least one phase
                while (minReduce > 0) {
                    amountPerEvcs = minReduce / onePhase.get(phaseIndex - 1).length;
                    minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex - 1), onePhase.get(phaseIndex - 1).length, amountPerEvcs, minReduce);

                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        throw new Exception();
                    }
                }
            }
        }

        //The one Phase EVCS will be reduces until the last phase is under the limit
        if (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

            int phaseIndex = 0;
            int problemOnePhase = 1;
            switch (this.getTwoPhasesUnderPhaseLimit(reduceL1, reduceL2, reduceL3)) {
                case 1:
                    amountPerEvcs = reduceL1 / onePhase.get(0).length;
                    afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(0), onePhase.get(0).length, amountPerEvcs, reduceL1);
                    reduceL1 -= afterOnePhaseReduction;
                    phaseIndex = 1;
                    problemOnePhase = reduceL1;
                    break;
                case 2:
                    amountPerEvcs = reduceL2 / onePhase.get(1).length;
                    afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(1), onePhase.get(1).length, amountPerEvcs, reduceL2);
                    reduceL2 -= afterOnePhaseReduction;
                    phaseIndex = 2;
                    problemOnePhase = reduceL2;
                    break;
                case 3:
                    amountPerEvcs = reduceL3 / onePhase.get(2).length;
                    afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(2), onePhase.get(2).length, amountPerEvcs, reduceL3);
                    reduceL3 -= afterOnePhaseReduction;
                    phaseIndex = 3;
                    problemOnePhase = reduceL3;
                    break;
            }
            if (problemOnePhase <= 0) {
                this.log.info("Successfully applied Phase limit");
            } else {
                previousReduceAmount = problemOnePhase;
                while (problemOnePhase > 0 && phaseIndex != 0) {
                    amountPerEvcs = problemOnePhase / onePhase.get(phaseIndex - 1).length;
                    problemOnePhase = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex - 1), onePhase.get(phaseIndex - 1).length, amountPerEvcs, problemOnePhase);

                    if (problemOnePhase != previousReduceAmount) {
                        previousReduceAmount = problemOnePhase;
                    } else {
                        throw new Exception();
                    }
                }
            }

        }
    }


    /**
     * Checks if only One Phase is over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean onePhaseOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0 && reduceL2 <= 0 && reduceL3 <= 0) {
            return true;
        } else if (reduceL1 <= 0 && reduceL2 > 0 && reduceL3 <= 0) {
            return true;
        } else {
            return reduceL1 <= 0 && reduceL2 <= 0 && reduceL3 > 0;
        }
    }

    /**
     * Returns the Phase that is the only one that is still over the Phase Limit.
     * NOTE: Only call after chack has been done that only one if over the limit in the first place.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return Index of the Phase that is over the Limit
     */
    private int getTwoPhasesUnderPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0 && reduceL2 <= 0 && reduceL3 <= 0) {
            return 1;
        } else if (reduceL1 <= 0 && reduceL2 > 0 && reduceL3 <= 0) {
            return 2;
        } else {
            return 3;
        }

    }

    /**
     * Checks if two Phases are over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean twoPhasesOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0) {
            if (reduceL2 > 0 && reduceL3 <= 0) {
                return true;
            } else {
                return reduceL2 <= 0 && reduceL3 > 0;
            }
        } else {
            if (reduceL2 <= 0) {
                return false;
            } else {
                return reduceL3 > 0;
            }
        }
    }

    /**
     * Returns the phase that is under the Phase Limit.
     * NOTE: Only call after check has been done that only one is under the limit in the first place.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return Index of the Phase that is under the Limit
     */
    private int getOnePhaseUnderPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 <= 0) {
            return 1;
        }
        if (reduceL2 <= 0) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * Checks if all Phases are over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean threePhasesOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        return (reduceL1 > 0 && reduceL2 > 0 && reduceL3 > 0);
    }

    //----------------Methods for Load Balancing-----------------\\

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
        if (amountLeft > 0 || (amountLeft2 > 0 && problem.size() > 2)) {
            int[] amountsLeft = new int[2];
            amountsLeft[0] = amountLeft;
            amountsLeft[1] = amountLeft2;
            int[] previousAmountsLeft = amountsLeft.clone();
            while (amountLeft > 0 || (amountLeft2 > 0 && problem.size() > 2)) {
                int amountToReduceTwoPhase = 0;
                if (amountLeft > 0) {
                    amountToReduceTwoPhase = amountLeft / twoPhaseLength;
                } else if (twoPhaseLength2 != 0) {
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

    //--------------------General Methods for Limiting-------------------\\

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
            int newPower = 0;
            if (onePhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(onePhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                newPower = ((onePhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(onePhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (onePhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
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
     * NOTE: This Method doesn't check if its allowed to do it. This Should only be used in the Phase Limitation.
     *
     * @param twoPhase       Array of all EVCS that have to be reduced
     * @param twoPhaseLength Length of that Array
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @return How much was reduced
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceTwoPhaseEvcs(ManagedEvcs[] twoPhase, int twoPhaseLength, int amountToReduce) throws OpenemsError.OpenemsNamedException {
        int amountReduced = 0;

        for (int i = 0; i < twoPhaseLength; i++) {
            int newPower = 0;
            if (twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                newPower = ((twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
            if (newPower > 6) {
                twoPhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountReduced += amountToReduce;
            }
        }
        return amountReduced;
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
                int newPower = 0;
                if (twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                    newPower = ((twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
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
                int newPower = 0;
                if (twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                    newPower = ((twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
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
     * Reduces the Power of EVCS that charge with two Phases by a amount given.
     *
     * @param threePhase       Array of all EVCS that have to be reduced
     * @param threePhaseLength Length of that Array
     * @param amountToReduce   Amount that has to be reduced per EVCS
     * @return modified amountsLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceThreePhaseEvcs(ManagedEvcs[] threePhase, int threePhaseLength, int amountToReduce) throws OpenemsError.OpenemsNamedException {
        int amountReduced = 0;
        for (int i = 0; i < threePhaseLength; i++) {
            int newPower = 0;
            if (threePhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(threePhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                newPower = ((threePhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(threePhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (threePhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
            if (newPower > 6) {
                threePhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountReduced += amountToReduce;
            }
        }
        return amountReduced;
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
     * Puts all EVCS that charge with one Phase on the unbalanced Phase in an Array.
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
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
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
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
     * This is an Expansion of the above Method.
     *
     * @param problemPhase  Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @param excludedPhase The Phase that should not be one of the two phases.
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(int problemPhase, int excludedPhase) {
        if (excludedPhase < 1) {
            excludedPhase = 3;
        }
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            int[] phaseConfiguration = this.evcss[i].getPhaseConfiguration();
            if (this.evcss[i].getPhases().get() == 2 && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)
                    && (phaseConfiguration[0] != excludedPhase || phaseConfiguration[1] != excludedPhase)) {
                twoPhase.add(this.evcss[i]);
            }
        }
        return (ManagedEvcs[]) twoPhase.toArray();
    }


    /**
     * Puts all EVCS that charge with three Phases in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getThreePhaseEvcs() {
        List<ManagedEvcs> threePhase = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            if (this.evcss[i].getPhases().get() == 3) {
                threePhase.add(this.evcss[i]);
            }
        }
        return (ManagedEvcs[]) threePhase.toArray();
    }


    //---------------------General Methods-----------------------\\

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
     * Returns the index of the Phase the current Power value is on.
     *
     * @param phasePower1 L1
     * @param phasePower2 L2
     * @param phasePower3 L3
     * @param power       Power on a phase
     * @return Index or 0 if the power is not on any phase
     */
    private int getPhaseByPower(int phasePower1, int phasePower2, int phasePower3, int power) {
        if (phasePower1 == power) {
            return 1;
        } else if (phasePower2 == power) {
            return 2;
        } else if (phasePower3 == power) {
            return 3;
        }
        return 0;
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
                int phaseCount = target.getPhases().orElse(0);
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
                int phaseCount = target.getPhases().orElse(0);
                for (int n = 0; n < phaseCount; n++) {
                    if (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                        switch (phases[n]) {
                            case 1:
                                this.powerL1 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE);
                                break;
                            case 2:
                                this.powerL2 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE);
                                break;
                            case 3:
                                this.powerL3 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE);
                                break;
                        }
                    } else {
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
            }

        }
    }
}
