package com.cocos.llallire.infrastructure.adapter.out.cache;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.METR;
import static com.cocos.llallire.TestData.PAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import com.cocos.llallire.application.port.out.LoadInstrumentPort;

/**
 * Tests of {@link CachingInstrumentPort}.
 */
class CachingInstrumentPortTest {

    private final LoadInstrumentPort database = mock(LoadInstrumentPort.class);
    private CachingInstrumentPort port;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = CacheTestSupport.inMemoryCacheManager();
        port = new CachingInstrumentPort(database,
                new CacheRegion<>(cacheManager, CacheNames.INSTRUMENTS, CacheTestSupport.breaker(), CacheMetrics.noop()),
                new CacheRegion<>(cacheManager, CacheNames.INSTRUMENT_SEARCH, CacheTestSupport.breaker(), CacheMetrics.noop()));
    }

    @Test
    void equivalentSearchesShareOneCacheEntry() {
        when(database.searchByTickerOrName("pamp")).thenReturn(List.of(PAMP));

        assertThat(port.searchByTickerOrName("pamp")).containsExactly(PAMP);
        assertThat(port.searchByTickerOrName("  PAMP ")).containsExactly(PAMP);

        verify(database, times(1)).searchByTickerOrName("pamp");
    }

    @Test
    void emptySearchResultsAreCached() {
        when(database.searchByTickerOrName("zzz")).thenReturn(List.of());

        port.searchByTickerOrName("zzz");
        port.searchByTickerOrName("zzz");

        verify(database, times(1)).searchByTickerOrName("zzz");
    }

    @Test
    void instrumentByIdIsCached() {
        when(database.loadInstrumentById(PAMP.id())).thenReturn(Optional.of(PAMP));

        port.loadInstrumentById(PAMP.id());

        assertThat(port.loadInstrumentById(PAMP.id())).contains(PAMP);
        verify(database, times(1)).loadInstrumentById(PAMP.id());
    }

    @Test
    void batchLoadQueriesOnlyTheMissingIds() {
        when(database.loadInstrumentById(PAMP.id())).thenReturn(Optional.of(PAMP));
        port.loadInstrumentById(PAMP.id());
        when(database.loadInstrumentsByIds(List.of(METR.id(), ARS.id()))).thenReturn(List.of(METR, ARS));

        assertThat(port.loadInstrumentsByIds(List.of(PAMP.id(), METR.id(), ARS.id())))
                .containsExactlyInAnyOrder(PAMP, METR, ARS);
        assertThat(port.loadInstrumentsByIds(List.of(PAMP.id(), METR.id(), ARS.id())))
                .containsExactlyInAnyOrder(PAMP, METR, ARS);

        verify(database, times(1)).loadInstrumentsByIds(List.of(METR.id(), ARS.id()));
    }
}
