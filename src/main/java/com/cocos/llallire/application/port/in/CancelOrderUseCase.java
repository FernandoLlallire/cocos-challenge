package com.cocos.llallire.application.port.in;

import com.cocos.llallire.application.dto.OrderInformation;

/**
 * Inbound port: cancel a pending order.
 */
public interface CancelOrderUseCase {

    /**
     * Cancels the order. Only orders in NEW status can be cancelled.
     *
     * @param orderId order to cancel
     * @return the order with CANCELLED status
     * @throws com.cocos.llallire.domain.exception.OrderNotFoundException       if the order does not exist
     * @throws com.cocos.llallire.domain.exception.OrderNotCancellableException if the order is not in NEW status
     */
    OrderInformation execute(Long orderId);
}
