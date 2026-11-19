package com.cocos.llallire.domain.exception;

/**
 * The requested user does not exist.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    /**
     * Creates the exception for the given identifier.
     *
     * @param userId requested identifier
     */
    public UserNotFoundException(Long userId) {
        super("User %d not found".formatted(userId));
    }
}
