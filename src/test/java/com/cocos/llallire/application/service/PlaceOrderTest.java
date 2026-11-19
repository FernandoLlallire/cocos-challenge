package com.cocos.llallire.application.service;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.USER_ID;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.marketdata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.domain.exception.InstrumentNotFoundException;
import com.cocos.llallire.domain.exception.InvalidOrderRequestException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.exception.UserNotFoundException;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.domain.service.OrderPlacementService;

/**
 * Tests of {@link PlaceOrder}. Uses the real {@link OrderPlacementService} and mocks the outbound ports.
 */
@ExtendWith(MockitoExtension.class)
class PlaceOrderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2023-07-14T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LoadUserPort userPort;
    @Mock
    private LoadInstrumentPort instrumentPort;
    @Mock
    private LoadMarketdataPort marketdataPort;
    @Mock
    private LoadOrderPort loadOrderPort;
    @Mock
    private SaveOrderPort saveOrderPort;

    private final RecordingPortfolioCache portfolioCache = new RecordingPortfolioCache();
    private PlaceOrder placeOrder;

    @BeforeEach
    void setUp() {
        placeOrder = new PlaceOrder(userPort, instrumentPort, marketdataPort, loadOrderPort, saveOrderPort,
                new OrderPlacementService(), portfolioCache, CLOCK);
    }

    @Test
    void marketBuyLocksUserUsesLastCloseAndPersistsFilledOrder() {
        givenUserAndInstrument(PAMP);
        when(marketdataPort.loadLastMarketdataByInstrumentId(PAMP.id()))
                .thenReturn(Optional.of(marketdata(PAMP, "1000", "990")));
        when(loadOrderPort.loadOrdersByUserIdAndStatuses(USER_ID, OrderStatus.affectingHoldings()))
                .thenReturn(List.of(cashIn(10_000)));
        when(saveOrderPort.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        OrderInformation result = placeOrder.execute(command(OrderSide.BUY, OrderType.MARKET, 3L, null));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(saveOrderPort).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(OrderStatus.FILLED);
        assertThat(saved.getValue().price()).isEqualByComparingTo("1000");
        assertThat(saved.getValue().dateTime()).isEqualTo(LocalDateTime.of(2023, 7, 14, 15, 0));
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        verify(userPort).loadUserByIdForUpdate(USER_ID);
        assertThat(portfolioCache.invalidatedUsers).containsExactly(USER_ID);
    }

    @Test
    void rejectedOrdersArePersistedToo() {
        givenUserAndInstrument(PAMP);
        when(marketdataPort.loadLastMarketdataByInstrumentId(PAMP.id()))
                .thenReturn(Optional.of(marketdata(PAMP, "1000", "990")));
        when(loadOrderPort.loadOrdersByUserIdAndStatuses(any(), any())).thenReturn(List.of());
        when(saveOrderPort.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 101L));

        OrderInformation result = placeOrder.execute(command(OrderSide.BUY, OrderType.MARKET, 1L, null));

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(portfolioCache.invalidatedUsers)
                .as("a rejected order does not change the portfolio, so the cache is kept")
                .isEmpty();
    }

    @Test
    void limitOrdersAndCashTransfersDoNotNeedMarketdata() {
        givenUserAndInstrument(PAMP);
        when(loadOrderPort.loadOrdersByUserIdAndStatuses(any(), any())).thenReturn(List.of(cashIn(10_000)));
        when(saveOrderPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderInformation result = placeOrder.execute(command(OrderSide.BUY, OrderType.LIMIT, 1L, "950"));

        assertThat(result.status()).isEqualTo(OrderStatus.NEW);
        verify(marketdataPort, never()).loadLastMarketdataByInstrumentId(any());
        assertThat(portfolioCache.invalidatedUsers).containsExactly(USER_ID);
    }

    @Test
    void invalidRequestFailsBeforeTouchingAnyPort() {
        assertThatThrownBy(() -> placeOrder.execute(command(OrderSide.BUY, OrderType.LIMIT, 1L, null)))
                .isInstanceOf(InvalidOrderRequestException.class);
        verifyNoInteractions(userPort, instrumentPort, marketdataPort, loadOrderPort, saveOrderPort);
    }

    @Test
    void unknownUserInstrumentOrMarketdataFail() {
        when(userPort.loadUserByIdForUpdate(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeOrder.execute(command(OrderSide.BUY, OrderType.MARKET, 1L, null)))
                .isInstanceOf(UserNotFoundException.class);

        when(userPort.loadUserByIdForUpdate(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "a@b.com", "1")));
        when(instrumentPort.loadInstrumentById(PAMP.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeOrder.execute(command(OrderSide.BUY, OrderType.MARKET, 1L, null)))
                .isInstanceOf(InstrumentNotFoundException.class);

        when(instrumentPort.loadInstrumentById(PAMP.id())).thenReturn(Optional.of(PAMP));
        when(marketdataPort.loadLastMarketdataByInstrumentId(PAMP.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeOrder.execute(command(OrderSide.BUY, OrderType.MARKET, 1L, null)))
                .isInstanceOf(MarketDataNotFoundException.class);

        verify(saveOrderPort, never()).save(any());
        assertThat(portfolioCache.invalidatedUsers).isEmpty();
    }

    @Test
    void cashInIsFilled() {
        givenUserAndInstrument(ARS);
        when(loadOrderPort.loadOrdersByUserIdAndStatuses(any(), any())).thenReturn(List.of());
        when(saveOrderPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderInformation result = placeOrder.execute(new PlaceOrderCommand(USER_ID, ARS.id(), OrderSide.CASH_IN,
                OrderType.MARKET, 5000L, null, null));

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.size()).isEqualTo(5000);
    }

    /**
     * Sets up the ports so the user and the instrument exist.
     */
    private void givenUserAndInstrument(com.cocos.llallire.domain.model.Instrument instrument) {
        when(userPort.loadUserByIdForUpdate(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "a@b.com", "1")));
        when(instrumentPort.loadInstrumentById(instrument.id())).thenReturn(Optional.of(instrument));
    }

    /**
     * Creates a command on PAMP.
     */
    private PlaceOrderCommand command(OrderSide side, OrderType type, Long size, String price) {
        return new PlaceOrderCommand(USER_ID, PAMP.id(), side, type, size, null,
                price == null ? null : new BigDecimal(price));
    }

    /**
     * Simulates the id assigned by the database on save.
     */
    private Order withId(Order order, Long id) {
        return new Order(id, order.instrumentId(), order.userId(), order.size(), order.price(), order.type(),
                order.side(), order.status(), order.dateTime());
    }
}
