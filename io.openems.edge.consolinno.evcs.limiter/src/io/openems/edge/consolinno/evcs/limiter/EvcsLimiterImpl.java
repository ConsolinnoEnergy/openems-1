package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ManagedEvcs;
import org.joda.time.DateTime;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private String[] ids;
    private ManagedEvcs[] evcss;
    private Integer powerL1;
    private Integer powerL2;
    private Integer powerL3;
    //Id and the Last known Power Request of an EVCS that was turned off.
    private Map<String, Integer> powerWaitingList;
    //Id and the time the EVCS was turned off.
    private Map<String, Integer> timeStamps;
    //The Maximum Power Consumptions
    private int max;
    //The phases where the maximal Consumption is on
    private int maxIndex;
    //The middle Load. Can be the same as max
    private int middle;
    //The phase where the middle Consumption is on
    private int middleIndex;
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
    private int offTime;
    private boolean symmetry;

    @Reference
    ComponentManager cpm;


    public EvcsLimiterImpl() {
        super(OpenemsComponent.ChannelId.values(), PowerLimitChannel.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.ids = config.evcss();
        this.evcss = new ManagedEvcs[this.ids.length];
        this.symmetry = config.symmetry();
        this.phaseLimit = config.phaseLimit();
        this.powerLimit = config.powerLimit();
        this.offTime = config.offTime();
        this.updateEvcss();


    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        String[] ids = config.evcss();
        this.evcss = new ManagedEvcs[ids.length];
        this.symmetry = config.symmetry();
        this.phaseLimit = config.phaseLimit();
        this.powerLimit = config.powerLimit();
        this.offTime = config.offTime();
        this.updateEvcss();

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        return "L1: " + this.powerL1 + " | L2: " + this.powerL2 + " | L3: " + this.powerL3 + " | on "
                + this.evcss.length + " EVCS: " + Arrays.toString(this.ids);
    }

    @Override
    public void handleEvent(Event event) {
        if (this.evcss[0] == null) {
            this.updateEvcss();
        } else {
            if (this.getPowerLimitValue() > 0) {
                this.powerLimit = getPowerLimitValue();
            }
            Optional<List<ManagedEvcs[]>> problem;
            problem = this.getRequestedPower();
            if (problem.isPresent()) {
                try {
                    this.limitPower(problem.get());

                } catch (Exception e) {
                    this.log.warn("Unable to Limit Power without tunring an EVCS off! One or more EVCS will now be turned off for " + this.offTime + " minutes.");
                    try {
                        this.turnOffEvcsBalance();
                    } catch (Exception emergencyStop) {
                        this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                        this.emergencyStop();
                    }
                }
            }
            this.updatePower(true);
            if (this.phaseLimit != 0 && this.getMaximumLoad() > (this.phaseLimit / GRID_VOLTAGE)) {
                this.log.info("Phase Limit has been exceeded. Rectifying in Process...");
                try {
                    this.applyPhaseLimit();
                } catch (Exception e) {
                    this.log.warn("Unable to apply Phase Limit without turning an EVCS off! One or more EVCS will now be turned off for " + this.offTime + " minutes.");
                    try {
                        this.turnOffEvcsPhaseLimit();
                    } catch (Exception emergencyStop) {
                        this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                        this.emergencyStop();
                    }
                }
            }
            this.updatePower(true);
            if (this.powerLimit != 0 && (this.powerL1 + this.powerL2 + this.powerL3 >= this.powerLimit / GRID_VOLTAGE)) {
                this.log.info("Power Limit has been exceeded. Rectifying in Process...");
                try {
                    this.applyPowerLimit();
                } catch (Exception e) {
                    this.log.warn("Unable to apply Power Limit without turning an EVCS off! One or more EVCS will now be turned off for " + this.offTime + " minutes.");
                    try {
                        this.turnOffEvcsPowerLimit();
                    } catch (Exception emergencyStop) {
                        this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                        this.emergencyStop();
                    }
                }
            }
            this.updatePower(true);
        }
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
        if (powerToReduce == 0) {
            powerToReduce = 1;
        }
        int powerPerEvcs = powerToReduce / this.evcss.length;
        if (powerPerEvcs == 0) {
            powerPerEvcs = 1;
        }
        int newPower;
        for (int i = 0; i < this.evcss.length; i++) {
            if (this.evcss[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(this.evcss[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                newPower = ((this.evcss[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(this.evcss[i].getSetChargePowerLimitChannel().value().orElse(0))) / GRID_VOLTAGE) - powerPerEvcs;
            } else {
                newPower = (this.evcss[i].getChargePower().get() / GRID_VOLTAGE) - powerPerEvcs;
            }
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
     * Priority List:
     * 1. All Three Phases are over the Limit:
     * 1.1 Three Phasers
     * 1.2. Two Phasers IF Three Phasers don't exist / not enough to reduce at least one Phase
     * 1.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 2. Two phases are over the Limit:
     * 2.1 Two Phasers
     * 2.2 Three Phasers IF no Two Phasers exist / not enough to reduce at least one Phase
     * 2.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 3. One Phases is over the Limit:
     * 3.1 One Phasers
     * 3.2 Three Phasers IF no One Phasers exist / not enough to reduce at least one Phase
     * 3.3 Two Phasers IF neither Three Phasers nor One Phasers exist / not enough to reduce at least one Phase
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
                    if (this.powerL1 > (this.phaseLimit / GRID_VOLTAGE)) {
                        problemPhases.add(i);
                    }
                    break;
                case 2:
                    if (this.powerL2 > (this.phaseLimit / GRID_VOLTAGE)) {
                        problemPhases.add(i);
                    }
                    break;
                case 3:
                    if (this.powerL3 > (this.phaseLimit / GRID_VOLTAGE)) {
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
        int reduceL1 = this.powerL1 - (this.phaseLimit / GRID_VOLTAGE);
        int reduceL2 = this.powerL2 - (this.phaseLimit / GRID_VOLTAGE);
        int reduceL3 = this.powerL3 - (this.phaseLimit / GRID_VOLTAGE);
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
            int phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce) - 1;
            //reduce at least one phase
            if (onePhase.size() > 0) {
                while (minReduce > 0) {
                    amountPerEvcs = minReduce / onePhase.get(phaseIndex).length;
                    minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex), onePhase.get(phaseIndex).length, amountPerEvcs, minReduce);

                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        throw new Exception();
                    }
                }
            } else {
                while (minReduce > 0) {
                    int phaseIndex2;
                    if (phaseIndex == 0) {
                        phaseIndex2 = 3;
                    } else {
                        phaseIndex2 = phaseIndex - 1;
                    }
                    int twoPhaseAmount = (twoPhase.get(phaseIndex)).length + twoPhase.get(phaseIndex2).length;
                    amountPerEvcs = minReduce / twoPhaseAmount;
                    int reducedByPhase1 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex), twoPhase.get(phaseIndex).length, amountPerEvcs);
                    int reducedByPhase2 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex2), twoPhase.get(phaseIndex2).length, amountPerEvcs);
                    minReduce -= (reducedByPhase1 + reducedByPhase2) / 2;
                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        throw new Exception();
                    }
                }
            }
        }
        //The Two Phase EVCS will be reduced until at least one is under the limit
        if (this.twoPhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {
            //Get what phase is ok
            int phaseOkIndex = this.getOnePhaseUnderPhaseLimit(reduceL1, reduceL2);
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
            if (twoPhaseOverLimit.length > 0) {
                //reduce until on phase is under the limit
                previousReduceAmount = minReduce;
                while (minReduce > 0) {
                    afterTwoPhaseReduction = this.reduceTwoPhaseEvcs(twoPhaseOverLimit, twoPhaseOverLimit.length, minReduce);
                    switch (phaseOkIndex) {
                        case 1:
                            reduceL2 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;

                            break;
                        case 2:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;

                            break;
                        case 3:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL2 -= afterTwoPhaseReduction;

                            break;
                    }

                    minReduce -= afterTwoPhaseReduction / 2;

                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        if (afterTwoPhaseReduction / 2 == 0) {
                            break;
                        } else {
                            throw new Exception();
                        }
                    }
                }
            } else {
                phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce) - 1;
                //reduce at least one phase
                if (onePhase.size() > 0) {
                    while (minReduce > 0) {
                        amountPerEvcs = minReduce / onePhase.get(phaseIndex).length;
                        minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex), onePhase.get(phaseIndex).length, amountPerEvcs, minReduce);

                        if (minReduce != previousReduceAmount) {
                            previousReduceAmount = minReduce;
                        } else {
                            throw new Exception();
                        }
                    }
                } else {
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
                }
            }
        }

        //The one Phase EVCS will be reduces until the last phase is under the limit
        if (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

            int phaseIndex = 0;
            int problemOnePhase = 1;
            if (onePhase.size() > 0) {
                switch (this.getTwoPhasesUnderPhaseLimit(reduceL1, reduceL2, reduceL3)) {
                    case 1:
                        amountPerEvcs = reduceL1 / onePhase.get(0).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(0), onePhase.get(0).length, amountPerEvcs, reduceL1);
                        reduceL1 = afterOnePhaseReduction;
                        phaseIndex = 0;
                        problemOnePhase = reduceL1;
                        break;
                    case 2:
                        amountPerEvcs = reduceL2 / onePhase.get(1).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(1), onePhase.get(1).length, amountPerEvcs, reduceL2);
                        reduceL2 = afterOnePhaseReduction;
                        phaseIndex = 1;
                        problemOnePhase = reduceL2;
                        break;
                    case 3:
                        amountPerEvcs = reduceL3 / onePhase.get(2).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(2), onePhase.get(2).length, amountPerEvcs, reduceL3);
                        reduceL3 = afterOnePhaseReduction;
                        phaseIndex = 2;
                        problemOnePhase = reduceL3;
                        break;
                }
                if (problemOnePhase <= 0) {
                    this.log.info("Successfully applied Phase limit");
                } else {
                    previousReduceAmount = problemOnePhase;
                    while (problemOnePhase > 0) {
                        amountPerEvcs = problemOnePhase / onePhase.get(phaseIndex).length;
                        problemOnePhase = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex), onePhase.get(phaseIndex).length, amountPerEvcs, problemOnePhase);

                        if (problemOnePhase != previousReduceAmount) {
                            previousReduceAmount = problemOnePhase;
                        } else {
                            throw new Exception();
                        }
                    }
                }

            }
        } else {
            if (threePhase.length > 1) {
                while (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {


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
            } else if (twoPhase.size() > 1) {
                while (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {
                    int phaseIndex = this.getTwoPhasesUnderPhaseLimit(reduceL1, reduceL2, reduceL3);
                    int phaseIndex2;
                    if (phaseIndex == 0) {
                        phaseIndex2 = 3;
                    } else {
                        phaseIndex2 = phaseIndex - 1;
                    }
                    int twoPhaseAmount = (twoPhase.get(phaseIndex)).length + twoPhase.get(phaseIndex2).length;
                    amountPerEvcs = minReduce / twoPhaseAmount;
                    int reducedByPhase1 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex), twoPhase.get(phaseIndex).length, amountPerEvcs);
                    int reducedByPhase2 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex2), twoPhase.get(phaseIndex2).length, amountPerEvcs);
                    minReduce -= (reducedByPhase1 + reducedByPhase2) / 2;
                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        if ((reducedByPhase1 + reducedByPhase2) / 2 == 0) {
                            break;
                        } else {
                            throw new Exception();
                        }
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
     * NOTE: Only call after check has been done that only one if over the limit in the first place.
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
     * @return Index of the Phase that is under the Limit
     */
    private int getOnePhaseUnderPhaseLimit(int reduceL1, int reduceL2) {
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
        int middle = this.getMiddleLoad();
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
        int powerDelta2 = this.middle - this.min;
        //The Power that have to be reduced to create balance
        int amountLeft = powerDelta - MAXIMUM_LOAD_DELTA + 1;
        int amountLeft2 = powerDelta2 - MAXIMUM_LOAD_DELTA + 1;
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
                this.log.info("Phase " + this.middleIndex + " has been successfully Balanced.");

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
            int newPower;
            if (amountLeft == 1 && amountToReduce == 0) {
                amountToReduce = 1;
            }
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
        if (amountToReduce == 0) {
            amountToReduce = 1;
        }
        for (int i = 0; i < twoPhaseLength; i++) {
            int newPower;
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
            if ((amountsLeft[0] == 1 || amountsLeft[1] == 1) && amountToReduce == 0) {
                amountToReduce = 1;
            }
            int[] phaseConfiguration = twoPhase[i].getPhaseConfiguration();
            if (this.min2Index == 0 && (phaseConfiguration[0] != this.minIndex && phaseConfiguration[1] != this.minIndex)) {
                int newPower;
                if (twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                    newPower = ((twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
                if (newPower > 6) {
                    twoPhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.middleIndex || phaseConfiguration[1] == this.middleIndex) {
                        amountsLeft[1] -= amountToReduce;
                    }
                    this.log.info(twoPhase[i].id() + " was reduced by " + amountToReduce * GRID_VOLTAGE + " W and is now at " + newPower * GRID_VOLTAGE + " W");
                }
                //If there exists an unbalanced load and the other two phases are both the minimum
            } else if (this.min2Index != 0) {
                int newPower;
                if (twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                    newPower = ((twoPhase[i].getSetChargePowerLimitChannel().getNextWriteValue().orElse(twoPhase[i].getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (twoPhase[i].getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
                if (newPower > 6) {
                    twoPhase[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.middleIndex || phaseConfiguration[1] == this.middleIndex) {
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
        if (amountToReduce == 0) {
            amountToReduce = 1;
        }
        for (int i = 0; i < threePhaseLength; i++) {
            int newPower;
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


    //-------------------Power Off Methods--------------------\\

    //-----------Power Off for Balancing--------\\

    /**
     * Turns off EVCS for the Phase Balancing.
     * Priority List:
     * 1. Two Phase EVCS that charge on two unbalanced Phases
     * 2. One Phase EVCS
     * 3. Two Phase EVCS
     */
    private void turnOffEvcsBalance() throws Exception {
        this.updatePower(true);

        int max = this.getMaximumLoad();
        int middle = this.getMiddleLoad();
        int min = this.getMinimumLoad();

        Optional<List<ManagedEvcs[]>> optionalProblem = this.unbalancedEvcsOnPhase();
        if (optionalProblem.isPresent()) {
            List<ManagedEvcs[]> problem = optionalProblem.get();
            ManagedEvcs[] onePhase = problem.get(ONE_PHASE_INDEX);
            ManagedEvcs[] twoPhase = problem.get(TWO_PHASE_INDEX);
            ManagedEvcs[] onePhase2 = null;
            ManagedEvcs[] twoPhase2 = null;
            if (problem.size() > 2) {
                onePhase2 = problem.get(ONE_PHASE_INDEX_2);
                twoPhase2 = problem.get(TWO_PHASE_INDEX_2);
            }

            //--------Handle all Two phase EVCS that charge with both problem Phases--------\\
            if (middle - min > MAXIMUM_LOAD_DELTA && twoPhase.length > 0) {
                if (!this.turnOffTwoPhaseDoubleHitEvcsBalancing(min, twoPhase)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            twoPhase = this.removeEvcsFromArray(twoPhase);
            if (twoPhase2 != null) {
                twoPhase2 = this.removeEvcsFromArray(twoPhase2);
            }

            //----------------Handle the One phase EVCS on one Phase------------------\\
            if (max - min > MAXIMUM_LOAD_DELTA && onePhase.length > 0) {
                if (!this.turnOffOnePhaseEvcsBalancing(min, onePhase, false)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            onePhase = this.removeEvcsFromArray(onePhase);

            //-------Handle the One phase EVCS on the other Phase (if necessary)--------\\
            if (middle - min > MAXIMUM_LOAD_DELTA && onePhase2 != null) {
                if (!this.turnOffOnePhaseEvcsBalancing(min, onePhase2, true)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            if (onePhase2 != null) {
                onePhase2 = this.removeEvcsFromArray(onePhase2);
            }

            //------Handle the Two Phase EVCS on the Max Phase (if necessary)--------\\
            if (max - min > MAXIMUM_LOAD_DELTA && twoPhase.length > 0) {
                if (!this.turnOffTwoPhaseEvcsBalancing(min, twoPhase)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            twoPhase = this.removeEvcsFromArray(twoPhase);


            //------Handle the Two Phase EVCS on the Middle Phase (if necessary)--------\\
            if (middle - min > MAXIMUM_LOAD_DELTA && twoPhase2 != null) {
                if (!this.turnOffTwoPhaseEvcsBalancing(min, twoPhase2)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            twoPhase = this.removeEvcsFromArray(twoPhase);

            if (max - min > MAXIMUM_LOAD_DELTA) {
                throw new Exception("Its impossible to balance the Phases. This should not have happened.");
            }

        } else {
            this.log.info("Phases already Balanced. This should not have happened.");
        }
    }

    /**
     * Turns off one phase EVCS that are the Unbalanced Phase until its either balanced or no EVCS are left.
     *
     * @param minimum  Lowest Power Consumption
     * @param onePhase All one phase EVCS that charge on the appropriate Phase
     * @param phase2   true if this is not on the Max Phase but on Middle
     * @return true if the Phases are now balanced
     */
    private boolean turnOffOnePhaseEvcsBalancing(int minimum, ManagedEvcs[] onePhase, boolean phase2) {
        int powerRemoved;
        boolean unbalanced = true;
        int onePhaseLength = onePhase.length;
        if (onePhaseLength > 0) {
            int i = 0;
            while (unbalanced && onePhaseLength > 0) {
                try {
                    powerRemoved = this.turnOffEvcs(onePhase[i]);
                    this.powerWaitingList.put(onePhase[i].id(), powerRemoved);
                    this.timeStamps.put(onePhase[i].id(), new DateTime().getMinuteOfHour());
                    this.updatePower(true);
                    int maximum;
                    if (phase2) {
                        maximum = this.getMiddleLoad();
                    } else {
                        maximum = this.getMaximumLoad();
                    }
                    onePhaseLength--;
                    i++;

                    if (maximum - minimum > MAXIMUM_LOAD_DELTA) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " One Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }

    /**
     * Turns off Two phase EVCS that are on both Unbalanced Phases until its either balanced or no EVCS are left.
     *
     * @param min      Lowest Power Consumption
     * @param twoPhase All two phase EVCS that charge on the appropriate Phases
     * @return true if the Phases are now balanced
     */
    private boolean turnOffTwoPhaseEvcsBalancing(int min, ManagedEvcs[] twoPhase) {
        int powerRemoved;
        boolean unbalanced = true;
        int twoPhaseLength = twoPhase.length;
        if (twoPhaseLength > 0) {
            int i = 0;
            while (unbalanced && twoPhaseLength > 0) {
                try {
                    powerRemoved = this.turnOffEvcs(twoPhase[i]);
                    this.powerWaitingList.put(twoPhase[i].id(), powerRemoved);
                    this.timeStamps.put(twoPhase[i].id(), new DateTime().getMinuteOfHour());
                    this.updatePower(true);
                    int maximum = this.getMaximumLoad();
                    int middleLoad = this.getMiddleLoad();
                    twoPhaseLength--;
                    i++;
                    if (this.balance(maximum, middleLoad, min)) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }

    /**
     * Turns off Two phase EVCS that are on both Unbalanced Phases until its either balanced or no EVCS are left.
     *
     * @param min      Lowest Power Consumption
     * @param twoPhase All two phase EVCS that charge on the appropriate Phases
     * @return true if the Phases are now balanced
     */
    private boolean turnOffTwoPhaseDoubleHitEvcsBalancing(int min, ManagedEvcs[] twoPhase) {
        int powerRemoved;
        boolean unbalanced = true;
        ManagedEvcs[] twoPhaseDoubleHit = this.getTwoPhaseEvcs(twoPhase, this.maxIndex, this.minIndex);
        int twoPhaseDoubleHitLength = twoPhaseDoubleHit.length;
        if (twoPhaseDoubleHitLength > 0) {
            int i = 0;
            while (unbalanced && twoPhaseDoubleHitLength > 0) {
                try {
                    powerRemoved = this.turnOffEvcs(twoPhaseDoubleHit[i]);
                    this.powerWaitingList.put(twoPhaseDoubleHit[i].id(), powerRemoved);
                    this.timeStamps.put(twoPhaseDoubleHit[i].id(), new DateTime().getMinuteOfHour());
                    this.updatePower(true);
                    int maximum = this.getMaximumLoad();
                    int middleLoad = this.getMiddleLoad();
                    twoPhaseDoubleHitLength--;
                    i++;
                    if (this.balance(maximum, middleLoad, min)) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }
    //--------------Power Off for Phase Limitation--------------\\

    /**
     * Turns off EVCS fot the Phase Limit.
     * Priority List:
     * 1. All Three Phases are over the Limit:
     * 1.1 Three Phasers
     * 1.2. Two Phasers IF Three Phasers don't exist / not enough to reduce at least one Phase
     * 1.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 2. Two phases are over the Limit:
     * 2.1 Two Phasers on both Phases over the Limit
     * 2.2 Three Phasers IF no Two Phasers of the above condition exist / not enough to reduce at least one Phase
     * 2.3 One Phasers IF neither Three nor Two Phasers of above condition exist / not enough to reduce at least one Phase
     * 2.4 Two Phasers IF none of the above apply
     * 3. One Phases is over the Limit:
     * 3.1 One Phasers
     * 3.2 Three Phasers IF no One Phasers exist / not enough to reduce at least one Phase
     * 3.3 Two Phasers IF neither Three Phasers nor One Phasers exist / not enough to reduce at least one Phase
     */
    private void turnOffEvcsPhaseLimit() throws Exception {
        this.updatePower(true);
        int powerToReduceL1 = this.powerL1 - (this.phaseLimit / GRID_VOLTAGE);
        int powerToReduceL2 = this.powerL2 - (this.phaseLimit / GRID_VOLTAGE);
        int powerToReduceL3 = this.powerL3 - (this.phaseLimit / GRID_VOLTAGE);
        ManagedEvcs[] threePhases = this.getThreePhaseEvcs();
        int minReduce = Math.min(Math.min(powerToReduceL1, powerToReduceL2), powerToReduceL3);
        int minIndex = this.getPhaseByPower(powerToReduceL1, powerToReduceL2, powerToReduceL3, minReduce);
        int phaseOkIndex;

        //--------1. All Three Phases are over the Limit-------\\
        if (this.threePhasesOverPhaseLimit(powerToReduceL1, powerToReduceL2, powerToReduceL3)) {
            //--------1.1 Reduce Three Phasers if they exist----------\\
            if (threePhases.length > 0) {
                int reduceDelta;
                minReduce = this.turnOffThreePhaseEvcs(threePhases, minReduce);
                switch (minIndex) {
                    case 1:
                        reduceDelta = powerToReduceL1 - minReduce;
                        break;
                    case 2:
                        reduceDelta = powerToReduceL2 - minReduce;
                        break;
                    case 3:
                        reduceDelta = powerToReduceL3 - minReduce;
                        break;
                    default:
                        reduceDelta = 0;
                }
                powerToReduceL1 -= reduceDelta;
                powerToReduceL2 -= reduceDelta;
                powerToReduceL3 -= reduceDelta;
            }
            //--------If there are no Three Phasers or not enough----------\\
            if (minReduce > 0) {
                ManagedEvcs[] twoPhases = this.getTwoPhaseEvcs(minIndex);

                //-------1.2 Reduce Two Phasers if they exist--------\\
                if (twoPhases.length > 0) {

                    ManagedEvcs[] twoPhases1 = this.getTwoPhaseEvcs(twoPhases, this.maxIndex, this.middleIndex);
                    ManagedEvcs[] twoPhases2 = this.getTwoPhaseEvcs(twoPhases, this.middleIndex, this.maxIndex);
                    if (twoPhases1.length > 0 || twoPhases2.length > 0) {
                        int reduceByGroup1;
                        int reduceByGroup2;
                        int reduce1Delta = 0;
                        int reduce2Delta = 0;

                        if (twoPhases1.length > 0 && twoPhases2.length > 0) {
                            reduceByGroup1 = Math.floorDiv(minReduce, 2);
                            reduceByGroup2 = Math.floorDiv(minReduce, 2) + 1;
                            reduceByGroup1 = this.turnOffTwoPhaseEvcs(twoPhases1, reduceByGroup1);
                            reduce1Delta = Math.floorDiv(minReduce, 2) - reduceByGroup1;
                            reduceByGroup2 = this.turnOffTwoPhaseEvcs(twoPhases2, reduceByGroup2);
                            reduce2Delta = (Math.floorDiv(minReduce, 2) + 1) - reduceByGroup2;
                        } else if (twoPhases1.length > 0) {
                            reduceByGroup1 = this.turnOffTwoPhaseEvcs(twoPhases1, minReduce);
                            reduce1Delta = minReduce - reduceByGroup1;
                        } else {
                            reduceByGroup2 = this.turnOffTwoPhaseEvcs(twoPhases2, minReduce);
                            reduce2Delta = minReduce - reduceByGroup2;
                        }
                        minReduce -= (reduce1Delta + reduce2Delta);
                        powerToReduceL1 -= this.allocateReduceToPhase(1, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                        powerToReduceL2 -= this.allocateReduceToPhase(2, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                        powerToReduceL3 -= this.allocateReduceToPhase(3, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                    }
                }
                //-----1.3 Reduce One Phasers. If this code is reached and they don't exist, something went wrong-----\\
                if (minReduce > 0) {
                    ManagedEvcs[] onePhases = this.getOnePhaseEvcs(minIndex);
                    minReduce = this.turnOffOnePhaseEvcs(onePhases, minReduce);
                    switch (minIndex) {
                        case 1:
                            powerToReduceL1 -= minReduce;
                            break;
                        case 2:
                            powerToReduceL2 -= minReduce;
                            break;
                        case 3:
                            powerToReduceL3 -= minReduce;
                            break;

                    }

                }
                if (minReduce > 0) {
                    throw new Exception();
                }
            }
            phaseOkIndex = minIndex;
            //------------------2.Reduce the Second Phase--------------\\
            if (this.twoPhasesOverPhaseLimit(powerToReduceL1, powerToReduceL2, powerToReduceL3)) {
                minReduce = this.getMiddleReduce(powerToReduceL1, powerToReduceL2, powerToReduceL3);
                minIndex = this.getPhaseByPower(powerToReduceL1, powerToReduceL2, powerToReduceL3, minReduce);

                //---------------2.1 Reduce Two Phasers on both Phases over the Limit--------------\\
                ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(minIndex, phaseOkIndex);
                if (twoPhase.length > 0) {
                    int reducedByTwoPhase = this.turnOffTwoPhaseEvcs(twoPhase, minReduce);
                    int reduceDelta = minReduce - reducedByTwoPhase;
                    switch (phaseOkIndex) {
                        case 1:
                            powerToReduceL2 -= reduceDelta;
                            powerToReduceL3 -= reduceDelta;
                            break;
                        case 2:
                            powerToReduceL1 -= reduceDelta;
                            powerToReduceL3 -= reduceDelta;
                            break;
                        case 3:
                            powerToReduceL1 -= reduceDelta;
                            powerToReduceL2 -= reduceDelta;
                            break;
                    }

                }
                //--------------2.2 Three Phasers--------------\\
                if (minReduce > 0) {

                }

            }
        }


    }

    /**
     * Returns the second highest Reduce value.
     *
     * @param powerToReduceL1 Reduce value of L1
     * @param powerToReduceL2 Reduce value of L2
     * @param powerToReduceL3 Reduce value of L3
     * @return the second highest Reduce value
     */
    private int getMiddleReduce(int powerToReduceL1, int powerToReduceL2, int powerToReduceL3) {
        if (powerToReduceL1 > powerToReduceL2 && powerToReduceL2 > powerToReduceL3) {
            return powerToReduceL2;
        } else if (powerToReduceL1 > powerToReduceL2 && powerToReduceL3 > powerToReduceL2) {
            return powerToReduceL3;

        } else if (powerToReduceL3 > powerToReduceL2 && powerToReduceL2 > powerToReduceL1) {
            return powerToReduceL2;
        } else if (powerToReduceL3 > powerToReduceL2 && powerToReduceL1 > powerToReduceL2) {
            return powerToReduceL1;
        } else {
            return Math.max(powerToReduceL1, powerToReduceL3);
        }

    }

    /**
     * Allocated Reduce Amounts to the Phase it belongs to.
     * NOTE: only for the Context where Two Phase EVCS have been turned off for the Phase Limit.
     *
     * @param phaseNumber  The Phase that has to be reduced
     * @param twoPhases1   the first Group of Two Phase Evcs
     * @param twoPhases2   the second Group of Two Phase Evcs
     * @param reduce1Delta the power Reduced by Group 1
     * @param reduce2Delta the power Reduced by Group 2
     * @return the appropriate reduce amount
     */
    private int allocateReduceToPhase(int phaseNumber, ManagedEvcs[] twoPhases1, ManagedEvcs[] twoPhases2, int reduce1Delta, int reduce2Delta) {
        ManagedEvcs tp1 = twoPhases1[0];
        int[] tpPhases = tp1.getPhaseConfiguration();
        if (tpPhases[0] == phaseNumber || tpPhases[1] == phaseNumber) {
            return reduce1Delta;
        }
        ManagedEvcs tp2 = twoPhases2[0];
        int[] tp2Phases = tp2.getPhaseConfiguration();
        if (tp2Phases[0] == phaseNumber || tp2Phases[1] == phaseNumber) {
            return reduce2Delta;
        }

        return reduce1Delta + reduce2Delta;
    }

    //--------------Power Off for Power Limitation--------------\\

    /**
     * Turns off EVCS for the Power Limit
     * Priority List:
     * 1. Three Phases
     * 2. Two Phases on the two highest Phases (if existent)
     * 3. Two Phases
     * 4. One Phase
     */
    private void turnOffEvcsPowerLimit() throws Exception {
        this.updatePower(true);
        int powerToReduce = this.powerL1 + this.powerL2 + this.powerL3 - this.powerLimit / GRID_VOLTAGE;
        if (powerToReduce > 0) {
            int max;
            int middle;
            int min;
            ManagedEvcs[] onePhases;
            ManagedEvcs[] twoPhases = this.getTwoPhaseEvcs();
            ManagedEvcs[] threePhases = this.getThreePhaseEvcs();

            //-----Power Off Three Phase EVCS because they won't create balance issues-----\\
            if (threePhases.length > 0) {
                powerToReduce = this.turnOffThreePhaseEvcs(threePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    return;
                }
            }

            //------------------------------Power Off Two Phase EVCS---------------------------------\\
            if (twoPhases.length > 0) {
                powerToReduce = this.turnOffTwoPhaseEvcs(twoPhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            onePhases = this.getOnePhaseEvcs(this.getPhaseByPower(max));
            //-----------------------------Power Off One Phase EVCS----------------------------------\\
            if (onePhases.length > 0) {
                powerToReduce = this.turnOffOnePhaseEvcs(onePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            onePhases = this.getOnePhaseEvcs(this.getPhaseByPower(middle));

            //-----------------------------Power Off One Phase EVCS----------------------------------\\
            if (onePhases.length > 0) {
                powerToReduce = this.turnOffOnePhaseEvcs(onePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

        } else {
            this.log.info("Already under the Power Limit. This should not have happened.");
        }
    }


    //-----------------------General Turn Offs-------------------------\\

    /**
     * Turns off One phase EVCS until its either under the Power Limit or no EVCS are left.
     *
     * @param onePhases     All one phase EVCS that charge on the appropriate Phase
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffOnePhaseEvcs(ManagedEvcs[] onePhases, int powerToReduce) {
        int powerRemoved;
        int onePhaseLength = onePhases.length;
        if (onePhaseLength > 0) {
            int i = 0;
            while (powerToReduce > 0 && onePhaseLength > 0) {
                try {
                    powerRemoved = this.turnOffEvcs(onePhases[i]);
                    this.powerWaitingList.put(onePhases[i].id(), powerRemoved);
                    this.timeStamps.put(onePhases[i].id(), new DateTime().getMinuteOfHour());
                    this.updatePower(true);
                    powerToReduce -= powerRemoved;
                    onePhaseLength--;
                    i++;

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " One Phase EVCS/s have been turned off.");
        }
        return powerToReduce;
    }


    /**
     * Turns off Two phase EVCS that are on the highest Phases until its either under the Power Limit or no EVCS are left.
     *
     * @param twoPhase      All two phase EVCS that charge on the appropriate Phases
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffTwoPhaseEvcs(ManagedEvcs[] twoPhase, int powerToReduce) {
        int powerRemoved;
        ManagedEvcs[] twoPhaseDoubleHit = this.getTwoPhaseEvcs(twoPhase, this.maxIndex, this.minIndex);
        int twoPhaseDoubleHitLength = twoPhaseDoubleHit.length;
        if (twoPhaseDoubleHitLength > 0) {
            int i = 0;
            while (powerToReduce > 0 && twoPhaseDoubleHitLength > 0) {
                try {
                    powerRemoved = this.turnOffEvcs(twoPhaseDoubleHit[i]);
                    this.powerWaitingList.put(twoPhaseDoubleHit[i].id(), powerRemoved);
                    this.timeStamps.put(twoPhaseDoubleHit[i].id(), new DateTime().getMinuteOfHour());
                    this.updatePower(true);
                    powerToReduce -= powerRemoved;
                    twoPhaseDoubleHitLength--;
                    i++;

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return powerToReduce;
    }

    /**
     * Turns off Three Phase EVCS until its either under the Power Limit or no EVCS are left.
     *
     * @param threePhases   All three phase EVCS
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffThreePhaseEvcs(ManagedEvcs[] threePhases, int powerToReduce) {
        int powerRemoved;
        int threePhaseLength = threePhases.length;
        int i = 0;
        while (powerToReduce > 0 && threePhaseLength > 0) {
            try {
                powerRemoved = this.turnOffEvcs(threePhases[i]);
                this.powerWaitingList.put(threePhases[i].id(), powerRemoved);
                this.timeStamps.put(threePhases[i].id(), new DateTime().getMinuteOfHour());
                this.updatePower(true);
                powerToReduce -= powerRemoved;
                threePhaseLength--;
                i++;
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Unable to turn Off EVCS. This should not have happened.");
            }
        }
        this.log.info(i + " Three Phase EVCS/s have been turned off.");

        return powerToReduce;
    }


    /**
     * Turns an EVCS off and returns their old Power Value.
     *
     * @param evcs The EVCS that has to be turned off
     * @return Last Power Value
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int turnOffEvcs(ManagedEvcs evcs) throws OpenemsError.OpenemsNamedException {
        int oldPower;
        if (evcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(evcs.getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
            oldPower = ((evcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(evcs.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE));
        } else {
            oldPower = (evcs.getChargePower().get() / GRID_VOLTAGE);
        }
        evcs.setChargePowerLimit(0);
        return oldPower;
    }

    /**
     * Removes an EVCS from an ManagedEvcs[] if its on the waitingList.
     *
     * @param evcs the ManagedEvcs[]
     * @return modified ManagedEvcs[]
     */
    private ManagedEvcs[] removeEvcsFromArray(ManagedEvcs[] evcs) {
        List<ManagedEvcs> output = new ArrayList<>();
        for (int i = 0; i < evcs.length; i++) {
            if (this.powerWaitingList.containsKey(evcs[i].id()) == false) {
                output.add(evcs[i]);
            }
        }
        return this.convertListIntoArray(output);
    }


    //---------------------General Methods-----------------------\\

    /**
     * Returns if the 3 given Phase Power Values are balanced or not.
     *
     * @param max     Highest Power Value
     * @param middle  Middle Power Value
     * @param minimum Lowest Power Value
     * @return true if its Balanced
     */
    private boolean balance(int max, int middle, int minimum) {
        return (max - minimum < MAXIMUM_LOAD_DELTA && middle - minimum < MAXIMUM_LOAD_DELTA);
    }

    /**
     * Creates Arrays of problematic EVCS and wraps them into another Array.
     *
     * @return Array (either 2 or 4 in length) of EVCS Arrays
     */
    private Optional<List<ManagedEvcs[]>> unbalancedEvcsOnPhase() {
        if (this.middleIndex != 0) {
            this.log.info("There exists an unbalanced load on Phases " + this.maxIndex + " and " + this.middleIndex);
        } else {
            this.log.info("There exists an unbalanced load on Phase " + this.maxIndex);
        }
        ManagedEvcs[] onePhase = this.getOnePhaseEvcs(this.maxIndex);
        ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(this.maxIndex);
        List<ManagedEvcs[]> output = new ArrayList<>();
        output.add(ONE_PHASE_INDEX, onePhase);
        output.add(TWO_PHASE_INDEX, twoPhase);
        //If there is an unbalanced load on 2 Phases
        if (this.middleIndex != 0) {
            ManagedEvcs[] onePhase2 = this.getOnePhaseEvcs(this.middleIndex);
            ManagedEvcs[] twoPhase2 = this.getTwoPhaseEvcs(this.middleIndex);
            output.add(ONE_PHASE_INDEX_2, onePhase2);
            output.add(TWO_PHASE_INDEX_2, twoPhase2);
        }
        return Optional.of(output);

    }


    /**
     * Puts all EVCS that charge with one Phase in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getOnePhaseEvcs() {
        List<ManagedEvcs> onePhaseList = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            if (this.evcss[i].getPhases().get() == 1) {
                onePhaseList.add(this.evcss[i]);
            }
        }
        return this.convertListIntoArray(onePhaseList);
    }


    /**
     * Puts all EVCS that charge with one Phase on the unbalanced Phase in an Array.
     *
     * @param problemPhase Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getOnePhaseEvcs(int problemPhase) {
        List<ManagedEvcs> onePhaseList = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            int[] phaseConfiguration = this.evcss[i].getPhaseConfiguration();
            if (this.evcss[i].getPhases().get() == 1 && phaseConfiguration[0] == problemPhase) {
                onePhaseList.add(this.evcss[i]);
            }
        }
        return this.convertListIntoArray(onePhaseList);
    }

    /**
     * Puts all EVCS that charge with two Phases in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs() {
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (int i = 0; i < this.evcss.length; i++) {
            int[] phaseConfiguration = this.evcss[i].getPhaseConfiguration();
            if (this.evcss[i].getPhases().get() == 2) {
                twoPhase.add(this.evcss[i]);
            }
        }
        return this.convertListIntoArray(twoPhase);
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
        return this.convertListIntoArray(twoPhase);
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
        return this.convertListIntoArray(twoPhase);
    }

    /**
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
     * This is an Expansion of the above Method.
     *
     * @param evcs          EVCS Array where all the applicable EVCS are in
     * @param problemPhase  Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @param excludedPhase The Phase that should not be one of the two phases.
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(ManagedEvcs[] evcs, int problemPhase, int excludedPhase) {
        if (excludedPhase < 1) {
            excludedPhase = 3;
        }
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (int i = 0; i < evcs.length; i++) {
            int[] phaseConfiguration = evcs[i].getPhaseConfiguration();
            if (evcs[i].getPhases().get() == 2 && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)
                    && (phaseConfiguration[0] != excludedPhase || phaseConfiguration[1] != excludedPhase)) {
                twoPhase.add(evcs[i]);
            }
        }
        return this.convertListIntoArray(twoPhase);
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
        return this.convertListIntoArray(threePhase);
    }

    /**
     * This converts a ManagedEVCS List into in Array since its impossible to cast.
     *
     * @param phaseList List of ManagedEvcs
     * @return Array of the same ManagedEvcs
     */
    private ManagedEvcs[] convertListIntoArray(List<ManagedEvcs> phaseList) {
        ManagedEvcs[] output = new ManagedEvcs[phaseList.size()];
        for (int i = 0; i < phaseList.size(); i++) {
            output[i] = phaseList.get(i);
        }
        return output;
    }

    /**
     * Update the Array of EVCSS.
     */
    private void updateEvcss() {
        for (int i = 0; i < this.ids.length; i++) {
            try {
                if (this.cpm.getComponent(this.ids[i]) instanceof ManagedEvcs) {
                    this.evcss[i] = this.cpm.getComponent(this.ids[i]);
                } else {
                    throw new ConfigurationException("The EVCSsId list contains a wrong ID: ", this.ids[i] + " is not a EVCS");
                }
            } catch (OpenemsError.OpenemsNamedException | ConfigurationException ignored) {
                this.log.info("Unable to find Components. Retrying...");
            }
        }
    }


    /**
     * Detects the Maximum Phase/s and stores the information in this Object ( max,max2,maxIndex,max2Index ).
     *
     * @return The highest Power Consumption of all Phases.
     */
    private int getMaximumLoad() {
        this.max = 0;
        this.maxIndex = 0;
        this.middleIndex = 0;
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
                this.middleIndex = 2;
            }
        }
        if (max == this.powerL3) {
            if (this.max == 0) {
                this.max = this.powerL3;
                this.maxIndex = 3;
            } else {
                this.middleIndex = 3;
            }
        }
        return this.max;
    }

    /**
     * Returns the Phase that is in the middle of the other two phases IF its not the same as one of the other.
     *
     * @return The middle Power Consumption of all Phases
     */
    private int getMiddleLoad() {
        this.middle = 0;
        if (this.powerL1 > this.powerL2) {
            if (this.powerL2 > this.powerL3) {
                this.middleIndex = 2;
                this.middle = this.powerL2;
            } else if (this.powerL3 > this.powerL2) {
                this.middleIndex = 3;
                return this.powerL3;
            }
        } else if (this.powerL1 > this.powerL3) {
            this.middleIndex = 1;
            this.middle = this.powerL1;

        } else if (this.powerL3 > this.powerL1) {
            if (this.powerL3 > this.powerL2) {
                this.middleIndex = 2;
                return this.powerL2;
            } else if (this.powerL2 > this.powerL3) {
                this.middleIndex = 3;
                this.middle = this.powerL3;
            }
        }
        return this.middle;
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
     * @param power Power on a phase
     * @return Index or 0 if the power is not on any phase
     */
    private int getPhaseByPower(int power) {
        if (this.powerL1 == power) {
            return 1;
        } else if (this.powerL2 == power) {
            return 2;
        } else if (this.powerL3 == power) {
            return 3;
        }
        return 0;
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
                            this.powerL1 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
                            break;
                        case 2:
                            this.powerL2 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
                            break;
                        case 3:
                            this.powerL3 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
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
                                this.powerL1 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) / phaseCount;
                                break;
                            case 2:
                                this.powerL2 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) / phaseCount;
                                break;
                            case 3:
                                this.powerL3 += (target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(target.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE) / phaseCount;
                                break;
                        }
                    } else {
                        switch (phases[n]) {
                            case 1:
                                this.powerL1 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
                                break;
                            case 2:
                                this.powerL2 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
                                break;
                            case 3:
                                this.powerL3 += (target.getChargePower().orElse(0) / GRID_VOLTAGE) / phaseCount;
                                break;
                        }
                    }
                }
            }

        }
    }

    private void emergencyStop() {
    }

}
