package com.cocos.llallire.application.service;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;

public class SearchInstrument implements SearchInstrumentUseCase {
    private final LoadInstrumentPort loadInstrumentPort;
    private final LoadMarketdataPort loadMarketdataPort;

    public SearchInstrument(LoadInstrumentPort loadInstrumentPort, LoadMarketdataPort loadMarketdataPort) {
        this.loadInstrumentPort = loadInstrumentPort;
        this.loadMarketdataPort = loadMarketdataPort;
    }

    @Override
    public List<InstrumentInformation> execute(String query) {
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