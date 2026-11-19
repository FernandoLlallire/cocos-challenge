package com.cocos.llallire.functional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Functional tests of {@code GET /api/v1/instruments}.
 */
@FunctionalTest
class InstrumentFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchesByTickerIgnoringCaseAndReturnsLastPriceAndDailyReturn() throws Exception {
        mockMvc.perform(get("/api/v1/instruments").param("query", "pamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(47))
                .andExpect(jsonPath("$[0].ticker").value("PAMP"))
                .andExpect(jsonPath("$[0].type").value("ACCIONES"))
                .andExpect(jsonPath("$[0].lastPrice").value(925.85))
                .andExpect(jsonPath("$[0].dailyReturnPct").value(0.44));
    }

    @Test
    void searchesByName() throws Exception {
        mockMvc.perform(get("/api/v1/instruments").param("query", "metrogas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("METR"));
    }

    @Test
    void instrumentsWithoutMarketdataHaveNullPrice() throws Exception {
        mockMvc.perform(get("/api/v1/instruments").param("query", "PESOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("ARS"))
                .andExpect(jsonPath("$[0].type").value("MONEDA"))
                .andExpect(jsonPath("$[0].lastPrice").doesNotExist());
    }

    @Test
    void withoutQueryReturnsAllInstrumentsSortedByTicker() throws Exception {
        mockMvc.perform(get("/api/v1/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(66)))
                .andExpect(jsonPath("$[0].ticker").value("AGRO"));
    }

    @Test
    void returnsEmptyListWhenNothingMatches() throws Exception {
        mockMvc.perform(get("/api/v1/instruments").param("query", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
