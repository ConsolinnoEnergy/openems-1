package io.openems.edge.utility.api;

/**
 * <p>
 * This Class calculates the function Value of a mapped Interval to another Interval.
 * Therefore we map an area of Interval A [a;b] to  Interval B [c;d]
 * With the Formula y = mx + h
 * where m = [d-c/b-a]
 * h = (bc-ad)/(b-a)
 * and x is the input.
 * Y is the function Value.
 * </p>
 */
public class IntervalToIntervalHelperCalculatorImpl implements IntervalToIntervalHelperCalculator {

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
    @Override
    public double calculateIntervalValueDouble(double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input) {
        double slope = (maxValueIntervalB - minValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        double constant = (maxValueIntervalA * minValueIntervalB - minValueIntervalA * maxValueIntervalB) / (maxValueIntervalA - minValueIntervalA);
        return input * slope + constant;
    }

}
