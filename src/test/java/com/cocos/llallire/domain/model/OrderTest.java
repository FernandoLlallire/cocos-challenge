package com.cocos.llallire.domain.model;

import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cocos.llallire.domain.exception.InvalidOrderDataException;
import com.cocos.llallire.domain.exception.OrderNotCancellableException;

/**
 * Tests of {@link Order}.
 */
class OrderTest {

    @Test
    void cancelChangesStatusOfNewOrderKeepingTheRestOfTheData() {
        Order order = order(PAMP, OrderSide.BUY, OrderStatus.NEW, 10, "900", 0);

        Order cancelled = order.cancel();

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled).usingRecursiveComparison().ignoringFields("status").isEqualTo(order);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "NEW", mode = EnumSource.Mode.EXCLUDE)
    void onlyNewOrdersCanBeCancelled(OrderStatus status) {
        Order order = order(PAMP, OrderSide.BUY, status, 10, "900", 0);

        assertThatThrownBy(order::cancel).isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    void notionalIsSizeTimesPrice() {
        assertThat(order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 3, "10.50", 0).notional())
                .isEqualByComparingTo("31.50");
    }

    @Test
    void notionalFailsWithInvalidPersistedData() {
        Order withoutPrice = new Order(1L, 47L, 1L, 10L, null, OrderType.MARKET, OrderSide.BUY, OrderStatus.FILLED,
                null);
        Order withZeroSize = new Order(2L, 47L, 1L, 0L, BigDecimal.TEN, OrderType.MARKET, OrderSide.BUY,
                OrderStatus.FILLED, null);

        assertThatThrownBy(withoutPrice::notional).isInstanceOf(InvalidOrderDataException.class);
        assertThatThrownBy(withZeroSize::notional).isInstanceOf(InvalidOrderDataException.class);
    }

    @Test
    void cashTransferSidesAreIdentified() {
        assertThat(OrderSide.CASH_IN.isCashTransfer()).isTrue();
        assertThat(OrderSide.CASH_OUT.isCashTransfer()).isTrue();
        assertThat(OrderSide.BUY.isCashTransfer()).isFalse();
        assertThat(OrderSide.SELL.isCashTransfer()).isFalse();
    }
}
