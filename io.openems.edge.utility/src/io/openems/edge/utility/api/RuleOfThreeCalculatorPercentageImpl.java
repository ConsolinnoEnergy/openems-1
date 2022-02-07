package io.openems.edge.utility.api;

public class RuleOfThreeCalculatorPercentageImpl implements RuleOfThreeCalculatorPercentage {

    @Override
    public double calculateByRuleOfThreeDouble(double minRange, double maxRange, double input) {
        double overall = maxRange - minRange;
        return (input - minRange) / overall;
        /*
        * 20 - 4 = 16      12 mA
        *
        * 12 - 4 / 16
        * 8/16 == 50% correct
        *
        * 4 == 0%
        *
        * */

    }

    @Override
    public int calculateByRuleOfThreeInteger(int minRange, int maxRange, int input) {
        return (int) this.calculateByRuleOfThreeDouble(minRange, maxRange, input);
    }
}
