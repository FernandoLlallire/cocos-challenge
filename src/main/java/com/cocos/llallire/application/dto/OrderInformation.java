package com.cocos.llallire.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;

/**
 * Order returned by the order use cases.
 *
 * @param id           identifier
 * @param instrumentId traded instrument
 * @param userId       user
 * @param side         side
 * @param type         type
 * @param size         quantity
 * @param price        unit price in pesos
 * @param status       status
 * @param dateTime     submission time
 */
public record OrderInformation(
        Long id,
        Long instrumentId,
        Long userId,
        OrderSide side,
        OrderType type,
        Long size,
        BigDecimal price,
        OrderStatus status,
        LocalDateTime dateTime) {

    /**
     * Converts the domain order into the output DTO.
     *
     * @param order order
     * @return equivalent DTO
     */
    public static OrderInformation from(Order order) {
        return new OrderInformation(order.id(), order.instrumentId(), order.userId(), order.side(), order.type(),
                order.size(), order.price(), order.status(), order.dateTime());
    }
}
