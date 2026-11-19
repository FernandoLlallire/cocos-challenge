package com.cocos.llallire.domain.model;

/**
 * Side of an order: buy/sell of assets or cash transfer.
 */
public enum OrderSide {
    /** Purchase of an asset. */
    BUY,
    /** Sale of an asset. */
    SELL,
    /** Incoming peso transfer. */
    CASH_IN,
    /** Outgoing peso transfer. */
    CASH_OUT;

    /**
     * Tells whether the side is a cash transfer.
     *
     * @return {@code true} for {@link #CASH_IN} and {@link #CASH_OUT}
     */
    public boolean isCashTransfer() {
        return this == CASH_IN || this == CASH_OUT;
    }
}
