package com.cocos.llallire.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.port.in.CancelOrderUseCase;

import io.opentelemetry.api.trace.Span;

/**
 * Observability decorator of {@link CancelOrderUseCase}: span, duration, business metric and an audit log line.
 */
public class ObservedCancelOrderUseCase implements CancelOrderUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservedCancelOrderUseCase.class);

    private final CancelOrderUseCase delegate;
    private final UseCaseTelemetry telemetry;
    private final OrderMetrics metrics;

    /**
     * Creates the decorator.
     *
     * @param delegate  use case (already transactional)
     * @param telemetry span and duration helper
     * @param metrics   order metrics
     */
    public ObservedCancelOrderUseCase(CancelOrderUseCase delegate, UseCaseTelemetry telemetry, OrderMetrics metrics) {
        this.delegate = delegate;
        this.telemetry = telemetry;
        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderInformation execute(Long orderId) {
        return telemetry.observe("CancelOrder", () -> {
            Span.current().setAttribute("order.id", orderId);
            OrderInformation order = delegate.execute(orderId);
            metrics.recordCancelled(order);
            LOGGER.info("Order cancelled: id={} user={}", order.id(), order.userId());
            return order;
        });
    }
}
