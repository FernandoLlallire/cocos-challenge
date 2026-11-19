package com.cocos.llallire.domain.exception;

/**
 * The order request is malformed (for example, a LIMIT order without price).
 * Unlike a REJECTED order, this error is not persisted: the order never reaches the market.
 */
public class InvalidOrderRequestException extends DomainException {

    /**
     * Creates the exception with the reason it is invalid.
     *
     * @param message reason
     */
    public InvalidOrderRequestException(String message) {
        super(message);
    }
}
