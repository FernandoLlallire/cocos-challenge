package com.cocos.llallire.aplication.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cocos.llallire.aplication.dto.InstrumentInformation;
import com.cocos.llallire.aplication.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.aplication.port.out.LoadInstrumentPort;
import com.cocos.llallire.aplication.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;

@Service
public class SearchInstrument implements SearchInstrumentUseCase {
    private final LoadInstrumentPort loadInstrumentPort;
    private final LoadMarketdataPort loadMarketdataPort;

    public SearchInstrument(LoadInstrumentPort loadInstrumentPort, LoadMarketdataPort loadMarketdataPort) {
        this.loadInstrumentPort = loadInstrumentPort;
        this.loadMarketdataPort = loadMarketdataPort;
    }

    @Override
    public List<InstrumentInformation> searchInstruments(String query) {
        return loadInstrumentPort.loadInstrumentByNameOrTicker(query)
                .stream()
                .map(this::toInformation)
                .toList();
    }

    private InstrumentInformation toInformation(Instrument instrument) {
        BigDecimal performanceRate = loadMarketdataPort.loadMarketdataByInstrumentId(instrument.id())
                .map(Marketdata::performanceRate)
                .orElse(null);
        return new InstrumentInformation(instrument.id(), instrument.ticker(), instrument.name(),
                instrument.type(), performanceRate);
    }
}