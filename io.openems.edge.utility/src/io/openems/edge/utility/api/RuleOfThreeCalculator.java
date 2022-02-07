package io.openems.edge.utility.api;

public interface RuleOfThreeCalculator {

    double calculateByRuleOfThreeDouble(double minRange, double maxRange, double input);

    int calculateByRuleOfThreeInteger(int minRange, int maxRange, int input);
}
