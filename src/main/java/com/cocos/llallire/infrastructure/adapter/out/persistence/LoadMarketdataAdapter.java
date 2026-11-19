package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.MarketdataRepository;

@Component
public class LoadMarketdataAdapter implements LoadMarketdataPort {

    private final MarketdataRepository marketdataRepository;

    public LoadMarketdataAdapter(MarketdataRepository marketdataRepository) {
        this.marketdataRepository = marketdataRepository;
    }

    @Override
    public Optional<Marketdata> loadLastMarketdataByInstrumentId(Long instrumentId) {
        return marketdataRepository.findFirstByInstrument_IdOrderByDateDescIdDesc(instrumentId)
            .map(EntityMapper::toDomain);
    }
}
