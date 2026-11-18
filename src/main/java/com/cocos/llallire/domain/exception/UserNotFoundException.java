package com.cocos.llallire.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long userId) {
        super("User %d not found".formatted(userId));
    }
}
