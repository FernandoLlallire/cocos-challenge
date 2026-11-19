package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.out.PortfolioCachePort;

/**
 * Implementation of {@link PortfolioCachePort} based on <b>generation tokens</b>.
 *
 * <p>Each user has a random generation token, and portfolios are stored under {@code userId:generation}.
 * Invalidating means replacing the token, which makes every previous entry unreachable (they expire with their
 * TTL). This avoids the classic cache-aside race: a reader that loaded data before an order was committed stores
 * its result under the old token, which is never read again.</p>
 *
 * <p>Invalidation happens immediately and, if a transaction is active, again after the commit, so a reader that
 * ran between both moments cannot leave stale data reachable.</p>
 *
 * <p>If an invalidation cannot be written (the cache is unreachable), the user is kept in a local pending set.
 * Pending invalidations are retried before any other cache operation, and while a user has one pending, their
 * portfolio is computed from the database and neither read from nor written to the cache. This prevents serving
 * a stale portfolio when the cache comes back without having lost its data. If the instance stops with pending
 * invalidations, the portfolio TTL bounds the staleness.</p>
 */
public class PortfolioCacheAdapter implements PortfolioCachePort {
    private final CacheRegion<String, PortfolioInformation> portfolios;
    private final CacheRegion<Long, String> generations;
    private final Set<Long> pendingInvalidations = ConcurrentHashMap.newKeySet();

    /**
     * Creates the adapter.
     *
     * @param portfolios  portfolio cache region
     * @param generations generation token cache region
     */
    public PortfolioCacheAdapter(CacheRegion<String, PortfolioInformation> portfolios,
            CacheRegion<Long, String> generations) {
        this.portfolios = portfolios;
        this.generations = generations;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The generation is read before calling the loader, so the result is stored under the generation that was
     * current when the data was read.</p>
     */
    @Override
    public PortfolioInformation getOrCompute(Long userId, Supplier<PortfolioInformation> loader) {
        retryPendingInvalidations();
        if (pendingInvalidations.contains(userId)) {
            return loader.get();
        }
        String key = userId + ":" + currentGeneration(userId);
        return portfolios.get(key).orElseGet(() -> {
            PortfolioInformation portfolio = loader.get();
            portfolios.put(key, portfolio);
            return portfolio;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidate(Long userId) {
        retryPendingInvalidations();
        rotateGeneration(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rotateGeneration(userId);
                }
            });
        }
    }

    /**
     * Returns the user's generation token, creating one if it does not exist.
     *
     * @param userId user
     * @return generation token
     */
    private String currentGeneration(Long userId) {
        return generations.get(userId).orElseGet(() -> rotateGeneration(userId));
    }

    /**
     * Replaces the user's generation token with a new random one. If it cannot be written, the user is added to
     * the pending invalidations.
     *
     * @param userId user
     * @return the new token
     */
    private String rotateGeneration(Long userId) {
        String generation = UUID.randomUUID().toString();
        if (generations.put(userId, generation)) {
            pendingInvalidations.remove(userId);
        } else {
            pendingInvalidations.add(userId);
        }
        return generation;
    }

    /**
     * Retries the invalidations that could not be written before. Stops at the first failure, since the cache is
     * still unavailable.
     */
    private void retryPendingInvalidations() {
        for (Long userId : pendingInvalidations) {
            if (!generations.put(userId, UUID.randomUUID().toString())) {
                return;
            }
            pendingInvalidations.remove(userId);
        }
    }
}
