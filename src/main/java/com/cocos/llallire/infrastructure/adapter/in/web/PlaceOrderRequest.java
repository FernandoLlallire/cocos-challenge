package com.cocos.llallire.infrastructure.adapter.in.web;

import java.math.BigDecimal;

import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderType;

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
public record PlaceOrderRequest(
        Long userId,
        Long instrumentId,
        OrderSide side,
        OrderType type,
        Long size,
        BigDecimal amount,
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
