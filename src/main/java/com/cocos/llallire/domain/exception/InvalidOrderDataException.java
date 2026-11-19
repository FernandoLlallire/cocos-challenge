package com.cocos.llallire.domain.exception;

/**
 * A persisted order has inconsistent data (for example, a FILLED order without price)
 * and cannot be used to calculate holdings.
 */
public class InvalidOrderDataException extends DomainException {

    /**
     * Creates the exception for the order with invalid data.
     *
     * @param orderId inconsistent order
     * @param reason  description of the invalid data
     */
    public InvalidOrderDataException(Long orderId, String reason) {
        super("Order %d has invalid data: %s".formatted(orderId, reason));
    }
}
