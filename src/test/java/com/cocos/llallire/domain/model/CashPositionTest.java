package com.cocos.llallire.domain.model;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link CashPosition}.
 */
class CashPositionTest {

    @Test
    void balanceAddsCashInAndSellsAndSubtractsCashOutAndBuys() {
        CashPosition cash = new CashPosition();
        cash.processOrders(List.of(
                cashIn(10_000),
                order(ARS, OrderSide.CASH_OUT, OrderStatus.FILLED, 1_000, "1", 1),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 5, "900", 2),
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 2, "950", 3)));

        assertThat(cash.balance()).isEqualByComparingTo("6400");
        assertThat(cash.reserved()).isEqualByComparingTo("0");
        assertThat(cash.available()).isEqualByComparingTo("6400");
    }

    @Test
    void pendingBuyOrdersReserveCashButPendingSellsDoNot() {
        CashPosition cash = new CashPosition();
        cash.processOrders(List.of(
                cashIn(10_000),
                order(PAMP, OrderSide.BUY, OrderStatus.NEW, 3, "1000", 1),
                order(PAMP, OrderSide.SELL, OrderStatus.NEW, 3, "1000", 2)));

        assertThat(cash.balance()).isEqualByComparingTo("10000");
        assertThat(cash.reserved()).isEqualByComparingTo("3000");
        assertThat(cash.available()).isEqualByComparingTo("7000");
    }

    @Test
    void rejectedAndCancelledOrdersAreIgnored() {
        CashPosition cash = new CashPosition();
        cash.processOrders(List.of(
                cashIn(1_000),
                order(PAMP, OrderSide.BUY, OrderStatus.REJECTED, 100, "900", 1),
                order(PAMP, OrderSide.BUY, OrderStatus.CANCELLED, 1, "900", 2)));

        assertThat(cash.available()).isEqualByComparingTo("1000");
    }

    @Test
    void amountsAreReturnedWithTwoDecimals() {
        CashPosition cash = new CashPosition();

        assertThat(cash.available().scale()).isEqualTo(2);
    }
}
