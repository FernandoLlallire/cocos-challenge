package com.cocos.llallire.domain.exception;

import com.cocos.llallire.domain.model.OrderStatus;

/**
 * An attempt was made to cancel an order that is not in {@link OrderStatus#NEW} status.
 */
public class OrderNotCancellableException extends DomainException {

    /**
     * Creates the exception for the order and its current status.
     *
     * @param orderId order that was attempted to be cancelled
     * @param status  current status of the order
     */
    public OrderNotCancellableException(Long orderId, OrderStatus status) {
        super("Order %d cannot be cancelled because its status is %s; only NEW orders can be cancelled"
                .formatted(orderId, status));
    }
}
