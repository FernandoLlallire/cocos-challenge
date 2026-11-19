package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.InstrumentEntity;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.InstrumentRepository;

/**
 * JPA adapter for {@link LoadInstrumentPort}.
 */
@Component
public class InstrumentPersistenceAdapter implements LoadInstrumentPort {
    private final InstrumentRepository instrumentRepository;

    /**
     * Creates the adapter.
     *
     * @param instrumentRepository instrument repository
     */
    public InstrumentPersistenceAdapter(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Instrument> searchByTickerOrName(String query) {
        String searchTerm = query == null ? "" : query.trim();
        List<InstrumentEntity> entities = searchTerm.isEmpty()
                ? instrumentRepository.findAll(Sort.by("ticker"))
                : instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByTickerAsc(
                        searchTerm, searchTerm);
        return entities.stream().map(EntityMapper::toDomain).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Instrument> loadInstrumentById(Long instrumentId) {
        return instrumentRepository.findById(instrumentId).map(EntityMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Instrument> loadInstrumentsByIds(Collection<Long> instrumentIds) {
        return instrumentRepository.findAllById(instrumentIds).stream().map(EntityMapper::toDomain).toList();
    }
}
