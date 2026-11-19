package com.cocos.llallire.infrastructure.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.model.User;

/**
 * Tests of {@link CachingUserPort}.
 */
class CachingUserPortTest {

    private static final User USER = new User(1L, "a@b.com", "10001");

    private final LoadUserPort database = mock(LoadUserPort.class);
    private CachingUserPort port;

    @BeforeEach
    void setUp() {
        port = new CachingUserPort(database,
                new CacheRegion<>(CacheTestSupport.inMemoryCacheManager(), CacheNames.USERS, CacheTestSupport.breaker(), CacheMetrics.noop()));
    }

    @Test
    void secondLookupIsServedFromCache() {
        when(database.loadUserById(1L)).thenReturn(Optional.of(USER));

        assertThat(port.loadUserById(1L)).contains(USER);
        assertThat(port.loadUserById(1L)).contains(USER);

        verify(database, times(1)).loadUserById(1L);
    }

    @Test
    void missingUsersAreNotCached() {
        when(database.loadUserById(9L)).thenReturn(Optional.empty());

        assertThat(port.loadUserById(9L)).isEmpty();
        assertThat(port.loadUserById(9L)).isEmpty();

        verify(database, times(2)).loadUserById(9L);
    }

    @Test
    void lockingLookupAlwaysGoesToTheDatabase() {
        when(database.loadUserByIdForUpdate(1L)).thenReturn(Optional.of(USER));
        port.loadUserById(1L);

        port.loadUserByIdForUpdate(1L);
        port.loadUserByIdForUpdate(1L);

        verify(database, times(2)).loadUserByIdForUpdate(1L);
    }
}
