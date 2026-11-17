package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

// domain/model/Returns.java
public final class Returns {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private Returns() {
    }

    /** Percentage change from {@code initialValue} to {@code finalValue}. Zero when there is no valid base price. */
    public static BigDecimal percentChange(BigDecimal initialValue, BigDecimal finalValue) {
        if (initialValue == null || finalValue == null || initialValue.signum() == 0) {
            return BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE);
        }
        return finalValue.subtract(initialValue)
                .multiply(ONE_HUNDRED)
                .divide(initialValue, Precision.PERCENT_SCALE, Precision.ROUNDING);
    }
}
