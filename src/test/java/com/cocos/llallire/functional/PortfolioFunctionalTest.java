package com.cocos.llallire.functional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Functional tests of {@code GET /api/v1/portfolio/{userId}} using the data in {@code database.sql}.
 */
@FunctionalTest
class PortfolioFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * User 1 according to the seed:
     * <ul>
     *   <li>balance: 1,000,000 - 100,000 - 46,500 + 9,400 - 30,800 - 125,000 + 45,900 = 753,000</li>
     *   <li>reserved by pending LIMIT BUYs: 50 x 710 + 60 x 1,500 = 125,500 → available 627,500</li>
     *   <li>PAMP: 40 shares at average cost 930 = 37,200; worth 40 x 925.85 = 37,034 (-0.45%)</li>
     *   <li>METR: 500 shares at 250 = 125,000; worth 500 x 229.50 = 114,750 (-8.20%)</li>
     *   <li>instrument 31: bought 20 and sold 30 → no holding, not shown</li>
     *   <li>total: 753,000 + 37,034 + 114,750 = 904,784</li>
     * </ul>
     */
    @Test
    void returnsTotalValueAvailableCashAndPositionsOfSeedUser() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountValue").value(904784.00))
                .andExpect(jsonPath("$.availableCash").value(627500.00))
                .andExpect(jsonPath("$.positions", hasSize(2)))
                .andExpect(jsonPath("$.positions[0].ticker").value("METR"))
                .andExpect(jsonPath("$.positions[0].quantity").value(500))
                .andExpect(jsonPath("$.positions[0].marketValue").value(114750.00))
                .andExpect(jsonPath("$.positions[0].totalReturnPct").value(-8.20))
                .andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
                .andExpect(jsonPath("$.positions[1].quantity").value(40))
                .andExpect(jsonPath("$.positions[1].marketValue").value(37034.00))
                .andExpect(jsonPath("$.positions[1].totalReturnPct").value(-0.45));
    }

    @Test
    void userWithoutOrdersHasEmptyPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccountValue").value(0))
                .andExpect(jsonPath("$.availableCash").value(0))
                .andExpect(jsonPath("$.positions", hasSize(0)));
    }

    @Test
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User 999 not found"));
    }

    @Test
    void nonNumericUserIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/abc"))
                .andExpect(status().isBadRequest());
    }
}
