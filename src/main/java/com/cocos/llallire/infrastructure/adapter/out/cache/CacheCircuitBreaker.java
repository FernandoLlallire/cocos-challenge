package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circuit breaker shared by every {@link CacheRegion}.
 *
 * <p>When a cache operation fails (for example, Redis is down), the breaker opens for a cooldown period. While
 * it is open, cache operations are skipped immediately and requests go straight to the database, instead of
 * waiting for a timeout on every cache call. After the cooldown the next operation tries the cache again.</p>
 */
public class CacheCircuitBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheCircuitBreaker.class);

    private final Duration cooldown;
    private final Clock clock;
    private final CacheMetrics metrics;
    private final AtomicReference<Instant> openUntil = new AtomicReference<>(Instant.MIN);

    /**
     * Creates the breaker.
     *
     * @param cooldown how long the cache is skipped after a failure
     * @param clock    clock, injectable for tests
     * @param metrics  cache metrics (counts each opening)
     */
    public CacheCircuitBreaker(Duration cooldown, Clock clock, CacheMetrics metrics) {
        this.cooldown = cooldown;
        this.clock = clock;
        this.metrics = metrics;
    }

    /**
     * Tells whether cache operations should be attempted.
     *
     * @return {@code false} while the breaker is open
     */
    public boolean allowsRequests() {
        return !clock.instant().isBefore(openUntil.get());
    }

    /**
     * Records a cache failure and opens the breaker. Only the failure that opens it is logged with the stack
     * trace, to avoid flooding the logs during an outage.
     *
     * @param operation description of the failed operation
     * @param exception cause
     */
    public void recordFailure(String operation, RuntimeException exception) {
        Instant now = clock.instant();
        Instant previous = openUntil.getAndSet(now.plus(cooldown));
        if (!now.isBefore(previous)) {
            metrics.recordBreakerOpened();
            LOGGER.warn("Cache unavailable ({}); using the database only for the next {}", operation, cooldown,
                    exception);
        }
    }
}
