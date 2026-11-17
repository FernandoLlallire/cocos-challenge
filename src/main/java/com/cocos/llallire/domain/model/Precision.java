package com.cocos.llallire.domain.model;

import java.math.RoundingMode;

public final class Precision {

    public static final int MONEY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Precision() {
    }
}