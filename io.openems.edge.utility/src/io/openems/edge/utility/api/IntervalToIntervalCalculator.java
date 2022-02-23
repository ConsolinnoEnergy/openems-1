package io.openems.edge.utility.api;

/**
 * <p>
 * The IntervalToIntervalCalculator that maps a value of one interval to another interval value.
 * When the Input x of the interval A is given, and has a range of [0;10] and interval B has a range of [-5;5].
 * This Interface calculates the corresponding value y to interval B.
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
     * These Combinations represent, if the input value comes from the first (A) or the second interval (B).
     * Therefore, the mapped value of the other interval can be calculated.
     */
    public enum RepresentationType {

        VALUE_FROM_INTERVAL_A, VALUE_FROM_INTERVAL_B
    }

    /**
     * Calculates a value depending on the CalculationType.
     *
     * @param representationType the CalculationType (is incoming input from interval A or B)
     * @param minValueIntervalA  the min value of interval A
     * @param maxValueIntervalA  the max value of interval A.
     * @param minValueIntervalB  the minimum value of interval B.
     * @param maxValueIntervalB  the maximum value of interval B.
     * @param input              the input, either from interval A or B.
     * @return the adapted calculated value corresponding to the other interval (different to Input)
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
     * Calculates a value depending on the CalculationType.
     *
     * @param representationType the CalculationType (is incoming input from interval A or B)
     * @param minValueIntervalA  the min value of interval A
     * @param maxValueIntervalA  the max value of interval A.
     * @param minValueIntervalB  the minimum value of interval B.
     * @param maxValueIntervalB  the maximum value of interval B.
     * @param input              the input, either from interval A or B.
     * @return the adapted calculated value corresponding to the other interval (different to Input)
     */

    static int calculateIntByCalculationType(RepresentationType representationType, int minValueIntervalA,
                                             int maxValueIntervalA, int minValueIntervalB, int maxValueIntervalB,
                                             int input) {
        return (int) Math.round(calculateDoubleByCalculationType(representationType, minValueIntervalA, maxValueIntervalA, minValueIntervalB, maxValueIntervalB, input));
    }

    /**
     * Calculates the function value, depending on the intervals A [a;b] and B [a;b].
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
     * @param input             the value of X.
     * @return the function value Y.
     */
    static double calculateIntervalValueDouble(double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input) {
        double slope = (maxValueIntervalB - minValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        double constant = (maxValueIntervalA * minValueIntervalB - minValueIntervalA * maxValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        return input * slope + constant;
    }
}
