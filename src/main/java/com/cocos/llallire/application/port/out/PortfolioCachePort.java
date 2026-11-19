package com.cocos.llallire.application.port.out;

import java.util.function.Supplier;

import com.cocos.llallire.application.dto.PortfolioInformation;

/**
 * Outbound port: cache of calculated portfolios.
 *
 * <p>A portfolio depends on the user's orders, so it cannot rely only on a TTL: every use case that changes a
 * user's orders must call {@link #invalidate(Long)}. Implementations must never return a portfolio computed
 * before the last invalidation, and must fall back to the loader if the cache is unavailable.</p>
 */
public interface PortfolioCachePort {

    /**
     * Returns the cached portfolio of the user or computes it with the loader and caches it.
     *
     * @param userId user whose portfolio is requested
     * @param loader computes the portfolio from the database on a cache miss; its exceptions are propagated
     *               and nothing is cached
     * @return the cached or freshly computed portfolio
     */
    PortfolioInformation getOrCompute(Long userId, Supplier<PortfolioInformation> loader);

    /**
     * Discards any cached portfolio of the user. When called inside a transaction, the invalidation is also
     * repeated after the commit so no reader can cache data from before the change.
     *
     * @param userId user whose orders changed
     */
    void invalidate(Long userId);
}
