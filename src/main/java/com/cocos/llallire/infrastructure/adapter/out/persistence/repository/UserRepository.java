package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.UserEntity;

import jakarta.persistence.LockModeType;

/**
 * Spring Data repository for users.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Finds a user and locks its row ({@code SELECT ... FOR UPDATE}) until the end of the transaction.
     *
     * @param id user id
     * @return the user, or empty if it does not exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
}
