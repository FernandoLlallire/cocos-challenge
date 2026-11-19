package com.cocos.llallire.domain.model;

/**
 * Account holder.
 *
 * @param id            identifier
 * @param email         user email
 * @param accountNumber account number
 */
public record User(Long id, String email, String accountNumber) {
}
