package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Typed, fault-tolerant view over one Spring {@link Cache}.
 *
 * <p>The cache is an optimization, never a dependency: any cache error (for example, Redis is down) is treated
 * as a miss, so callers fall back to the database. Errors open the shared {@link CacheCircuitBreaker}, which
 * skips the cache for a while instead of paying a timeout on every call.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class CacheRegion<K, V> {
    private final Cache cache;
    private final CacheCircuitBreaker circuitBreaker;
    private final CacheMetrics metrics;
    private final String name;

    /**
     * Creates a region over the cache with the given name.
     *
     * @param cacheManager cache manager
     * @param name           region name (see {@link CacheNames})
     * @param circuitBreaker breaker shared by all regions
     * @param metrics        cache metrics
     * @throws IllegalStateException if the cache manager does not know the region
     */
    public CacheRegion(CacheManager cacheManager, String name, CacheCircuitBreaker circuitBreaker,
            CacheMetrics metrics) {
        this.cache = Optional.ofNullable(cacheManager.getCache(name))
                .orElseThrow(() -> new IllegalStateException("Cache region not configured: " + name));
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.name = name;
    }

    /**
     * Reads a value.
     *
     * @param key key
     * @return the cached value, or empty on a miss, a cache error or while the breaker is open
     */
    @SuppressWarnings("unchecked")
    public Optional<V> get(K key) {
        if (!circuitBreaker.allowsRequests()) {
            metrics.record(name, "get", "skipped");
            return Optional.empty();
        }
        try {
            Cache.ValueWrapper wrapper = cache.get(key);
            metrics.record(name, "get", wrapper == null ? "miss" : "hit");
            return wrapper == null ? Optional.empty() : Optional.ofNullable((V) wrapper.get());
        } catch (RuntimeException exception) {
            metrics.record(name, "get", "error");
            circuitBreaker.recordFailure("read " + name, exception);
            return Optional.empty();
        }
    }

    /**
     * Reads several values. Missing keys are not included in the result.
     *
     * @param keys keys
     * @return cached values indexed by key
     */
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, V> found = new HashMap<>();
        keys.forEach(key -> get(key).ifPresent(value -> found.put(key, value)));
        return found;
    }

    /**
     * Stores a value. Errors are ignored (they only open the breaker); skipped while the breaker is open.
     *
     * @param key   key
     * @param value value; must not be {@code null}
     * @return {@code true} if the value was written
     */
    public boolean put(K key, V value) {
        if (!circuitBreaker.allowsRequests()) {
            metrics.record(name, "put", "skipped");
            return false;
        }
        try {
            cache.put(key, value);
            metrics.record(name, "put", "success");
            return true;
        } catch (RuntimeException exception) {
            metrics.record(name, "put", "error");
            circuitBreaker.recordFailure("write " + name, exception);
            return false;
        }
    }

    /**
     * Removes a value. Errors are ignored (they only open the breaker); skipped while the breaker is open.
     *
     * @param key key
     */
    public void evict(K key) {
        if (!circuitBreaker.allowsRequests()) {
            metrics.record(name, "evict", "skipped");
            return;
        }
        try {
            cache.evict(key);
            metrics.record(name, "evict", "success");
        } catch (RuntimeException exception) {
            metrics.record(name, "evict", "error");
            circuitBreaker.recordFailure("evict " + name, exception);
        }
    }
}
