package io.openems.edge.utility.api;

/**
 * The Interface of an IntervalToIntervalCalculator.
 * Use this if you want to calculate a Mapping of an Interval to another Interval.
 */
public interface IntervalToIntervalCalculator {

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
    double calculateDoubleByCalculationType(RepresentationType representationType, double minValueIntervalA, double maxValueIntervalA, double minValueIntervalB, double maxValueIntervalB, double input);


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

    int calculateIntByCalculationType(RepresentationType representationType, int minValueIntervalA, int maxValueIntervalA, int minValueIntervalB, int maxValueIntervalB, int input);
}
