package com.cocos.llallire.domain.exception;

/**
 * Indicates that an entity required by the use case does not exist.
 */
public abstract class ResourceNotFoundException extends DomainException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message description of the missing resource
     */
    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
