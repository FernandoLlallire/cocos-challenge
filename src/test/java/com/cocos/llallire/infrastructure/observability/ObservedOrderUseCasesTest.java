package com.cocos.llallire.infrastructure.observability;

import static com.cocos.llallire.infrastructure.observability.TelemetryTestSupport.doubleCounter;
import static com.cocos.llallire.infrastructure.observability.TelemetryTestSupport.longCounter;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

/**
 * Tests of {@link ObservedPlaceOrderUseCase}, {@link ObservedCancelOrderUseCase} and {@link OrderMetrics}.
 */
class ObservedOrderUseCasesTest {

    @RegisterExtension
    static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

    private static final PlaceOrderCommand COMMAND = new PlaceOrderCommand(1L, 47L, OrderSide.BUY, OrderType.MARKET,
            10L, null, null);

    private UseCaseTelemetry telemetry;
    private OrderMetrics metrics;

    @BeforeEach
    void setUp() {
        telemetry = new UseCaseTelemetry(OTEL.getOpenTelemetry());
        metrics = new OrderMetrics(OTEL.getOpenTelemetry());
    }

    @Test
    void placedOrderIsCountedByStatusAndItsAmountIsAdded() {
        new ObservedPlaceOrderUseCase(command -> order(OrderStatus.FILLED), telemetry, metrics).execute(COMMAND);

        assertThat(longCounter(OTEL.getMetrics(), "cocos.orders.placed", placedAttributes("FILLED"))).isEqualTo(1);
        assertThat(doubleCounter(OTEL.getMetrics(), "cocos.orders.amount", Attributes.of(OrderMetrics.SIDE, "BUY")))
                .isEqualTo(9258.5);
        assertThat(OTEL.getSpans().getFirst().getAttributes().get(AttributeKey.stringKey("order.status")))
                .isEqualTo("FILLED");
    }

    @Test
    void rejectedOrderIsCountedButAddsNoAmount() {
        new ObservedPlaceOrderUseCase(command -> order(OrderStatus.REJECTED), telemetry, metrics).execute(COMMAND);

        assertThat(longCounter(OTEL.getMetrics(), "cocos.orders.placed", placedAttributes("REJECTED")))
                .isEqualTo(1);
        assertThat(doubleCounter(OTEL.getMetrics(), "cocos.orders.amount", Attributes.of(OrderMetrics.SIDE, "BUY")))
                .isZero();
    }

    @Test
    void cancelledOrderIsCounted() {
        new ObservedCancelOrderUseCase(orderId -> order(OrderStatus.CANCELLED), telemetry, metrics).execute(5L);

        assertThat(longCounter(OTEL.getMetrics(), "cocos.orders.cancelled", Attributes.of(OrderMetrics.SIDE, "BUY")))
                .isEqualTo(1);
        assertThat(OTEL.getSpans().getFirst().getName()).isEqualTo("UseCase CancelOrder");
    }

    /**
     * Attributes of a MARKET BUY placed with the given status.
     */
    private Attributes placedAttributes(String status) {
        return Attributes.of(OrderMetrics.SIDE, "BUY", OrderMetrics.TYPE, "MARKET", OrderMetrics.STATUS, status);
    }

    /**
     * Builds a MARKET BUY of 10 PAMP at 925.85 with the given status.
     */
    private OrderInformation order(OrderStatus status) {
        return new OrderInformation(12L, 47L, 1L, OrderSide.BUY, OrderType.MARKET, 10L, new BigDecimal("925.85"),
                status, LocalDateTime.of(2026, 11, 19, 12, 0));
    }
}
