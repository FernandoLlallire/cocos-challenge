package com.cocos.llallire.application.port.out;

import java.util.Optional;

import com.cocos.llallire.domain.model.User;

/**
 * Outbound port: user reads.
 */
public interface LoadUserPort {

    /**
     * Finds a user by id.
     *
     * @param userId user id
     * @return the user, or empty if it does not exist
     */
    Optional<User> loadUserById(Long userId);

    /**
     * Finds a user by id and locks it until the end of the transaction. Used to serialize concurrent
     * orders of the same user and prevent two orders from spending the same funds.
     *
     * @param userId user id
     * @return the user, or empty if it does not exist
     */
    Optional<User> loadUserByIdForUpdate(Long userId);
}
