package com.cocos.llallire.infrastructure.adapter.in.web;

import static com.cocos.llallire.infrastructure.adapter.in.web.OpenApiExamples.PROBLEM_JSON;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.port.in.CancelOrderUseCase;
import com.cocos.llallire.application.port.in.PlaceOrderUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST adapter for orders.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/orders")
@Tag(name = "Orders", description = "Send orders to the market and cancel pending ones")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    /**
     * Creates the controller.
     *
     * @param placeOrderUseCase  order placement use case
     * @param cancelOrderUseCase order cancellation use case
     */
    public OrderController(PlaceOrderUseCase placeOrderUseCase, CancelOrderUseCase cancelOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    /**
     * {@code POST /api/v1/orders}: sends an order to the market.
     *
     * <p>Responds 201 whenever the order is recorded, even if it ended REJECTED: the rejection is a business
     * outcome and is stored in the {@code orders} table. Format errors return 400 and are not
     * stored.</p>
     *
     * @param request order data
     * @return 201 with the recorded order and its status
     */
    @PostMapping
    @Operation(
            summary = "Send an order",
            description = """
                    Records a BUY/SELL of a stock or a CASH_IN/CASH_OUT of pesos.

                    - **MARKET** executes at the latest `close` → `FILLED`. **LIMIT** uses the request `price`
                      → `NEW` (pending), reserving `size * price` of cash (BUY) or `size` shares (SELL).
                    - Send exactly one of `size` or `amount`; `amount` becomes `floor(amount / price)` shares.
                    - CASH_IN/CASH_OUT must be MARKET and use the ARS instrument (id 66): price 1, size = pesos.
                    - Insufficient cash/shares, or an `amount` below one share, → the order is stored as
                      `REJECTED` and the response is still **201**: check `status`.
                    - Malformed requests → **400** and nothing is stored.""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = PlaceOrderRequest.class), examples = {
                            @ExampleObject(name = "MARKET buy by size",
                                    summary = "Buys 10 PAMP at the latest close",
                                    value = OpenApiExamples.REQUEST_MARKET_BUY_SIZE),
                            @ExampleObject(name = "MARKET buy by amount",
                                    summary = "Invests up to 10,000 pesos: size = floor(10000 / 925.85) = 10",
                                    value = OpenApiExamples.REQUEST_MARKET_BUY_AMOUNT),
                            @ExampleObject(name = "LIMIT buy",
                                    summary = "Stays NEW and reserves 5 x 900.50 of the available cash",
                                    value = OpenApiExamples.REQUEST_LIMIT_BUY),
                            @ExampleObject(name = "MARKET sell",
                                    summary = "REJECTED if it exceeds the available shares",
                                    value = OpenApiExamples.REQUEST_MARKET_SELL),
                            @ExampleObject(name = "CASH_IN",
                                    summary = "Incoming transfer of 50,000 pesos",
                                    value = OpenApiExamples.REQUEST_CASH_IN),
                            @ExampleObject(name = "CASH_OUT",
                                    summary = "Outgoing transfer; REJECTED if it exceeds the available cash",
                                    value = OpenApiExamples.REQUEST_CASH_OUT),
                            @ExampleObject(name = "Invalid: LIMIT without price",
                                    summary = "Returns 400 and is not stored",
                                    value = OpenApiExamples.REQUEST_LIMIT_WITHOUT_PRICE)})))
    @ApiResponse(responseCode = "201",
            description = "Order recorded; `status` is FILLED, NEW or REJECTED",
            headers = @Header(name = "Location", description = "URI of the created order",
                    schema = @Schema(type = "string", example = "/api/v1/orders/12")),
            content = @Content(schema = @Schema(implementation = OrderInformation.class), examples = {
                    @ExampleObject(name = "FILLED (MARKET buy)", value = OpenApiExamples.ORDER_FILLED_BUY),
                    @ExampleObject(name = "NEW (LIMIT buy)", value = OpenApiExamples.ORDER_NEW_LIMIT),
                    @ExampleObject(name = "REJECTED (not enough shares)",
                            value = OpenApiExamples.ORDER_REJECTED_SELL),
                    @ExampleObject(name = "FILLED (CASH_IN)", value = OpenApiExamples.ORDER_FILLED_CASH_IN)}))
    @ApiResponse(responseCode = "400",
            description = "Malformed request (missing fields, size and amount together, LIMIT without price, "
                    + "MARKET with price, unknown enum, wrong instrument for the side); not stored",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "LIMIT without price",
                                    value = OpenApiExamples.PROBLEM_LIMIT_WITHOUT_PRICE),
                            @ExampleObject(name = "size and amount", value = OpenApiExamples.PROBLEM_SIZE_AND_AMOUNT),
                            @ExampleObject(name = "Malformed body", value = OpenApiExamples.PROBLEM_MALFORMED_BODY)}))
    @ApiResponse(responseCode = "404", description = "The user or the instrument does not exist",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Unknown user", value = OpenApiExamples.PROBLEM_USER_NOT_FOUND),
                            @ExampleObject(name = "Unknown instrument",
                                    value = OpenApiExamples.PROBLEM_INSTRUMENT_NOT_FOUND)}))
    public ResponseEntity<OrderInformation> placeOrder(@RequestBody PlaceOrderRequest request) {
        OrderInformation order = placeOrderUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/orders/" + order.id())).body(order);
    }

    /**
     * {@code POST /api/v1/orders/{orderId}/cancel}: cancels an order in NEW status.
     *
     * @param orderId order to cancel
     * @return 200 with the cancelled order; 404 if it does not exist; 409 if it is not in NEW status
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(
            summary = "Cancel a pending order",
            description = """
                    Moves a `NEW` order to `CANCELLED`, releasing the cash or shares it reserved. The order is not
                    deleted: it stays in the history. Any other status returns **409**.""")
    @ApiResponse(responseCode = "200", description = "Cancelled order",
            content = @Content(schema = @Schema(implementation = OrderInformation.class),
                    examples = @ExampleObject(name = "Seed order 5 cancelled",
                            value = OpenApiExamples.ORDER_CANCELLED)))
    @ApiResponse(responseCode = "400", description = "`orderId` is not a number",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The order does not exist",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown order", value = OpenApiExamples.PROBLEM_ORDER_NOT_FOUND)))
    @ApiResponse(responseCode = "409", description = "The order is not in NEW status",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Already FILLED", value = OpenApiExamples.PROBLEM_NOT_CANCELLABLE)))
    public ResponseEntity<OrderInformation> cancelOrder(
            @Parameter(description = "Identifier of the order to cancel", example = "5") @PathVariable Long orderId) {
        return ResponseEntity.ok(cancelOrderUseCase.execute(orderId));
    }
}
