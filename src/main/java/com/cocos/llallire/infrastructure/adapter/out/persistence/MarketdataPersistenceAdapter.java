package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.MarketdataRepository;

/**
 * JPA adapter for {@link LoadMarketdataPort}.
 */
@Component
public class MarketdataPersistenceAdapter implements LoadMarketdataPort {
    private final MarketdataRepository marketdataRepository;

    /**
     * Creates the adapter.
     *
     * @param marketdataRepository marketdata repository
     */
    public MarketdataPersistenceAdapter(MarketdataRepository marketdataRepository) {
        this.marketdataRepository = marketdataRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Marketdata> loadLastMarketdataByInstrumentId(Long instrumentId) {
        return marketdataRepository.findFirstByInstrumentIdOrderByDateDescIdDesc(instrumentId)
                .map(EntityMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If an instrument has more than one row on its latest date, the one with the highest id is kept.</p>
     */
    @Override
    public Map<Long, Marketdata> loadLastMarketdataByInstrumentIds(Collection<Long> instrumentIds) {
        if (instrumentIds.isEmpty()) {
            return Map.of();
        }
        return marketdataRepository.findLatestByInstrumentIds(instrumentIds).stream()
                .map(EntityMapper::toDomain)
                .collect(Collectors.toMap(Marketdata::instrumentId, marketdata -> marketdata,
                        BinaryOperator.maxBy(Comparator.comparing(Marketdata::id))));
    }
}
