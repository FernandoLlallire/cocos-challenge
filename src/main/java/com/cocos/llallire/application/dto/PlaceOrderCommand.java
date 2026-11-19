package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

import com.cocos.llallire.domain.model.OrderRequest;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderType;

/**
 * Input data to send an order.
 *
 * @param userId       user sending the order
 * @param instrumentId instrument to trade
 * @param side         order side
 * @param type         order type
 * @param size         exact number of shares; mutually exclusive with {@code amount}
 * @param amount       amount in pesos to invest; mutually exclusive with {@code size}
 * @param price        limit price (LIMIT only)
 */
public record PlaceOrderCommand(
        Long userId,
        Long instrumentId,
        OrderSide side,
        OrderType type,
        Long size,
        BigDecimal amount,
        BigDecimal price) {

    /**
     * Converts the command into a domain request, which validates its structure.
     *
     * @return domain request
     * @throws com.cocos.llallire.domain.exception.InvalidOrderRequestException if the request is invalid
     */
    public OrderRequest toOrderRequest() {
        return new OrderRequest(userId, instrumentId, side, type, size, amount, price);
    }
}
