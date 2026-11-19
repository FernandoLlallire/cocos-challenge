package com.cocos.llallire.application.service;

import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.domain.exception.OrderNotCancellableException;
import com.cocos.llallire.domain.exception.OrderNotFoundException;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;

/**
 * Tests of {@link CancelOrder}.
 */
@ExtendWith(MockitoExtension.class)
class CancelOrderTest {

    @Mock
    private LoadOrderPort loadOrderPort;
    @Mock
    private SaveOrderPort saveOrderPort;
    private final RecordingPortfolioCache portfolioCache = new RecordingPortfolioCache();
    private CancelOrder cancelOrder;

    @BeforeEach
    void setUp() {
        cancelOrder = new CancelOrder(loadOrderPort, saveOrderPort, portfolioCache);
    }

    @Test
    void cancelsNewOrderAndPersistsIt() {
        Order order = order(PAMP, OrderSide.BUY, OrderStatus.NEW, 1, "900", 0);
        when(loadOrderPort.loadOrderById(order.id())).thenReturn(Optional.of(order));
        when(saveOrderPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(cancelOrder.execute(order.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(portfolioCache.invalidatedUsers).containsExactly(order.userId());
    }

    @Test
    void doesNotPersistWhenOrderIsNotNew() {
        Order order = order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 1, "900", 0);
        when(loadOrderPort.loadOrderById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancelOrder.execute(order.id())).isInstanceOf(OrderNotCancellableException.class);
        verify(saveOrderPort, never()).save(any());
        assertThat(portfolioCache.invalidatedUsers).isEmpty();
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        when(loadOrderPort.loadOrderById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelOrder.execute(99L)).isInstanceOf(OrderNotFoundException.class);
    }
}
