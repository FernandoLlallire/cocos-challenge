package com.cocos.llallire.domain.model;

import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.marketdata;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link Marketdata}.
 */
class MarketdataTest {

    @Test
    void dailyReturnUsesCloseAndPreviousClose() {
        assertThat(marketdata(PAMP, "925.85", "921.80").dailyReturnPct()).isEqualByComparingTo("0.44");
    }
}
