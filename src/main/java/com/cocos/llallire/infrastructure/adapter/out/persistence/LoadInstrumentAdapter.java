package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.InstrumentEntity;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.InstrumentRepository;

@Component
public class LoadInstrumentAdapter implements LoadInstrumentPort {

    private final InstrumentRepository instrumentRepository;

    public LoadInstrumentAdapter(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    public List<Instrument> loadInstrumentByNameOrTicker(String query) {
        String searchTerm = query == null ? "" : query.trim();
        List<InstrumentEntity> entities = searchTerm.isEmpty()
            ? instrumentRepository.findAll()
            : instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(
                searchTerm,
                searchTerm
            );

        return entities.stream()
            .map(EntityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Instrument> loadInstrumentById(Long instrumentId) {
        return instrumentRepository.findById(instrumentId).map(EntityMapper::toDomain);
    }
}
