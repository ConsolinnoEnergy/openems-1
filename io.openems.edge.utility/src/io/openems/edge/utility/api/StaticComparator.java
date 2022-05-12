package io.openems.edge.utility.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;

/**
 * The Static Comparator helps to compare two values depending on the ComparatorType.
 */
public interface StaticComparator {
    /**
     * Compare Two Double Values.
     *
     * @param comparatorType the {@link ComparatorType}
     * @param value_A        the first Value
     * @param value_B        the second Value
     * @return a boolean
     */
    static boolean compare(ComparatorType comparatorType, Double value_A, Double value_B) {
        if (value_A != null && value_B != null) {
            switch (comparatorType) {

                case EQUALS:
                    return value_A.equals(value_B);
                case GREATER:
                    return value_A > value_B;
                case LESS:
                    return value_A < value_B;
                case GREATER_OR_EQUALS:
                    return value_A >= value_B;
                case LESSER_OR_EQUALS:
                    return value_A <= value_B;
                case NOT_EQUALS:
                default:
                    return !value_A.equals(value_B);
            }
        }
        return false;
    }

    /**
     * Compare Two Double Values.
     *
     * @param comparatorType the {@link ComparatorType}
     * @param value_A        the first Value
     * @param value_B        the second Value
     * @return a boolean
     */
    static boolean compare(ComparatorType comparatorType, Integer value_A, Integer value_B) {
        if (value_A != null && value_B != null) {
            switch (comparatorType) {

                case EQUALS:
                    return value_A.equals(value_B);
                case GREATER:
                    return value_A > value_B;
                case LESS:
                    return value_A < value_B;
                case GREATER_OR_EQUALS:
                    return value_A >= value_B;
                case LESSER_OR_EQUALS:
                    return value_A <= value_B;
                case BIT_EQUALS:
                    return (value_A & value_B) == value_B;
                case NOT_EQUALS:
                default:
                    return !value_A.equals(value_B);
            }
        }
        return false;
    }

    /**
     * Compare Two Double Values.
     *
     * @param comparatorType the {@link ComparatorType}
     * @param value_A        the first Value
     * @param value_B        the second Value
     * @return a boolean
     */
    static boolean compare(ComparatorType comparatorType, Value<?> value_A, Value<?> value_B) {
        return compare(comparatorType, (Double) TypeUtils.getAsType(OpenemsType.DOUBLE, value_A), (Double) TypeUtils.getAsType(OpenemsType.DOUBLE, value_B));
    }

    /**
     * Compare Two Double Values.
     *
     * @param comparatorType the {@link ComparatorType}. Only Equals/NotEquals is considered.
     * @param value_A        the first Value
     * @param value_B        the second Value
     * @return a boolean
     */
    static boolean compare(ComparatorType comparatorType, Boolean value_A, Boolean value_B) {
        if (value_A != null && value_B != null) {
            if (comparatorType == ComparatorType.NOT_EQUALS) {
                return value_A != value_B;
            }
            return value_A == value_B;
        }
        return false;
    }
}
