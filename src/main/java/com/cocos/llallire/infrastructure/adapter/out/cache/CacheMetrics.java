package com.cocos.llallire.infrastructure.adapter.out.cache;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

/**
 * Cache metrics.
 *
 * <ul>
 *   <li>{@code cocos.cache.requests} — cache operations by {@code region}, {@code operation} (get, put, evict) and
 *       {@code result} (hit, miss, success, error, skipped). The hit ratio is {@code hit / (hit + miss)}.</li>
 *   <li>{@code cocos.cache.circuit_breaker.opened} — times the circuit breaker opened (cache outages).</li>
 * </ul>
 */
public class CacheMetrics {
    static final AttributeKey<String> REGION = AttributeKey.stringKey("region");
    static final AttributeKey<String> OPERATION = AttributeKey.stringKey("operation");
    static final AttributeKey<String> RESULT = AttributeKey.stringKey("result");

    private final LongCounter requests;
    private final LongCounter breakerOpened;

    /**
     * Creates the metrics.
     *
     * @param meter OpenTelemetry meter
     */
    public CacheMetrics(Meter meter) {
        this.requests = meter.counterBuilder("cocos.cache.requests")
                .setDescription("Cache operations by region, operation and result")
                .setUnit("{request}")
                .build();
        this.breakerOpened = meter.counterBuilder("cocos.cache.circuit_breaker.opened")
                .setDescription("Times the cache circuit breaker opened")
                .setUnit("{event}")
                .build();
    }

    /**
     * Metrics that record nothing; for tests and code that runs without telemetry.
     *
     * @return no-op metrics
     */
    public static CacheMetrics noop() {
        return new CacheMetrics(OpenTelemetry.noop().getMeter("noop"));
    }

    /**
     * Records one cache operation.
     *
     * @param region    cache region
     * @param operation get, put or evict
     * @param result    hit, miss, success, error or skipped
     */
    public void record(String region, String operation, String result) {
        requests.add(1, Attributes.of(REGION, region, OPERATION, operation, RESULT, result));
    }

    /**
     * Records that the circuit breaker opened.
     */
    public void recordBreakerOpened() {
        breakerOpened.add(1);
    }
}
