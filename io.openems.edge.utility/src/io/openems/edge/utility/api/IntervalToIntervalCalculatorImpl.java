package io.openems.edge.utility.api;


/**
 * The Implementation of an advanced RuleOfThree Calculator,
 * that can calculate a RuleOfThree by having different min and Max values as well as another minPercentage.
 * Use this if you want to calculate RuleOfThrees by yourself.
 * This class uses {@link IntervalToIntervalHelperCalculator}'s to calculate adapted Percentage values or "V" Values (V/P = G / 100)
 */
public class IntervalToIntervalCalculatorImpl implements IntervalToIntervalCalculator {

    private final IntervalToIntervalHelperCalculator valueCalculator = new IntervalToIntervalHelperCalculatorImpl();

    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param calculationType   the CalculationType (is incoming input a Percent Value or a "V" Value.
     * @param minValueIntervalA the min Value in this Value Range.
     * @param maxValueIntervalA the max Value in this Value Range.
     * @param minValueIntervalB the minimum Percentage Value.
     * @param maxValueIntervalB the maximum Percentage Value.
     * @param input             the input, either Percent or a "V" Value depending on the calculation type.
     * @return the adapted calculated Value.
     */

    @Override
    public double calculateDoubleByCalculationType(CalculationType calculationType, double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input) {
        switch (calculationType) {
            case VALUE_FROM_INTERVAL_A:
                return this.valueCalculator.calculateIntervalValueDouble(minValueIntervalA, maxValueIntervalA, minValueIntervalB, maxValueIntervalB, input);
            case VALUE_FROM_INTERVAL_B:
            default:
                return this.valueCalculator.calculateIntervalValueDouble(minValueIntervalB, maxValueIntervalB, minValueIntervalA, maxValueIntervalA, input);
        }
    }

    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param calculationType the CalculationType (is incoming input a Percent Value or a "V" Value.
     * @param minValue        the min Value in this Value Range.
     * @param maxValue        the max Value in this Value Range.
     * @param minPercentage   the minimum Percentage Value.
     * @param maxPercentage   the maximum Percentage Value.
     * @param input           the input, either Percent or a "V" Value depending on the calculation type.
     * @return the adapted calculated Value.
     */

    @Override
    public int calculateIntByCalculationType(CalculationType calculationType, int minValue, int maxValue, int minPercentage, int maxPercentage, int input) {
        return (int) Math.round(this.calculateDoubleByCalculationType(calculationType, minValue, maxValue, minPercentage, maxPercentage, input));
    }
}
