package com.cocos.llallire.domain.shared;

import java.math.BigDecimal;

/**
 * Percentage return calculations.
 */
public final class Returns {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /**
     * Utility class; not instantiable.
     */
    private Returns() {
    }

    /**
     * Calculates the percentage change: {@code (finalValue - initialValue) / initialValue * 100}.
     *
     * @param initialValue initial value
     * @param finalValue   final value
     * @return change with {@link Precision#PERCENT_SCALE} decimals, or zero if any value is null
     *         or the initial value is zero
     */
    public static BigDecimal percentChange(BigDecimal initialValue, BigDecimal finalValue) {
        if (initialValue == null || finalValue == null || initialValue.signum() == 0) {
            return BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE);
        }
        return finalValue.subtract(initialValue)
                .multiply(ONE_HUNDRED)
                .divide(initialValue, Precision.PERCENT_SCALE, Precision.ROUNDING);
    }
}
