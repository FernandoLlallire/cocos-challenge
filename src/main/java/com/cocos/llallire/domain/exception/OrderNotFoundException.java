package com.cocos.llallire.domain.exception;

/**
 * The requested order does not exist.
 */
public class OrderNotFoundException extends ResourceNotFoundException {

    /**
     * Creates the exception for the given identifier.
     *
     * @param orderId requested identifier
     */
    public OrderNotFoundException(Long orderId) {
        super("Order %d not found".formatted(orderId));
    }
}
