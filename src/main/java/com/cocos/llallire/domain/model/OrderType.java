package com.cocos.llallire.domain.model;

/**
 * Order type.
 */
public enum OrderType {
    /** Executed immediately at the latest market price. */
    MARKET,
    /** Sent to the market with a limit price and left pending. */
    LIMIT
}
