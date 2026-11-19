package com.cocos.llallire.domain.exception;

/**
 * Base exception for every domain rule violation.
 * The web layer translates it into an HTTP response based on its subtype.
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message error description
     */
    protected DomainException(String message) {
        super(message);
    }
}
