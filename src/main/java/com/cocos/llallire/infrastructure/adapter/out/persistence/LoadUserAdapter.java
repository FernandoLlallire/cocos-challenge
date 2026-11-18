package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.UserRepository;

public class LoadUserAdapter implements LoadUserPort {
    private final UserRepository userRepository;

    LoadUserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId).map(EntityMapper::toDomain);
    }
    
}
