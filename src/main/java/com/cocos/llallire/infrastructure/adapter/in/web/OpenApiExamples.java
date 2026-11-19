package com.cocos.llallire.infrastructure.adapter.in.web;

/**
 * JSON examples shown in the OpenAPI document and Swagger UI. Values come from the seed in {@code database.sql}
 * (user 1, PAMP = instrument 47 with close 925.85, ARS = instrument 66), the same data used by {@code api.http}
 * and the Postman collection. They are compile-time constants because annotation attributes require them.
 */
final class OpenApiExamples {

    /** Media type of RFC 9457 error responses. */
    static final String PROBLEM_JSON = "application/problem+json";

    // ---------------------------------------------------------------- portfolio

    /** Portfolio of seed user 1. */
    static final String PORTFOLIO_USER_1 = """
            {
              "totalAccountValue": 904784.00,
              "availableCash": 627500.00,
              "positions": [
                { "instrumentId": 54, "ticker": "METR", "name": "MetroGAS S.A.", "quantity": 500,
                  "marketValue": 114750.00, "totalReturnPct": -8.20 },
                { "instrumentId": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "quantity": 40,
                  "marketValue": 37034.00, "totalReturnPct": -0.45 }
              ]
            }""";

    /** Portfolio of a user without orders (seed user 2). */
    static final String PORTFOLIO_EMPTY = """
            { "totalAccountValue": 0, "availableCash": 0, "positions": [] }""";

    // ---------------------------------------------------------------- instruments

    /** Search result for {@code query=pamp}. */
    static final String INSTRUMENTS_PAMP = """
            [
              { "id": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "type": "ACCIONES",
                "lastPrice": 925.85, "dailyReturnPct": 0.44 }
            ]""";

    /** Search result for the currency, which has no marketdata. */
    static final String INSTRUMENTS_ARS = """
            [
              { "id": 66, "ticker": "ARS", "name": "PESOS", "type": "MONEDA",
                "lastPrice": null, "dailyReturnPct": null }
            ]""";

    /** Search without matches. */
    static final String INSTRUMENTS_EMPTY = "[]";

    // ---------------------------------------------------------------- order requests

    /** MARKET buy by exact quantity. */
    static final String REQUEST_MARKET_BUY_SIZE = """
            { "userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "size": 10 }""";

    /** MARKET buy by amount in pesos. */
    static final String REQUEST_MARKET_BUY_AMOUNT = """
            { "userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "amount": 10000 }""";

    /** LIMIT buy. */
    static final String REQUEST_LIMIT_BUY = """
            { "userId": 1, "instrumentId": 47, "side": "BUY", "type": "LIMIT", "size": 5, "price": 900.50 }""";

    /** MARKET sell. */
    static final String REQUEST_MARKET_SELL = """
            { "userId": 1, "instrumentId": 47, "side": "SELL", "type": "MARKET", "size": 5 }""";

    /** Incoming peso transfer. */
    static final String REQUEST_CASH_IN = """
            { "userId": 1, "instrumentId": 66, "side": "CASH_IN", "type": "MARKET", "amount": 50000 }""";

    /** Outgoing peso transfer. */
    static final String REQUEST_CASH_OUT = """
            { "userId": 1, "instrumentId": 66, "side": "CASH_OUT", "type": "MARKET", "amount": 10000 }""";

    /** LIMIT order without price (400). */
    static final String REQUEST_LIMIT_WITHOUT_PRICE = """
            { "userId": 1, "instrumentId": 47, "side": "BUY", "type": "LIMIT", "size": 1 }""";

    // ---------------------------------------------------------------- order responses

    /** MARKET buy executed at the latest close. */
    static final String ORDER_FILLED_BUY = """
            { "id": 12, "instrumentId": 47, "userId": 1, "side": "BUY", "type": "MARKET", "size": 10,
              "price": 925.85, "status": "FILLED", "dateTime": "2026-11-19T11:30:20.132" }""";

    /** Pending LIMIT buy (reserves size * price of cash). */
    static final String ORDER_NEW_LIMIT = """
            { "id": 13, "instrumentId": 47, "userId": 1, "side": "BUY", "type": "LIMIT", "size": 5,
              "price": 900.50, "status": "NEW", "dateTime": "2026-11-19T11:31:02.410" }""";

    /** Sell larger than the available shares (user 1 holds 40 PAMP). */
    static final String ORDER_REJECTED_SELL = """
            { "id": 14, "instrumentId": 47, "userId": 1, "side": "SELL", "type": "MARKET", "size": 100,
              "price": 925.85, "status": "REJECTED", "dateTime": "2026-11-19T11:32:45.007" }""";

    /** Incoming transfer: price 1, size = pesos. */
    static final String ORDER_FILLED_CASH_IN = """
            { "id": 15, "instrumentId": 66, "userId": 1, "side": "CASH_IN", "type": "MARKET", "size": 50000,
              "price": 1.00, "status": "FILLED", "dateTime": "2026-11-19T11:33:10.551" }""";

    /** Seed order 5 after cancelling it. */
    static final String ORDER_CANCELLED = """
            { "id": 5, "instrumentId": 45, "userId": 1, "side": "BUY", "type": "LIMIT", "size": 50,
              "price": 710.00, "status": "CANCELLED", "dateTime": "2023-07-12T15:14:20" }""";

    // ---------------------------------------------------------------- errors

    /** Unknown user. */
    static final String PROBLEM_USER_NOT_FOUND = """
            { "title": "Resource not found", "status": 404,
              "detail": "User 999 not found", "instance": "/api/v1/portfolio/999" }""";

    /** Non-numeric path parameter. */
    static final String PROBLEM_INVALID_USER_ID = """
            { "title": "Invalid parameter", "status": 400,
              "detail": "Parameter 'userId' has an invalid value", "instance": "/api/v1/portfolio/abc" }""";

    /** LIMIT order without price. */
    static final String PROBLEM_LIMIT_WITHOUT_PRICE = """
            { "title": "Invalid order request", "status": 400,
              "detail": "LIMIT orders require a positive price", "instance": "/api/v1/orders" }""";

    /** Both size and amount (or neither). */
    static final String PROBLEM_SIZE_AND_AMOUNT = """
            { "title": "Invalid order request", "status": 400,
              "detail": "exactly one of size or amount must be sent", "instance": "/api/v1/orders" }""";

    /** Unknown enum value or invalid JSON. */
    static final String PROBLEM_MALFORMED_BODY = """
            { "title": "Malformed request body", "status": 400,
              "detail": "The request body is not valid JSON or contains invalid values", "instance": "/api/v1/orders" }""";

    /** Unknown instrument. */
    static final String PROBLEM_INSTRUMENT_NOT_FOUND = """
            { "title": "Resource not found", "status": 404,
              "detail": "Instrument 999 not found", "instance": "/api/v1/orders" }""";

    /** Unknown order. */
    static final String PROBLEM_ORDER_NOT_FOUND = """
            { "title": "Resource not found", "status": 404,
              "detail": "Order 999 not found", "instance": "/api/v1/orders/999/cancel" }""";

    /** Cancelling an order that is not NEW. */
    static final String PROBLEM_NOT_CANCELLABLE = """
            { "title": "Order cannot be cancelled", "status": 409,
              "detail": "Order 2 cannot be cancelled because its status is FILLED; only NEW orders can be cancelled",
              "instance": "/api/v1/orders/2/cancel" }""";

    /**
     * Constants class; not instantiable.
     */
    private OpenApiExamples() {
    }
}
