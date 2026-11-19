package com.cocos.llallire.infrastructure.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.domain.exception.UserNotFoundException;

/**
 * Tests of {@link PortfolioCacheAdapter}.
 */
class PortfolioCacheAdapterTest {

    private static final PortfolioInformation OLD = portfolio("100");
    private static final PortfolioInformation NEW = portfolio("200");

    private PortfolioCacheAdapter cache;
    private final AtomicInteger loads = new AtomicInteger();

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = CacheTestSupport.inMemoryCacheManager();
        cache = new PortfolioCacheAdapter(
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO, CacheTestSupport.breaker(), CacheMetrics.noop()),
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO_GENERATION, CacheTestSupport.breaker(), CacheMetrics.noop()));
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void computesOnceAndThenServesFromCache() {
        assertThat(cache.getOrCompute(1L, () -> load(OLD))).isEqualTo(OLD);
        assertThat(cache.getOrCompute(1L, () -> load(NEW))).isEqualTo(OLD);

        assertThat(loads).hasValue(1);
    }

    @Test
    void invalidationForcesARecalculationOnlyForThatUser() {
        cache.getOrCompute(1L, () -> load(OLD));
        cache.getOrCompute(2L, () -> load(OLD));

        cache.invalidate(1L);

        assertThat(cache.getOrCompute(1L, () -> load(NEW))).isEqualTo(NEW);
        assertThat(cache.getOrCompute(2L, () -> load(NEW))).isEqualTo(OLD);
        assertThat(loads).hasValue(3);
    }

    @Test
    void readerThatLoadedBeforeTheInvalidationCannotLeaveStaleDataReachable() {
        // A reader computes the portfolio; while it is loading, an order invalidates the cache.
        PortfolioInformation stale = cache.getOrCompute(1L, () -> {
            cache.invalidate(1L);
            return load(OLD);
        });

        assertThat(stale).isEqualTo(OLD);
        assertThat(cache.getOrCompute(1L, () -> load(NEW))).isEqualTo(NEW);
    }

    @Test
    void insideATransactionTheInvalidationIsRepeatedAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        cache.invalidate(1L);
        // Between the invalidation and the commit, a reader still sees pre-commit data and caches it.
        cache.getOrCompute(1L, () -> load(OLD));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(cache.getOrCompute(1L, () -> load(NEW))).isEqualTo(NEW);
    }

    @Test
    void loaderErrorsArePropagatedAndNothingIsCached() {
        assertThatThrownBy(() -> cache.getOrCompute(9L, () -> {
            throw new UserNotFoundException(9L);
        })).isInstanceOf(UserNotFoundException.class);

        assertThat(cache.getOrCompute(9L, () -> load(NEW))).isEqualTo(NEW);
    }

    @Test
    void invalidationThatCannotBeWrittenIsRetriedAndStaleDataIsNeverServed() {
        // Generations live in a cache that can be "disconnected" without losing its data (network partition).
        FlakyCache generationsCache = new FlakyCache(CacheNames.PORTFOLIO_GENERATION);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(CacheNames.PORTFOLIO)).thenReturn(new ConcurrentMapCache(CacheNames.PORTFOLIO));
        when(cacheManager.getCache(CacheNames.PORTFOLIO_GENERATION)).thenReturn(generationsCache);
        MutableClock clock = new MutableClock();
        CacheCircuitBreaker breaker = new CacheCircuitBreaker(Duration.ofSeconds(30), clock, CacheMetrics.noop());
        PortfolioCacheAdapter adapter = new PortfolioCacheAdapter(
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO, breaker, CacheMetrics.noop()),
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO_GENERATION, breaker, CacheMetrics.noop()));
        adapter.getOrCompute(1L, () -> load(OLD));

        generationsCache.connected = false;
        adapter.invalidate(1L);
        generationsCache.connected = true;

        assertThat(adapter.getOrCompute(1L, () -> load(NEW)))
                .as("while the breaker is open the invalidation is pending and the cache is bypassed")
                .isEqualTo(NEW);
        clock.advance(Duration.ofSeconds(31));
        assertThat(adapter.getOrCompute(1L, () -> load(NEW)))
                .as("once the cache is reachable the pending invalidation is written first")
                .isEqualTo(NEW);
        assertThat(adapter.getOrCompute(1L, () -> load(OLD))).isEqualTo(NEW);
    }

    /**
     * Counts the call and returns the given portfolio.
     */
    private PortfolioInformation load(PortfolioInformation portfolio) {
        loads.incrementAndGet();
        return portfolio;
    }

    /**
     * Builds a portfolio with the given total value.
     */
    private static PortfolioInformation portfolio(String total) {
        return new PortfolioInformation(new BigDecimal(total), BigDecimal.ZERO, List.of());
    }

    /**
     * In-memory cache that keeps its data while "disconnected" and fails every operation in that state.
     */
    private static final class FlakyCache extends ConcurrentMapCache {
        boolean connected = true;

        FlakyCache(String name) {
            super(name);
        }

        @Override
        public ValueWrapper get(Object key) {
            check();
            return super.get(key);
        }

        @Override
        public void put(Object key, Object value) {
            check();
            super.put(key, value);
        }

        private void check() {
            if (!connected) {
                throw new IllegalStateException("cache unreachable");
            }
        }
    }

    /**
     * Clock that tests can move forward.
     */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
