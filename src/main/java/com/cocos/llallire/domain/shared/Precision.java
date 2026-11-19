package com.cocos.llallire.domain.shared;

import java.math.RoundingMode;

/**
 * Numeric precision conventions of the domain.
 */
public final class Precision {

    /** Decimals of peso amounts. */
    public static final int MONEY_SCALE = 2;
    /** Decimals of percentages. */
    public static final int PERCENT_SCALE = 2;
    /** Commercial rounding. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Constants class; not instantiable.
     */
    private Precision() {
    }
}
