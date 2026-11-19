package com.cocos.llallire.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.application.port.in.PlaceOrderUseCase;

import io.opentelemetry.api.trace.Span;

/**
 * Observability decorator of {@link PlaceOrderUseCase}: span, duration, business metrics and an audit log line.
 * The use case itself is unaware of it.
 */
public class ObservedPlaceOrderUseCase implements PlaceOrderUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservedPlaceOrderUseCase.class);

    private final PlaceOrderUseCase delegate;
    private final UseCaseTelemetry telemetry;
    private final OrderMetrics metrics;

    /**
     * Creates the decorator.
     *
     * @param delegate  use case (already transactional)
     * @param telemetry span and duration helper
     * @param metrics   order metrics
     */
    public ObservedPlaceOrderUseCase(PlaceOrderUseCase delegate, UseCaseTelemetry telemetry, OrderMetrics metrics) {
        this.delegate = delegate;
        this.telemetry = telemetry;
        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the order id, side, type and status to the span, records the metrics and logs the result.</p>
     */
    @Override
    public OrderInformation execute(PlaceOrderCommand command) {
        return telemetry.observe("PlaceOrder", () -> {
            OrderInformation order = delegate.execute(command);
            Span.current()
                    .setAttribute("order.id", order.id())
                    .setAttribute("order.side", order.side().name())
                    .setAttribute("order.type", order.type().name())
                    .setAttribute("order.status", order.status().name());
            metrics.recordPlaced(order);
            LOGGER.info("Order placed: id={} user={} instrument={} {} {} size={} price={} status={}", order.id(),
                    order.userId(), order.instrumentId(), order.side(), order.type(), order.size(), order.price(),
                    order.status());
            return order;
        });
    }
}
