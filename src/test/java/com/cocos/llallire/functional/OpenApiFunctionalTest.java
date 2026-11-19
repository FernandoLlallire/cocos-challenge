package com.cocos.llallire.functional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Functional tests of the OpenAPI document and Swagger UI. They also guard the response DTO documentation added
 * by {@code OpenApiConfig}: if a DTO field is renamed, the document fails to generate.
 */
@FunctionalTest
class OpenApiFunctionalTest {

    private static final String ORDERS = "$.paths['/api/v1/orders'].post";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentListsEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Cocos Challenge API"))
                .andExpect(jsonPath("$.paths['/api/v1/portfolio/{userId}'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/instruments'].get.summary").exists())
                .andExpect(jsonPath(ORDERS + ".summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{orderId}/cancel'].post.responses['409']").exists());
    }

    @Test
    void placeOrderHasRequestExamplesAndErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(ORDERS + ".requestBody.content['application/json'].examples['MARKET buy by amount']")
                        .exists())
                .andExpect(jsonPath(ORDERS + ".responses['201'].headers.Location").exists())
                .andExpect(jsonPath(ORDERS + ".responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.required",
                        hasItems("userId", "instrumentId", "side", "type")))
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.properties.amount.description",
                        containsString("floor(amount / price)")));
    }

    @Test
    void responseDtosAreDescribed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.PortfolioInformation.properties.availableCash.description",
                        containsString("reserved")))
                .andExpect(jsonPath("$.components.schemas.InstrumentPositionInformation.properties.totalReturnPct"
                        + ".description").exists())
                .andExpect(jsonPath("$.components.schemas.InstrumentInformation.properties.lastPrice.description")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.OrderInformation.properties.status.description")
                        .exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
