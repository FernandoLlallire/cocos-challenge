package com.cocos.llallire.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cache settings ({@code app.cache.*}), overridable with environment variables (see the README).
 *
 * @param type             cache implementation
 * @param failureCooldown  how long the cache is skipped after a cache error (see {@code CacheCircuitBreaker})
 * @param ttl              time to live of each cache region
 */
@ConfigurationProperties(prefix = "app.cache")
public record AppCacheProperties(
        @DefaultValue("redis") Type type,
        @DefaultValue("30s") Duration failureCooldown,
        @DefaultValue Ttl ttl) {

    /**
     * Available cache implementations.
     */
    public enum Type {
        /** Shared Redis cache (default; required when running more than one API instance). */
        REDIS,
        /** In-process cache without TTL; for tests and local work without Redis. */
        MEMORY,
        /** Cache disabled: every request goes to the database. */
        NONE
    }

    /**
     * Time to live per region.
     *
     * @param users               users by id
     * @param instruments         instruments by id
     * @param instrumentSearch    search results
     * @param marketdata          latest marketdata; bounds how old a cached price can be
     * @param portfolio           calculated portfolios; a safety net, since order changes invalidate them
     * @param portfolioGeneration portfolio generation tokens; must be longer than {@code portfolio}
     */
    public record Ttl(
            @DefaultValue("1h") Duration users,
            @DefaultValue("1h") Duration instruments,
            @DefaultValue("10m") Duration instrumentSearch,
            @DefaultValue("1m") Duration marketdata,
            @DefaultValue("5m") Duration portfolio,
            @DefaultValue("1d") Duration portfolioGeneration) {
    }
}
