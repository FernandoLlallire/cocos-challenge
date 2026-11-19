package com.cocos.llallire.functional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.persistence.EntityManagerFactory;

/**
 * Functional tests of the cache: they count the SQL statements Hibernate executes to prove that repeated requests
 * do not reach the database, and that order changes invalidate the cached portfolio.
 */
@FunctionalTest
class CacheFunctionalTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void repeatedPortfolioRequestDoesNotQueryTheDatabase() throws Exception {
        long firstCall = statementsExecutedBy(get("/api/v1/portfolio/1"));
        long secondCall = statementsExecutedBy(get("/api/v1/portfolio/1"));

        assertThat(firstCall).isPositive();
        assertThat(secondCall).isZero();
    }

    @Test
    void repeatedSearchDoesNotQueryTheDatabase() throws Exception {
        long firstCall = statementsExecutedBy(get("/api/v1/instruments").param("query", "pamp"));
        long secondCall = statementsExecutedBy(get("/api/v1/instruments").param("query", " PAMP "));

        assertThat(firstCall).isPositive();
        assertThat(secondCall).isZero();
    }

    @Test
    void placingAnOrderInvalidatesTheCachedPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(627500.00));

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 10}
                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(618241.50))
                .andExpect(jsonPath("$.positions[1].quantity").value(50));
    }

    @Test
    void cancellingAnOrderInvalidatesTheCachedPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(627500.00));

        // Seed order 5 is a pending LIMIT BUY of 50 x 710 = 35,500 reserved.
        mockMvc.perform(post("/api/v1/orders/5/cancel")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(663000.00));
    }

    @Test
    void rejectedOrderKeepsTheCachedPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/1"));

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 100000}
                """))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(statementsExecutedBy(get("/api/v1/portfolio/1"))).isZero();
    }

    @Test
    void placingAnOrderStillReadsCashAndHoldingsFromTheDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio/1"));

        long statements = statementsExecutedBy(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 1}
                        """));

        // user lock + user orders + insert; instrument and price come from the cache
        assertThat(statements).isEqualTo(3);
    }

    /**
     * Performs the request and returns how many SQL statements it executed.
     */
    private long statementsExecutedBy(MockHttpServletRequestBuilder request) throws Exception {
        statistics.clear();
        mockMvc.perform(request).andExpect(status().is2xxSuccessful());
        return statistics.getPrepareStatementCount();
    }
}
