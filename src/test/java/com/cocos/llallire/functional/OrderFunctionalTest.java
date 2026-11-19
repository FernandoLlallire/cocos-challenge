package com.cocos.llallire.functional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.OrderRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Functional test of order placement and cancellation ({@code POST /api/v1/orders} and
 * {@code POST /api/v1/orders/{id}/cancel}) using the data in {@code database.sql}.
 *
 * <p>Initial state of user 1: 627,500 available pesos and 40 PAMP shares (id 47, close 925.85).</p>
 */
@FunctionalTest
class OrderFunctionalTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void marketBuyIsFilledAtLastCloseAndUpdatesPortfolio() throws Exception {
        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 10}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/orders/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.price").value(925.85))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.dateTime").isNotEmpty());

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(618241.50))
                .andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
                .andExpect(jsonPath("$.positions[1].quantity").value(50));
    }

    @Test
    void marketBuyByAmountSendsTheMaximumWholeNumberOfShares() throws Exception {
        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "amount": 10000}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void limitBuyIsNewReservesCashAndCanBeCancelled() throws Exception {
        String body = placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "LIMIT", "size": 100, "price": 900}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.price").value(900))
                .andReturn().getResponse().getContentAsString();
        long orderId = JSON.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(537500.00))
                .andExpect(jsonPath("$.positions[1].quantity").value(40));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(627500.00));
    }

    @Test
    void buyAboveAvailableCashIsRejectedAndPersisted() throws Exception {
        String body = placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 1000}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andReturn().getResponse().getContentAsString();

        JsonNode order = JSON.readTree(body);
        assertThat(orderRepository.findById(order.get("id").asLong()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(OrderStatus.REJECTED));
        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(627500.00));
    }

    @Test
    void sellIsValidatedAgainstHeldShares() throws Exception {
        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "SELL", "type": "MARKET", "size": 41}
                """)
                .andExpect(jsonPath("$.status").value("REJECTED"));

        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "SELL", "type": "MARKET", "size": 40}
                """)
                .andExpect(jsonPath("$.status").value("FILLED"));

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(664534.00))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'PAMP')]").isEmpty());
    }

    @Test
    void cashTransfersMoveAvailableCash() throws Exception {
        placeOrder("""
                {"userId": 1, "instrumentId": 66, "side": "CASH_IN", "type": "MARKET", "amount": 2500}
                """)
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.size").value(2500))
                .andExpect(jsonPath("$.price").value(1));

        placeOrder("""
                {"userId": 1, "instrumentId": 66, "side": "CASH_OUT", "type": "MARKET", "size": 700000}
                """)
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/portfolio/1"))
                .andExpect(jsonPath("$.availableCash").value(630000.00));
    }

    @Test
    void malformedRequestsReturn400AndAreNotPersisted() throws Exception {
        long ordersBefore = orderRepository.count();

        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "LIMIT", "size": 1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("LIMIT orders require a positive price"));
        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 1, "amount": 100}
                """)
                .andExpect(status().isBadRequest());
        placeOrder("""
                {"userId": 1, "instrumentId": 66, "side": "BUY", "type": "MARKET", "size": 1}
                """)
                .andExpect(status().isBadRequest());
        placeOrder("""
                {"userId": 1, "instrumentId": 47, "side": "HOLD", "type": "MARKET", "size": 1}
                """)
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    void unknownUserOrInstrumentReturns404() throws Exception {
        placeOrder("""
                {"userId": 999, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 1}
                """)
                .andExpect(status().isNotFound());
        placeOrder("""
                {"userId": 1, "instrumentId": 999, "side": "BUY", "type": "MARKET", "size": 1}
                """)
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyNewOrdersCanBeCancelled() throws Exception {
        // Seed order 5: LIMIT BUY in NEW status. Order 2: MARKET BUY FILLED.
        mockMvc.perform(post("/api/v1/orders/5/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/orders/2/cancel"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/orders/9999/cancel"))
                .andExpect(status().isNotFound());
    }

    /**
     * Sends an order with the given JSON body.
     */
    private ResultActions placeOrder(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
