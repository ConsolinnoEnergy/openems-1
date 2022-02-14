package io.openems.edge.utility.api;

/**
 * <p>
 * The IntervalToIntervalCalculator.
 * Use this if you want to calculate a Mapping of an Interval to another Interval.
 * E.g. Interval A has a range of [0;10] Interval B[-5;5]. When the input x is of Interval A,
 * you can calculate the corresponding value to Interval B.
 * </p>
 * <p>
 * Formula: y = mx + h
 * m = [d-c/b-a] = slope
 * h = (bc-ad)/(b-a) = constant
 * a = minValue
 * b = maxValue
 * c = minPercentage
 * d = maxPercentage
 * </p>
 */
public interface IntervalToIntervalCalculator {

    /**
     * Calculation Type.
     * These Combinations represent, if the input Value comes from the first (A) or the second interval (B).
     * Therefore, the mapped value of the other Interval can be calculated.
     */
    public enum RepresentationType {

        VALUE_FROM_INTERVAL_A, VALUE_FROM_INTERVAL_B
    }

    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param representationType the CalculationType (is incoming input from Interval A or B)
     * @param minValueIntervalA  the min Value of Interval A
     * @param maxValueIntervalA  the max Value of Interval A.
     * @param minValueIntervalB  the minimum Value of Interval B.
     * @param maxValueIntervalB  the maximum Value of Interval B.
     * @param input              the input, either from Interval A or B.
     * @return the adapted calculated Value corresponding to the other Interval (different to Input)
     */
    static double calculateDoubleByCalculationType(RepresentationType representationType, double minValueIntervalA,
                                                   double maxValueIntervalA, double minValueIntervalB,
                                                   double maxValueIntervalB, double input) {
        switch (representationType) {
            case VALUE_FROM_INTERVAL_A:
                return calculateIntervalValueDouble(minValueIntervalA, maxValueIntervalA, minValueIntervalB, maxValueIntervalB, input);
            case VALUE_FROM_INTERVAL_B:
            default:
                return calculateIntervalValueDouble(minValueIntervalB, maxValueIntervalB, minValueIntervalA, maxValueIntervalA, input);
        }
    }


    /**
     * Calculates a Value depending on the CalculationType.
     *
     * @param representationType the CalculationType (is incoming input from Interval A or B)
     * @param minValueIntervalA  the min Value of Interval A
     * @param maxValueIntervalA  the max Value of Interval A.
     * @param minValueIntervalB  the minimum Value of Interval B.
     * @param maxValueIntervalB  the maximum Value of Interval B.
     * @param input              the input, either from Interval A or B.
     * @return the adapted calculated Value corresponding to the other Interval (different to Input)
     */

    static int calculateIntByCalculationType(RepresentationType representationType, int minValueIntervalA,
                                             int maxValueIntervalA, int minValueIntervalB, int maxValueIntervalB,
                                             int input) {
        return (int) Math.round(calculateDoubleByCalculationType(representationType, minValueIntervalA, maxValueIntervalA, minValueIntervalB, maxValueIntervalB, input));
    }

    /**
     * Calculates the function Value, depending on the Intervals A [a;b]and B[a;b].
     * Formula: y = mx + h
     * m = [d-c/b-a] = slope
     * h = (bc-ad)/(b-a) = constant
     * a = minValue
     * b = maxValue
     * c = minPercentage
     * d = maxPercentage
     *
     * @param minValueIntervalA the minimum of the interval A.
     * @param maxValueIntervalA the maximum of the interval A.
     * @param minValueIntervalB the minimum of the interval B.
     * @param maxValueIntervalB the maximum of the interval B.
     * @param input             the Value of X.
     * @return the function Value Y.
     */
    static double calculateIntervalValueDouble(double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input) {
        double slope = (maxValueIntervalB - minValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        double constant = (maxValueIntervalA * minValueIntervalB - minValueIntervalA * maxValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        return input * slope + constant;
    }
}
