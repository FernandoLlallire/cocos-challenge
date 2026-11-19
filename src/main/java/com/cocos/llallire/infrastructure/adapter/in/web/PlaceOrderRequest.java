package com.cocos.llallire.infrastructure.adapter.in.web;

import java.math.BigDecimal;

import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderType;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * JSON body of {@code POST /api/v1/orders}.
 *
 * @param userId       user sending the order
 * @param instrumentId instrument to trade
 * @param side         BUY, SELL, CASH_IN or CASH_OUT
 * @param type         MARKET or LIMIT
 * @param size         exact number of shares; mutually exclusive with {@code amount}
 * @param amount       amount in pesos to invest; mutually exclusive with {@code size}
 * @param price        limit price; required only for LIMIT
 */
@Schema(description = "Order to send. Exactly one of `size` or `amount` is required.")
public record PlaceOrderRequest(
        @Schema(description = "User sending the order", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "Stock to trade; for CASH_IN/CASH_OUT, the ARS instrument (id 66)", example = "47",
                requiredMode = RequiredMode.REQUIRED)
        Long instrumentId,

        @Schema(description = "BUY/SELL trade a stock; CASH_IN/CASH_OUT move pesos in or out of the account",
                example = "BUY", requiredMode = RequiredMode.REQUIRED)
        OrderSide side,

        @Schema(description = "MARKET executes at the latest close; LIMIT stays pending at `price`. "
                + "Cash transfers are always MARKET", example = "MARKET", requiredMode = RequiredMode.REQUIRED)
        OrderType type,

        @Schema(description = "Exact number of shares (or pesos, for transfers). Positive. "
                + "Mutually exclusive with `amount`", example = "10", minimum = "1",
                requiredMode = RequiredMode.NOT_REQUIRED)
        Long size,

        @Schema(description = "Amount in pesos. The order is sent for `floor(amount / price)` shares; "
                + "if that is 0 the order is REJECTED. Positive. Mutually exclusive with `size`",
                example = "10000", requiredMode = RequiredMode.NOT_REQUIRED)
        BigDecimal amount,

        @Schema(description = "Limit price in pesos, positive with at most 2 decimals. Required for LIMIT; "
                + "MARKET orders must not send it", example = "900.50",
                requiredMode = RequiredMode.NOT_REQUIRED)
        BigDecimal price) {

    /**
     * Converts the HTTP body into the use case command.
     *
     * @return equivalent command
     */
    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(userId, instrumentId, side, type, size, amount, price);
    }
}
