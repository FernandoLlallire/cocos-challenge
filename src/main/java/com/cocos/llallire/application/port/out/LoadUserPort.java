package com.cocos.llallire.application.port.out;

import java.util.Optional;

import com.cocos.llallire.domain.model.User;

public interface LoadUserPort {
    Optional<User> getUserById(Long userId);
}
