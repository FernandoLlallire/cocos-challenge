package com.cocos.llallire.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Possible order statuses. Names match the values stored in the {@code orders} table.
 */
public enum OrderStatus {
    /** LIMIT order sent to the market, pending execution. */
    NEW,
    /** Executed order. */
    FILLED,
    /** Order rejected for not meeting the requirements (for example, insufficient funds). */
    REJECTED,
    /** Order cancelled by the user. */
    CANCELLED;

    /**
     * Statuses that affect a user's holdings: FILLED moves pesos and shares, NEW reserves them.
     *
     * @return immutable set with {@link #FILLED} and {@link #NEW}
     */
    public static Set<OrderStatus> affectingHoldings() {
        return Set.copyOf(EnumSet.of(FILLED, NEW));
    }
}
