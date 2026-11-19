package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * State of a user's account.
 *
 * @param totalAccountValue total account value: peso balance (including reserved) plus the market value
 *                          of all positions
 * @param availableCash     pesos available to trade
 * @param positions         positions with holdings greater than zero, sorted by ticker
 */
public record Portfolio(
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        List<PositionValuation> positions) {
}
