package com.cocos.llallire.domain.model;

import static com.cocos.llallire.TestData.METR;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.marketdata;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link InstrumentPosition}.
 */
class InstrumentPositionTest {

    @Test
    void buysAccumulateQuantityAndCost() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "800", 0),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1));

        assertThat(position.quantity()).isEqualTo(20);
        assertThat(position.costBasis()).isEqualByComparingTo("17000");
    }

    @Test
    void sellsReleaseCostAtWeightedAveragePrice() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "800", 0),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1),
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 5, "1200", 2));

        assertThat(position.quantity()).isEqualTo(15);
        assertThat(position.costBasis()).isEqualByComparingTo("12750");
    }

    @Test
    void ordersAreAppliedChronologicallyRegardlessOfInputOrder() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 5, "1200", 2),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "800", 0));

        assertThat(position.quantity()).isEqualTo(15);
        assertThat(position.costBasis()).isEqualByComparingTo("12750");
    }

    @Test
    void sellingMoreThanHeldLeavesAnEmptyPosition() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 20, "1540", 0),
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 30, "1530", 1));

        assertThat(position.quantity()).isZero();
        assertThat(position.costBasis()).isEqualByComparingTo("0");
    }

    @Test
    void pendingSellsReserveSharesAndPendingBuysDoNotAddShares() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 0),
                order(PAMP, OrderSide.SELL, OrderStatus.NEW, 4, "1000", 1),
                order(PAMP, OrderSide.BUY, OrderStatus.NEW, 50, "850", 2));

        assertThat(position.quantity()).isEqualTo(10);
        assertThat(position.reservedQuantity()).isEqualTo(4);
        assertThat(position.availableQuantity()).isEqualTo(6);
    }

    @Test
    void ignoresOtherInstrumentsCashAndNonFilledOrders() {
        InstrumentPosition position = positionOf(
                cashIn(1_000_000),
                order(METR, OrderSide.BUY, OrderStatus.FILLED, 100, "200", 0),
                order(PAMP, OrderSide.BUY, OrderStatus.REJECTED, 100, "900", 1),
                order(PAMP, OrderSide.BUY, OrderStatus.CANCELLED, 100, "900", 2),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 1, "900", 3));

        assertThat(position.quantity()).isEqualTo(1);
        assertThat(position.costBasis()).isEqualByComparingTo("900");
    }

    @Test
    void valuatesAtLastCloseAndComputesTotalReturn() {
        InstrumentPosition position = positionOf(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "800", 0),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1),
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 5, "1200", 2));

        PositionValuation valuation = position.valuate(marketdata(PAMP, "1000", "990"));

        assertThat(valuation.instrument()).isEqualTo(PAMP);
        assertThat(valuation.quantity()).isEqualTo(15);
        assertThat(valuation.marketValue()).isEqualByComparingTo("15000.00");
        assertThat(valuation.totalReturnPct()).isEqualByComparingTo("17.65");
    }

    /**
     * Creates a PAMP position and applies the orders to it.
     */
    private InstrumentPosition positionOf(Order... orders) {
        InstrumentPosition position = new InstrumentPosition(PAMP);
        position.processOrders(List.of(orders));
        return position;
    }
}
