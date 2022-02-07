package io.openems.edge.utility.api;

/**
 * A Calculator, that can calculate a RuleOfThree by having different min and Max values as well as another minPercentage.
 * Use this if you want to calculate RuleOfThrees by yourself.
 * This class uses {@link IntervalToIntervalHelperCalculator}'s to calculate adapted Percentage values or "V" Values (V/P = G / 100)
 */
public interface IntervalToIntervalCalculator {

    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param calculationType   the CalculationType (is incoming input a Percent Value or a "V" Value.
     * @param minValueIntervalA the min Value of the Interval A.
     * @param maxValueIntervalA the max Value of Interval A.
     * @param minValueIntervalB the min Value of Interval B.
     * @param maxValueIntervalB the max Value of Interval B.
     * @param input             the input, either Percent or a "V" Value depending on the calculation type.
     * @return the adapted calculated Value.
     */

    double calculateDoubleByCalculationType(CalculationType calculationType, double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input);

    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param calculationType   the CalculationType (is incoming input a Percent Value or a "V" Value.
     * @param minValueIntervalA the min Value of the Interval A.
     * @param maxValueIntervalA the max Value of Interval A.
     * @param minValueIntervalB the min Value of Interval B.
     * @param maxValueIntervalB the max Value of Interval B.
     * @param input             the input, either Percent or a "V" Value depending on the calculation type.
     * @return the adapted calculated Value.
     */

    int calculateIntByCalculationType(CalculationType calculationType, int minValueIntervalA, int maxValueIntervalA, int minValueIntervalB, int maxValueIntervalB, int input);
}
