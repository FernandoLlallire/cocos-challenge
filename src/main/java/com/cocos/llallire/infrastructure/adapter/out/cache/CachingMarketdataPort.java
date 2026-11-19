package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Marketdata;

/**
 * Cache-aside decorator of {@link LoadMarketdataPort}.
 *
 * <p>Prices change, so this region uses a short TTL: a cached price can be at most that old. The absence of
 * marketdata is cached as well (for example, the ARS currency has none), so it is not queried on every
 * request.</p>
 */
public class CachingMarketdataPort implements LoadMarketdataPort {
    private final LoadMarketdataPort delegate;
    private final CacheRegion<Long, CachedValue<Marketdata>> marketdata;

    /**
     * Creates the decorator.
     *
     * @param delegate   port that reads from the database
     * @param marketdata latest-marketdata cache region
     */
    public CachingMarketdataPort(LoadMarketdataPort delegate, CacheRegion<Long, CachedValue<Marketdata>> marketdata) {
        this.delegate = delegate;
        this.marketdata = marketdata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Marketdata> loadLastMarketdataByInstrumentId(Long instrumentId) {
        Optional<CachedValue<Marketdata>> cached = marketdata.get(instrumentId);
        if (cached.isPresent()) {
            return Optional.ofNullable(cached.get().value());
        }
        Optional<Marketdata> loaded = delegate.loadLastMarketdataByInstrumentId(instrumentId);
        marketdata.put(instrumentId, CachedValue.of(loaded.orElse(null)));
        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only the ids missing from the cache are loaded, in a single database query.</p>
     */
    @Override
    public Map<Long, Marketdata> loadLastMarketdataByInstrumentIds(Collection<Long> instrumentIds) {
        Map<Long, CachedValue<Marketdata>> cached = marketdata.getAll(instrumentIds);
        Map<Long, Marketdata> result = new HashMap<>();
        cached.forEach((id, value) -> {
            if (value.value() != null) {
                result.put(id, value.value());
            }
        });
        List<Long> missing = instrumentIds.stream().filter(id -> !cached.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            Map<Long, Marketdata> loaded = delegate.loadLastMarketdataByInstrumentIds(missing);
            missing.forEach(id -> marketdata.put(id, CachedValue.of(loaded.get(id))));
            result.putAll(loaded);
        }
        return result;
    }
}
