package com.cocos.llallire.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.cocos.llallire.domain.exception.InvalidOrderRequestException;

/**
 * Tests of the format validations of {@link OrderRequest}.
 */
class OrderRequestTest {

    @Test
    void acceptsMarketOrderBySizeAndLimitOrderByAmount() {
        assertThatCode(() -> new OrderRequest(1L, 47L, OrderSide.BUY, OrderType.MARKET, 10L, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> new OrderRequest(1L, 47L, OrderSide.BUY, OrderType.LIMIT, null, BigDecimal.TEN,
                BigDecimal.ONE)).doesNotThrowAnyException();
    }

    @Test
    void requiresMandatoryFields() {
        assertInvalid(null, 47L, OrderSide.BUY, OrderType.MARKET, 1L, null, null, "userId");
        assertInvalid(1L, null, OrderSide.BUY, OrderType.MARKET, 1L, null, null, "instrumentId");
        assertInvalid(1L, 47L, null, OrderType.MARKET, 1L, null, null, "side");
        assertInvalid(1L, 47L, OrderSide.BUY, null, 1L, null, null, "type");
    }

    @Test
    void requiresExactlyOneOfSizeOrAmount() {
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.MARKET, null, null, null, "exactly one");
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.MARKET, 1L, BigDecimal.TEN, null, "exactly one");
    }

    @Test
    void requiresPositiveSizeAndAmount() {
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.MARKET, 0L, null, null, "size must be positive");
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.MARKET, null, new BigDecimal("-5"), null,
                "amount must be positive");
    }

    @Test
    void limitOrdersRequirePriceAndMarketOrdersRejectIt() {
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.LIMIT, 1L, null, null, "LIMIT orders require");
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.LIMIT, 1L, null, BigDecimal.ZERO, "LIMIT orders require");
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.MARKET, 1L, null, BigDecimal.TEN, "MARKET orders must not");
    }

    @Test
    void limitPriceAcceptsAtMostTwoDecimals() {
        assertInvalid(1L, 47L, OrderSide.BUY, OrderType.LIMIT, 1L, null, new BigDecimal("900.123"),
                "at most 2 decimals");
        assertThatCode(() -> new OrderRequest(1L, 47L, OrderSide.BUY, OrderType.LIMIT, 1L, null,
                new BigDecimal("900.1200"))).doesNotThrowAnyException();
    }

    @Test
    void cashTransfersMustBeMarketOrders() {
        assertInvalid(1L, 66L, OrderSide.CASH_IN, OrderType.LIMIT, 1L, null, BigDecimal.ONE, "cash transfers");
    }

    /**
     * Checks that the request is rejected with a message containing the expected text.
     */
    private void assertInvalid(Long userId, Long instrumentId, OrderSide side, OrderType type, Long size,
            BigDecimal amount, BigDecimal price, String expectedMessage) {
        assertThatThrownBy(() -> new OrderRequest(userId, instrumentId, side, type, size, amount, price))
                .isInstanceOf(InvalidOrderRequestException.class)
                .hasMessageContaining(expectedMessage);
    }
}
