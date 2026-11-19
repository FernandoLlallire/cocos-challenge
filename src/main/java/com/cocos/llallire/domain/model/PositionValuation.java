package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

/**
 * Position valued at market price.
 *
 * @param instrument     instrument of the position
 * @param quantity       number of shares
 * @param marketValue    market value in pesos ({@code quantity * close})
 * @param totalReturnPct total return (%) against the average cost
 */
public record PositionValuation(
        Instrument instrument,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPct) {
}
