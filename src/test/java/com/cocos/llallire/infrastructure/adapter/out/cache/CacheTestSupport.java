package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.time.Clock;
import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Helpers for cache unit tests.
 */
final class CacheTestSupport {

    /**
     * Utility class; not instantiable.
     */
    private CacheTestSupport() {
    }

    /**
     * Creates an in-memory cache manager with every region of the application.
     *
     * @return fresh cache manager
     */
    static CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(CacheNames.ALL.toArray(String[]::new));
    }

    /**
     * Creates a closed circuit breaker with a 30 second cooldown.
     *
     * @return fresh breaker
     */
    static CacheCircuitBreaker breaker() {
        return new CacheCircuitBreaker(Duration.ofSeconds(30), Clock.systemUTC(), CacheMetrics.noop());
    }
}
