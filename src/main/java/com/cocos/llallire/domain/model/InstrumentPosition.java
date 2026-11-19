package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import com.cocos.llallire.domain.shared.Precision;
import com.cocos.llallire.domain.shared.Returns;

/**
 * A user's holding of an instrument (stock), calculated from their orders.
 *
 * <p>FILLED buy and sell orders are processed chronologically, and the cost is calculated using
 * the <b>weighted average cost</b> method:</p>
 * <ul>
 *   <li>{@code BUY}: adds the quantity and adds {@code size * price} to the cost.</li>
 *   <li>{@code SELL}: subtracts the quantity and releases the proportional part of the cost
 *       ({@code average cost * size}).</li>
 * </ul>
 *
 * <p>{@code SELL} orders in {@code NEW} status reserve shares: they cannot be sold again
 * while the limit order is still pending.</p>
 *
 * <p>If a sale exceeds the quantity held (inconsistent data), the holding becomes zero instead of
 * negative, since short selling is not supported.</p>
 */
public class InstrumentPosition {
    private final Instrument instrument;
    private long quantity;
    private long reservedQuantity;
    private BigDecimal costBasis;

    /**
     * Creates an empty position for the instrument.
     *
     * @param instrument instrument of the position
     */
    public InstrumentPosition(Instrument instrument) {
        this.instrument = instrument;
        this.quantity = 0;
        this.reservedQuantity = 0;
        this.costBasis = BigDecimal.ZERO;
    }

    /**
     * Returns the instrument of the position.
     *
     * @return instrument
     */
    public Instrument instrument() {
        return instrument;
    }

    /**
     * Returns the number of shares held (FILLED).
     *
     * @return number of shares
     */
    public long quantity() {
        return quantity;
    }

    /**
     * Returns the shares committed by pending sell orders.
     *
     * @return reserved quantity
     */
    public long reservedQuantity() {
        return reservedQuantity;
    }

    /**
     * Returns the shares that can be sold ({@code quantity - reserved}).
     *
     * @return available quantity, never negative
     */
    public long availableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }

    /**
     * Returns the cost of the current holding using the weighted average cost.
     *
     * @return cost in pesos with money scale
     */
    public BigDecimal costBasis() {
        return costBasis.setScale(Precision.MONEY_SCALE, Precision.ROUNDING);
    }

    /**
     * Applies the orders to the position. Only {@code BUY}/{@code SELL} orders of this instrument are
     * considered: FILLED ones change the holding (chronologically, because the average cost depends on
     * the order) and {@code SELL} orders in NEW status reserve shares. Everything else is ignored.
     *
     * @param orders user orders
     */
    public void processOrders(List<Order> orders) {
        List<Order> instrumentOrders = orders.stream()
                .filter(order -> instrument.id().equals(order.instrumentId()))
                .filter(order -> order.side() == OrderSide.BUY || order.side() == OrderSide.SELL)
                .toList();
        instrumentOrders.stream()
                .filter(Order::isFilled)
                .sorted(Order.CHRONOLOGICAL)
                .forEach(this::applyFilledOrder);
        instrumentOrders.stream()
                .filter(order -> order.isPending() && order.side() == OrderSide.SELL)
                .forEach(order -> reservedQuantity += order.validSize());
    }

    /**
     * Values the position at the latest market price.
     *
     * @param lastMarketdata latest marketdata of the instrument
     * @return quantity, market value and total return of the position
     */
    public PositionValuation valuate(Marketdata lastMarketdata) {
        BigDecimal rawMarketValue = lastMarketdata.close().multiply(BigDecimal.valueOf(quantity));
        return new PositionValuation(
                instrument,
                quantity,
                rawMarketValue.setScale(Precision.MONEY_SCALE, Precision.ROUNDING),
                Returns.percentChange(costBasis, rawMarketValue));
    }

    /**
     * Applies an executed order to the holding.
     *
     * @param order FILLED buy or sell order
     */
    private void applyFilledOrder(Order order) {
        if (order.side() == OrderSide.BUY) {
            buy(order);
        } else {
            sell(order.validSize());
        }
    }

    /**
     * Adds the purchase to the holding and its cost.
     *
     * @param order executed buy order
     */
    private void buy(Order order) {
        costBasis = costBasis.add(order.notional());
        quantity += order.validSize();
    }

    /**
     * Subtracts the sale from the holding and releases the cost proportional to the average cost.
     *
     * @param size quantity sold
     */
    private void sell(long size) {
        long remaining = quantity - size;
        if (remaining <= 0) {
            quantity = 0;
            costBasis = BigDecimal.ZERO;
            return;
        }
        costBasis = costBasis.multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(quantity), MathContext.DECIMAL128);
        quantity = remaining;
    }
}
