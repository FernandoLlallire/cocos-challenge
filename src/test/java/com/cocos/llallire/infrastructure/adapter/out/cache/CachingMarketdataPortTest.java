package com.cocos.llallire.infrastructure.adapter.out.cache;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.METR;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.marketdata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Marketdata;

/**
 * Tests of {@link CachingMarketdataPort}.
 */
class CachingMarketdataPortTest {

    private static final Marketdata PAMP_PRICE = marketdata(PAMP, "925.85", "921.80");
    private static final Marketdata METR_PRICE = marketdata(METR, "229.50", "232.00");

    private final LoadMarketdataPort database = mock(LoadMarketdataPort.class);
    private CachingMarketdataPort port;

    @BeforeEach
    void setUp() {
        port = new CachingMarketdataPort(database,
                new CacheRegion<>(CacheTestSupport.inMemoryCacheManager(), CacheNames.MARKETDATA, CacheTestSupport.breaker(), CacheMetrics.noop()));
    }

    @Test
    void singleLookupIsCached() {
        when(database.loadLastMarketdataByInstrumentId(PAMP.id())).thenReturn(Optional.of(PAMP_PRICE));

        port.loadLastMarketdataByInstrumentId(PAMP.id());

        assertThat(port.loadLastMarketdataByInstrumentId(PAMP.id())).contains(PAMP_PRICE);
        verify(database, times(1)).loadLastMarketdataByInstrumentId(PAMP.id());
    }

    @Test
    void absenceOfMarketdataIsCachedToo() {
        when(database.loadLastMarketdataByInstrumentId(ARS.id())).thenReturn(Optional.empty());

        assertThat(port.loadLastMarketdataByInstrumentId(ARS.id())).isEmpty();
        assertThat(port.loadLastMarketdataByInstrumentId(ARS.id())).isEmpty();

        verify(database, times(1)).loadLastMarketdataByInstrumentId(ARS.id());
    }

    @Test
    void batchLoadQueriesOnlyMissingIdsAndRemembersAbsentOnes() {
        when(database.loadLastMarketdataByInstrumentIds(List.of(PAMP.id(), METR.id(), ARS.id())))
                .thenReturn(Map.of(PAMP.id(), PAMP_PRICE, METR.id(), METR_PRICE));

        Map<Long, Marketdata> first = port.loadLastMarketdataByInstrumentIds(List.of(PAMP.id(), METR.id(), ARS.id()));
        Map<Long, Marketdata> second = port.loadLastMarketdataByInstrumentIds(List.of(PAMP.id(), METR.id(), ARS.id()));

        assertThat(first).isEqualTo(Map.of(PAMP.id(), PAMP_PRICE, METR.id(), METR_PRICE));
        assertThat(second).isEqualTo(first);
        verify(database, times(1)).loadLastMarketdataByInstrumentIds(List.of(PAMP.id(), METR.id(), ARS.id()));
        verify(database, never()).loadLastMarketdataByInstrumentId(ARS.id());
    }
}
