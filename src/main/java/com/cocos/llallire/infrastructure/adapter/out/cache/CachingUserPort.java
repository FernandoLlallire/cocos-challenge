package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.Optional;

import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.model.User;

/**
 * Cache-aside decorator of {@link LoadUserPort}.
 *
 * <p>{@link #loadUserById(Long)} is cached. {@link #loadUserByIdForUpdate(Long)} always goes to the database,
 * because its purpose is to take a row lock.</p>
 */
public class CachingUserPort implements LoadUserPort {
    private final LoadUserPort delegate;
    private final CacheRegion<Long, User> users;

    /**
     * Creates the decorator.
     *
     * @param delegate port that reads from the database
     * @param users    users cache region
     */
    public CachingUserPort(LoadUserPort delegate, CacheRegion<Long, User> users) {
        this.delegate = delegate;
        this.users = users;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Missing users are not cached.</p>
     */
    @Override
    public Optional<User> loadUserById(Long userId) {
        Optional<User> cached = users.get(userId);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<User> loaded = delegate.loadUserById(userId);
        loaded.ifPresent(user -> users.put(userId, user));
        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Never cached: the database lock is the point of this method.</p>
     */
    @Override
    public Optional<User> loadUserByIdForUpdate(Long userId) {
        return delegate.loadUserByIdForUpdate(userId);
    }
}
