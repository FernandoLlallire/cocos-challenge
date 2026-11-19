package com.cocos.llallire.domain.model;

/**
 * Supported instrument types. Cash (ARS) is modeled as a {@link #MONEDA} instrument.
 */
public enum InstrumentType {
    /** Stocks tradable in the market. */
    ACCIONES,
    /** Currency; the peso (ARS) is the only instrument of this type. */
    MONEDA
}
