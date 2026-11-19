package com.cocos.llallire.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;

/**
 * OpenAPI document ({@code /v3/api-docs}) and Swagger UI ({@code /swagger-ui.html}).
 *
 * <p>Request bodies and controllers are annotated in the web adapter. The response DTOs live in the application
 * layer, which must stay free of framework annotations (hexagonal rule), so their field descriptions and examples
 * are added here by {@link #describeApplicationDtos()}. If a DTO field is renamed, the customizer fails and
 * {@code OpenApiFunctionalTest} catches it.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Field descriptions and examples of the response schemas, by schema name and property. */
    private static final Map<String, Map<String, Doc>> RESPONSE_DOCS = responseDocs();

    /**
     * General information of the API.
     *
     * @return OpenAPI model with title, version and description
     */
    @Bean
    public OpenAPI cocosOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Cocos Challenge API")
                .version("v1")
                .description("""
                        Portfolio, instrument search and orders. Examples use the seed in `database.sql`
                        (user 1, PAMP = instrument 47, ARS = instrument 66). Money values are in pesos with
                        2 decimals; percentages have 2 decimals. Errors follow RFC 9457 (Problem Details)."""));
    }

    /**
     * Adds descriptions and examples to the schemas of the application response DTOs.
     *
     * @return customizer applied when the document is generated
     * @throws IllegalStateException (when the document is generated) if a documented schema or property does
     *                               not exist
     */
    @Bean
    public OpenApiCustomizer describeApplicationDtos() {
        return openApi -> {
            @SuppressWarnings("rawtypes")
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            RESPONSE_DOCS.forEach((schemaName, properties) -> {
                Schema<?> schema = schemas.get(schemaName);
                if (schema == null) {
                    throw new IllegalStateException("OpenAPI schema not found: " + schemaName);
                }
                properties.forEach((propertyName, doc) -> {
                    Schema<?> property = schema.getProperties() == null ? null : schema.getProperties().get(propertyName);
                    if (property == null) {
                        throw new IllegalStateException("OpenAPI property not found: %s.%s"
                                .formatted(schemaName, propertyName));
                    }
                    property.setDescription(doc.description());
                    if (doc.example() != null) {
                        property.setExample(doc.example());
                    }
                });
            });
        };
    }

    /**
     * Builds the documentation of every response DTO field.
     *
     * @return map schema name → property name → documentation, in declaration order
     */
    private static Map<String, Map<String, Doc>> responseDocs() {
        Map<String, Map<String, Doc>> docs = new LinkedHashMap<>();
        docs.put("PortfolioInformation", ordered(
                "totalAccountValue", new Doc("Cash balance in pesos (including cash reserved by pending LIMIT "
                        + "buys) + market value of the positions", 904784.00),
                "availableCash", new Doc("Pesos available to trade: cash balance minus the cash reserved by "
                        + "pending LIMIT buys", 627500.00),
                "positions", new Doc("Assets with a holding greater than zero, sorted by ticker", null)));
        docs.put("InstrumentPositionInformation", ordered(
                "instrumentId", new Doc("Instrument identifier", 47),
                "ticker", new Doc("Market symbol", "PAMP"),
                "name", new Doc("Instrument name", "Pampa Holding S.A."),
                "quantity", new Doc("Shares held (FILLED orders only)", 40),
                "marketValue", new Doc("quantity x latest close, in pesos", 37034.00),
                "totalReturnPct", new Doc("Total return (%): (marketValue - cost) / cost x 100, with weighted "
                        + "average cost", -0.45)));
        docs.put("InstrumentInformation", ordered(
                "id", new Doc("Instrument identifier", 47),
                "ticker", new Doc("Market symbol", "PAMP"),
                "name", new Doc("Instrument name", "Pampa Holding S.A."),
                "type", new Doc("ACCIONES (stock) or MONEDA (currency)", "ACCIONES"),
                "lastPrice", new Doc("Latest close in pesos; null if there is no marketdata", 925.85),
                "dailyReturnPct", new Doc("Daily return (%): (close - previousClose) / previousClose x 100; "
                        + "null if there is no marketdata", 0.44)));
        docs.put("OrderInformation", ordered(
                "id", new Doc("Order identifier", 12),
                "instrumentId", new Doc("Traded instrument (66 = ARS for cash transfers)", 47),
                "userId", new Doc("User that sent the order", 1),
                "side", new Doc("BUY, SELL, CASH_IN or CASH_OUT", "BUY"),
                "type", new Doc("MARKET or LIMIT", "MARKET"),
                "size", new Doc("Number of shares (pesos for transfers); computed from `amount` when it was sent",
                        10),
                "price", new Doc("Unit price in pesos: latest close (MARKET), request price (LIMIT) or 1 "
                        + "(transfers)", 925.85),
                "status", new Doc("FILLED (executed), NEW (pending LIMIT), REJECTED (insufficient funds or "
                        + "shares) or CANCELLED", "FILLED"),
                "dateTime", new Doc("Submission time (server local time, ISO-8601)",
                        "2026-11-19T11:30:20.132")));
        return docs;
    }

    /**
     * Builds an insertion-ordered map from alternating keys and values.
     *
     * @param entries property name, documentation, property name, documentation, ...
     * @return ordered map
     */
    private static Map<String, Doc> ordered(Object... entries) {
        Map<String, Doc> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], (Doc) entries[i + 1]);
        }
        return map;
    }

    /**
     * Documentation of one schema property.
     *
     * @param description human-readable meaning of the field
     * @param example     example value; {@code null} to keep the generated one
     */
    private record Doc(String description, Object example) {
    }
}
