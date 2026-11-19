package com.cocos.llallire.infrastructure.observability;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.domain.model.OrderStatus;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

/**
 * Business metrics of orders.
 *
 * <ul>
 *   <li>{@code cocos.orders.placed} — orders placed, by {@code side}, {@code type} and {@code status};</li>
 *   <li>{@code cocos.orders.cancelled} — orders cancelled, by {@code side};</li>
 *   <li>{@code cocos.orders.amount} — pesos of accepted (FILLED or NEW) orders, by {@code side}.</li>
 * </ul>
 */
public class OrderMetrics {
    static final AttributeKey<String> SIDE = AttributeKey.stringKey("side");
    static final AttributeKey<String> TYPE = AttributeKey.stringKey("type");
    static final AttributeKey<String> STATUS = AttributeKey.stringKey("status");

    private final LongCounter placed;
    private final LongCounter cancelled;
    private final DoubleCounter amount;

    /**
     * Creates the metrics.
     *
     * @param openTelemetry OpenTelemetry instance
     */
    public OrderMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter(TelemetryNames.INSTRUMENTATION_SCOPE);
        this.placed = meter.counterBuilder("cocos.orders.placed")
                .setDescription("Orders placed, by side, type and resulting status")
                .setUnit("{order}")
                .build();
        this.cancelled = meter.counterBuilder("cocos.orders.cancelled")
                .setDescription("Orders cancelled by the user")
                .setUnit("{order}")
                .build();
        this.amount = meter.counterBuilder("cocos.orders.amount")
                .ofDoubles()
                .setDescription("Amount in pesos of accepted orders (FILLED or NEW)")
                .setUnit("ARS")
                .build();
    }

    /**
     * Records a placed order.
     *
     * @param order the order as persisted
     */
    public void recordPlaced(OrderInformation order) {
        placed.add(1, Attributes.of(SIDE, order.side().name(), TYPE, order.type().name(),
                STATUS, order.status().name()));
        if (order.status() != OrderStatus.REJECTED) {
            amount.add(order.price().doubleValue() * order.size(), Attributes.of(SIDE, order.side().name()));
        }
    }

    /**
     * Records a cancelled order.
     *
     * @param order the cancelled order
     */
    public void recordCancelled(OrderInformation order) {
        cancelled.add(1, Attributes.of(SIDE, order.side().name()));
    }
}
