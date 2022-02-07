package io.openems.edge.utility.api;

/**
 * The Interface for a HelperClass of the RuleOfThree Calculator.
 * The Implementations either calculate a Percent Value or a "V" Value.
 */
public interface IntervalToIntervalHelperCalculator {

    /**
     * Calculates a Value depending on the Implementation (Percentage or "V").
     *
     * @param minValueIntervalA the min Value in this Value Range.
     * @param maxValueIntervalA the max Value in this Value Range.
     * @param minValueIntervalB the minimum Percentage Value.
     * @param maxValueIntervalB the maximum Percentage Value.
     * @param input             the input, either Percent or a "V" Value depending on the calculation type.
     * @return the adapted calculated Value.
     */
    double calculateIntervalValueDouble(double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input);

}
