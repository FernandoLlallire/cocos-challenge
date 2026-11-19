package com.cocos.llallire.application.port.in;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.dto.PlaceOrderCommand;

/**
 * Inbound port: send an order to the market.
 */
public interface PlaceOrderUseCase {

    /**
     * Sends the order and persists it with the resulting status (FILLED, NEW or REJECTED).
     *
     * @param command order data
     * @return the persisted order
     * @throws com.cocos.llallire.domain.exception.InvalidOrderRequestException if the request is invalid
     * @throws com.cocos.llallire.domain.exception.ResourceNotFoundException     if the user, the instrument
     *                                                                           or its price does not exist
     */
    OrderInformation execute(PlaceOrderCommand command);
}
