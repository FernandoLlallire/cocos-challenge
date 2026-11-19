package com.cocos.llallire.application.service;

import static com.cocos.llallire.TestData.METR;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.marketdata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;

/**
 * Tests of {@link SearchInstrument}.
 */
@ExtendWith(MockitoExtension.class)
class SearchInstrumentTest {

    @Mock
    private LoadInstrumentPort instrumentPort;
    @Mock
    private LoadMarketdataPort marketdataPort;
    @InjectMocks
    private SearchInstrument searchInstrument;

    @Test
    void returnsInstrumentsWithLastPriceAndDailyReturnLoadedInOneQuery() {
        when(instrumentPort.searchByTickerOrName("pa")).thenReturn(List.of(PAMP, METR));
        when(marketdataPort.loadLastMarketdataByInstrumentIds(List.of(PAMP.id(), METR.id())))
                .thenReturn(Map.of(PAMP.id(), marketdata(PAMP, "925.85", "921.80")));

        List<InstrumentInformation> result = searchInstrument.execute("pa");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ticker()).isEqualTo("PAMP");
        assertThat(result.get(0).lastPrice()).isEqualByComparingTo("925.85");
        assertThat(result.get(0).dailyReturnPct()).isEqualByComparingTo("0.44");
        assertThat(result.get(1).lastPrice()).isNull();
        assertThat(result.get(1).dailyReturnPct()).isNull();
    }

    @Test
    void doesNotQueryPricesWhenNothingMatches() {
        when(instrumentPort.searchByTickerOrName("zzz")).thenReturn(List.of());

        assertThat(searchInstrument.execute("zzz")).isEmpty();
        verify(marketdataPort, never()).loadLastMarketdataByInstrumentIds(any());
    }
}
