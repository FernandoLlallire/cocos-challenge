package com.cocos.llallire.domain.service;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.T0;
import static com.cocos.llallire.TestData.USER_ID;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cocos.llallire.domain.exception.InvalidOrderRequestException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderRequest;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;

/**
 * Tests of {@link OrderPlacementService}.
 */
class OrderPlacementServiceTest {

    private static final BigDecimal MARKET_PRICE = new BigDecimal("1000");
    /** 10,000 pesos and 10 PAMP shares (bought with 9,000 from a CASH_IN of 19,000). */
    private static final List<Order> ORDERS = List.of(
            cashIn(19_000),
            order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1));

    private final OrderPlacementService service = new OrderPlacementService();

    @Test
    void marketBuyIsFilledAtLastPrice() {
        Order order = place(request(OrderSide.BUY, OrderType.MARKET, 5L, null, null), PAMP);

        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.price()).isEqualByComparingTo("1000");
        assertThat(order.size()).isEqualTo(5);
        assertThat(order.id()).isNull();
        assertThat(order.userId()).isEqualTo(USER_ID);
        assertThat(order.instrumentId()).isEqualTo(PAMP.id());
        assertThat(order.dateTime()).isEqualTo(T0);
    }

    @Test
    void limitOrderIsNewAtRequestedPrice() {
        Order order = place(request(OrderSide.BUY, OrderType.LIMIT, 5L, null, "950"), PAMP);

        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.price()).isEqualTo(new BigDecimal("950.00"));
        assertThat(order.type()).isEqualTo(OrderType.LIMIT);
    }

    @Test
    void amountIsConvertedToTheMaximumWholeNumberOfShares() {
        Order order = place(request(OrderSide.BUY, OrderType.MARKET, null, "9999.99", null), PAMP);

        assertThat(order.size()).isEqualTo(9);
        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void amountBelowOneShareIsRejected() {
        Order order = place(request(OrderSide.BUY, OrderType.MARKET, null, "999", null), PAMP);

        assertThat(order.size()).isZero();
        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void buyUsingExactlyTheAvailableCashIsAccepted() {
        assertThat(place(request(OrderSide.BUY, OrderType.MARKET, 10L, null, null), PAMP).status())
                .isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void buyAboveAvailableCashIsRejected() {
        assertThat(place(request(OrderSide.BUY, OrderType.MARKET, 11L, null, null), PAMP).status())
                .isEqualTo(OrderStatus.REJECTED);
        assertThat(place(request(OrderSide.BUY, OrderType.LIMIT, 1L, null, "10000.01"), PAMP).status())
                .isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void pendingBuysReduceAvailableCash() {
        List<Order> orders = List.of(
                cashIn(10_000),
                order(PAMP, OrderSide.BUY, OrderStatus.NEW, 5, "1000", 1));

        Order order = service.place(request(OrderSide.BUY, OrderType.MARKET, 6L, null, null), PAMP, MARKET_PRICE,
                orders, T0);

        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void sellUpToHeldSharesIsAcceptedAndAboveIsRejected() {
        assertThat(place(request(OrderSide.SELL, OrderType.MARKET, 10L, null, null), PAMP).status())
                .isEqualTo(OrderStatus.FILLED);
        assertThat(place(request(OrderSide.SELL, OrderType.MARKET, 11L, null, null), PAMP).status())
                .isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void pendingSellsReduceAvailableShares() {
        List<Order> orders = List.of(
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 0),
                order(PAMP, OrderSide.SELL, OrderStatus.NEW, 8, "1100", 1));

        Order order = service.place(request(OrderSide.SELL, OrderType.LIMIT, 3L, null, "1200"), PAMP, null,
                orders, T0);

        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void sellByAmountUsesTheMaximumWholeNumberOfShares() {
        Order order = place(request(OrderSide.SELL, OrderType.MARKET, null, "5500", null), PAMP);

        assertThat(order.size()).isEqualTo(5);
        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void cashInIsFilledWithUnitPrice() {
        Order order = place(request(OrderSide.CASH_IN, OrderType.MARKET, null, "5000", null), ARS);

        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.price()).isEqualByComparingTo("1");
        assertThat(order.size()).isEqualTo(5000);
    }

    @Test
    void cashOutAboveAvailableCashIsRejected() {
        assertThat(place(request(OrderSide.CASH_OUT, OrderType.MARKET, 10_000L, null, null), ARS).status())
                .isEqualTo(OrderStatus.FILLED);
        assertThat(place(request(OrderSide.CASH_OUT, OrderType.MARKET, 10_001L, null, null), ARS).status())
                .isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void sideMustMatchInstrumentType() {
        assertThatThrownBy(() -> place(request(OrderSide.BUY, OrderType.MARKET, 1L, null, null), ARS))
                .isInstanceOf(InvalidOrderRequestException.class);
        assertThatThrownBy(() -> place(request(OrderSide.CASH_IN, OrderType.MARKET, 1L, null, null), PAMP))
                .isInstanceOf(InvalidOrderRequestException.class);
    }

    @Test
    void marketOrderWithoutMarketPriceFails() {
        OrderRequest request = request(OrderSide.BUY, OrderType.MARKET, 1L, null, null);

        assertThatThrownBy(() -> service.place(request, PAMP, null, ORDERS, T0))
                .isInstanceOf(MarketDataNotFoundException.class);
    }

    /**
     * Sends the request with the default market price and orders.
     */
    private Order place(OrderRequest request, Instrument instrument) {
        return service.place(request, instrument, MARKET_PRICE, ORDERS, T0);
    }

    /**
     * Creates a request of the test user on PAMP or ARS depending on the side.
     */
    private OrderRequest request(OrderSide side, OrderType type, Long size, String amount, String price) {
        Long instrumentId = side.isCashTransfer() ? ARS.id() : PAMP.id();
        return new OrderRequest(USER_ID, instrumentId, side, type, size,
                amount == null ? null : new BigDecimal(amount), price == null ? null : new BigDecimal(price));
    }
}
