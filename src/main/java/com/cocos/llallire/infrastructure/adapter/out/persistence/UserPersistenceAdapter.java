package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.UserRepository;

/**
 * JPA adapter for {@link LoadUserPort}.
 */
@Component
public class UserPersistenceAdapter implements LoadUserPort {
    private final UserRepository userRepository;

    /**
     * Creates the adapter.
     *
     * @param userRepository user repository
     */
    public UserPersistenceAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> loadUserById(Long userId) {
        return userRepository.findById(userId).map(EntityMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code SELECT ... FOR UPDATE}; requires an active transaction.</p>
     */
    @Override
    public Optional<User> loadUserByIdForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId).map(EntityMapper::toDomain);
    }
}
