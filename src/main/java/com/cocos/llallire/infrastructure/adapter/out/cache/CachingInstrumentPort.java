package com.cocos.llallire.infrastructure.adapter.out.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.domain.model.Instrument;

/**
 * Cache-aside decorator of {@link LoadInstrumentPort}. Instruments are reference data that do not change
 * through this API, so they are cached with a long TTL.
 */
public class CachingInstrumentPort implements LoadInstrumentPort {
    private final LoadInstrumentPort delegate;
    private final CacheRegion<Long, Instrument> instruments;
    private final CacheRegion<String, List<Instrument>> searches;

    /**
     * Creates the decorator.
     *
     * @param delegate    port that reads from the database
     * @param instruments instruments-by-id cache region
     * @param searches    search results cache region
     */
    public CachingInstrumentPort(LoadInstrumentPort delegate, CacheRegion<Long, Instrument> instruments,
            CacheRegion<String, List<Instrument>> searches) {
        this.delegate = delegate;
        this.instruments = instruments;
        this.searches = searches;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The query is normalized (trimmed, lower case) so equivalent searches share one cache entry. Empty
     * results are cached too.</p>
     */
    @Override
    public List<Instrument> searchByTickerOrName(String query) {
        String key = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Optional<List<Instrument>> cached = searches.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<Instrument> loaded = List.copyOf(delegate.searchByTickerOrName(query));
        searches.put(key, loaded);
        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Instrument> loadInstrumentById(Long instrumentId) {
        Optional<Instrument> cached = instruments.get(instrumentId);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<Instrument> loaded = delegate.loadInstrumentById(instrumentId);
        loaded.ifPresent(instrument -> instruments.put(instrumentId, instrument));
        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only the ids missing from the cache are loaded, in a single database query.</p>
     */
    @Override
    public List<Instrument> loadInstrumentsByIds(Collection<Long> instrumentIds) {
        Map<Long, Instrument> cached = instruments.getAll(instrumentIds);
        List<Instrument> result = new ArrayList<>(cached.values());
        List<Long> missing = instrumentIds.stream().filter(id -> !cached.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            List<Instrument> loaded = delegate.loadInstrumentsByIds(missing);
            loaded.forEach(instrument -> instruments.put(instrument.id(), instrument));
            result.addAll(loaded);
        }
        return result;
    }
}
