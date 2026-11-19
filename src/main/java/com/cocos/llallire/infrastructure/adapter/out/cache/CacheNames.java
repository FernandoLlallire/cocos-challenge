package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.List;

/**
 * Names of the cache regions. Each region has its own TTL and value type (see
 * {@link com.cocos.llallire.infrastructure.config.CacheConfig}).
 */
public final class CacheNames {

    /** {@code userId → User}. Only existing users are cached. */
    public static final String USERS = "users";
    /** {@code instrumentId → Instrument}. Only existing instruments are cached. */
    public static final String INSTRUMENTS = "instruments";
    /** {@code normalized query → List<Instrument>}. */
    public static final String INSTRUMENT_SEARCH = "instrument-search";
    /** {@code instrumentId → CachedValue<Marketdata>}. Absent marketdata is cached too (negative caching). */
    public static final String MARKETDATA = "marketdata";
    /** {@code userId:generation → PortfolioInformation}. */
    public static final String PORTFOLIO = "portfolio";
    /** {@code userId → generation token} used to invalidate the user's portfolio. */
    public static final String PORTFOLIO_GENERATION = "portfolio-generation";

    /** All region names. */
    public static final List<String> ALL = List.of(USERS, INSTRUMENTS, INSTRUMENT_SEARCH, MARKETDATA, PORTFOLIO,
            PORTFOLIO_GENERATION);

    /**
     * Constants class; not instantiable.
     */
    private CacheNames() {
    }
}
