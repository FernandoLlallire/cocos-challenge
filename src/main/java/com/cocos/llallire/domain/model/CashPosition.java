package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.llallire.domain.shared.Precision;

/**
 * A user's peso (ARS) position, calculated from their orders.
 *
 * <ul>
 *   <li><b>Balance</b>: sum of FILLED movements. {@code CASH_IN} and {@code SELL} add;
 *       {@code CASH_OUT} and {@code BUY} subtract {@code size * price}.</li>
 *   <li><b>Reserved</b>: amount committed by {@code BUY} orders in {@code NEW} status. It cannot be used
 *       for another order while the limit order is still pending.</li>
 *   <li><b>Available to trade</b>: balance minus reserved.</li>
 * </ul>
 */
public class CashPosition {
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal reserved = BigDecimal.ZERO;

    /**
     * Applies the orders to the cash position. Orders in other statuses (REJECTED, CANCELLED) are
     * ignored. Order does not matter because the operations are sums.
     *
     * @param orders user orders
     */
    public void processOrders(List<Order> orders) {
        orders.forEach(this::processOrder);
    }

    /**
     * Applies one order to the balance or the reserved amount, depending on its status and side.
     *
     * @param order order to apply
     */
    private void processOrder(Order order) {
        if (order.isFilled()) {
            switch (order.side()) {
                case CASH_IN, SELL -> balance = balance.add(order.notional());
                case CASH_OUT, BUY -> balance = balance.subtract(order.notional());
            }
        } else if (order.isPending() && order.side() == OrderSide.BUY) {
            reserved = reserved.add(order.notional());
        }
    }

    /**
     * Returns the balance in pesos, including the reserved amount.
     *
     * @return balance with money scale
     */
    public BigDecimal balance() {
        return balance.setScale(Precision.MONEY_SCALE, Precision.ROUNDING);
    }

    /**
     * Returns the pesos committed by pending buy orders.
     *
     * @return reserved amount with money scale
     */
    public BigDecimal reserved() {
        return reserved.setScale(Precision.MONEY_SCALE, Precision.ROUNDING);
    }

    /**
     * Returns the pesos available to trade ({@code balance - reserved}).
     *
     * @return available amount with money scale
     */
    public BigDecimal available() {
        return balance.subtract(reserved).setScale(Precision.MONEY_SCALE, Precision.ROUNDING);
    }
}
