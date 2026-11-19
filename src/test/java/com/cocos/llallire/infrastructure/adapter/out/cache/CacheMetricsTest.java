package com.cocos.llallire.infrastructure.adapter.out.cache;

import static com.cocos.llallire.infrastructure.observability.TelemetryTestSupport.longCounter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

/**
 * Tests of the metrics recorded by {@link CacheRegion} and {@link CacheCircuitBreaker}.
 */
class CacheMetricsTest {

    @RegisterExtension
    static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

    private CacheMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new CacheMetrics(OTEL.getOpenTelemetry().getMeter("test"));
    }

    @Test
    void countsHitsMissesAndWritesPerRegion() {
        CacheRegion<Long, String> region = new CacheRegion<>(CacheTestSupport.inMemoryCacheManager(),
                CacheNames.USERS, CacheTestSupport.breaker(), metrics);

        region.get(1L);
        region.put(1L, "a");
        region.get(1L);
        region.get(1L);

        assertThat(count(CacheNames.USERS, "get", "miss")).isEqualTo(1);
        assertThat(count(CacheNames.USERS, "get", "hit")).isEqualTo(2);
        assertThat(count(CacheNames.USERS, "put", "success")).isEqualTo(1);
    }

    @Test
    void countsErrorsSkippedOperationsAndBreakerOpenings() {
        Cache broken = mock(Cache.class);
        when(broken.get(any())).thenThrow(new IllegalStateException("redis down"));
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(broken);
        CacheCircuitBreaker breaker = new CacheCircuitBreaker(Duration.ofSeconds(30), Clock.systemUTC(), metrics);
        CacheRegion<Long, String> region = new CacheRegion<>(cacheManager, CacheNames.USERS, breaker, metrics);

        region.get(1L);
        region.get(1L);

        assertThat(count(CacheNames.USERS, "get", "error")).isEqualTo(1);
        assertThat(count(CacheNames.USERS, "get", "skipped")).isEqualTo(1);
        assertThat(longCounter(OTEL.getMetrics(), "cocos.cache.circuit_breaker.opened", Attributes.empty()))
                .isEqualTo(1);
    }

    /**
     * Value of {@code cocos.cache.requests} for the given attributes.
     */
    private long count(String region, String operation, String result) {
        return longCounter(OTEL.getMetrics(), "cocos.cache.requests", Attributes.of(CacheMetrics.REGION, region,
                CacheMetrics.OPERATION, operation, CacheMetrics.RESULT, result));
    }
}
