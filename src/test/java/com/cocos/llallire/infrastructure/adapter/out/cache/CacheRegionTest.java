package com.cocos.llallire.infrastructure.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Tests of {@link CacheRegion}.
 */
class CacheRegionTest {

    @Test
    void storesReadsAndEvictsValues() {
        CacheRegion<Long, String> region = new CacheRegion<>(CacheTestSupport.inMemoryCacheManager(),
                CacheNames.PORTFOLIO_GENERATION, CacheTestSupport.breaker(), CacheMetrics.noop());

        region.put(1L, "a");
        region.put(2L, "b");

        assertThat(region.get(1L)).contains("a");
        assertThat(region.getAll(List.of(1L, 2L, 3L))).isEqualTo(Map.of(1L, "a", 2L, "b"));
        region.evict(1L);
        assertThat(region.get(1L)).isEmpty();
    }

    @Test
    void cacheFailuresAreTreatedAsMissesAndNeverPropagate() {
        Cache broken = mock(Cache.class);
        when(broken.getName()).thenReturn("broken");
        when(broken.get(any())).thenThrow(new IllegalStateException("redis down"));
        doThrow(new IllegalStateException("redis down")).when(broken).put(any(), any());
        doThrow(new IllegalStateException("redis down")).when(broken).evict(any());
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("broken")).thenReturn(broken);
        CacheRegion<Long, String> region = new CacheRegion<>(cacheManager, "broken", CacheTestSupport.breaker(), CacheMetrics.noop());

        assertThat(region.get(1L)).isEmpty();
        assertThatCode(() -> region.put(1L, "a")).doesNotThrowAnyException();
        assertThatCode(() -> region.evict(1L)).doesNotThrowAnyException();
    }

    @Test
    void afterAFailureTheCacheIsSkippedUntilTheCooldownEnds() {
        Cache broken = mock(Cache.class);
        when(broken.getName()).thenReturn("broken");
        when(broken.get(any())).thenThrow(new IllegalStateException("redis down"));
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("broken")).thenReturn(broken);
        MutableClock clock = new MutableClock();
        CacheCircuitBreaker breaker = new CacheCircuitBreaker(Duration.ofSeconds(30), clock, CacheMetrics.noop());
        CacheRegion<Long, String> region = new CacheRegion<>(cacheManager, "broken", breaker, CacheMetrics.noop());

        region.get(1L);
        region.get(1L);
        region.put(1L, "a");
        verify(broken, times(1)).get(any());
        verify(broken, times(0)).put(any(), any());

        clock.advance(Duration.ofSeconds(31));
        region.get(1L);
        verify(broken, times(2)).get(any());
    }

    @Test
    void breakerIsSharedBetweenRegions() {
        Cache broken = mock(Cache.class);
        when(broken.getName()).thenReturn("broken");
        when(broken.get(any())).thenThrow(new IllegalStateException("redis down"));
        CacheManager cacheManager = CacheTestSupport.inMemoryCacheManager();
        CacheManager brokenManager = mock(CacheManager.class);
        when(brokenManager.getCache("broken")).thenReturn(broken);
        CacheCircuitBreaker breaker = CacheTestSupport.breaker();
        CacheRegion<Long, String> failing = new CacheRegion<>(brokenManager, "broken", breaker, CacheMetrics.noop());
        CacheRegion<Long, String> healthy = new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO_GENERATION, breaker, CacheMetrics.noop());
        healthy.put(1L, "a");

        failing.get(1L);

        assertThat(healthy.get(1L)).as("skipped while the breaker is open").isEmpty();
    }

    @Test
    void unknownRegionFailsFast() {
        assertThatThrownBy(() -> new CacheRegion<>(CacheTestSupport.inMemoryCacheManager(), "unknown", CacheTestSupport.breaker(), CacheMetrics.noop()))
                .isInstanceOf(IllegalStateException.class);
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
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
