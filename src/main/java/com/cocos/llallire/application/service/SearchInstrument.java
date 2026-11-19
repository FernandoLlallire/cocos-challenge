package com.cocos.llallire.application.service;

import java.util.List;
import java.util.Map;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;

/**
 * Searches instruments by ticker or name and adds their latest price and daily return.
 */
public class SearchInstrument implements SearchInstrumentUseCase {
    private final LoadInstrumentPort loadInstrumentPort;
    private final LoadMarketdataPort loadMarketdataPort;

    /**
     * Creates the use case.
     *
     * @param loadInstrumentPort instrument reads
     * @param loadMarketdataPort price reads
     */
    public SearchInstrument(LoadInstrumentPort loadInstrumentPort, LoadMarketdataPort loadMarketdataPort) {
        this.loadInstrumentPort = loadInstrumentPort;
        this.loadMarketdataPort = loadMarketdataPort;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Prices of all instruments found are loaded in a single query.</p>
     */
    @Override
    public List<InstrumentInformation> execute(String query) {
        List<Instrument> instruments = loadInstrumentPort.searchByTickerOrName(query);
        if (instruments.isEmpty()) {
            return List.of();
        }
        Map<Long, Marketdata> lastMarketdata = loadMarketdataPort.loadLastMarketdataByInstrumentIds(
                instruments.stream().map(Instrument::id).toList());
        return instruments.stream()
                .map(instrument -> toInformation(instrument, lastMarketdata.get(instrument.id())))
                .toList();
    }

    /**
     * Builds the DTO of an instrument.
     *
     * @param instrument instrument
     * @param marketdata latest marketdata, may be {@code null}
     * @return DTO with price and return, or with those fields {@code null} if there are no prices
     */
    private InstrumentInformation toInformation(Instrument instrument, Marketdata marketdata) {
        return new InstrumentInformation(
                instrument.id(),
                instrument.ticker(),
                instrument.name(),
                instrument.type(),
                marketdata == null ? null : marketdata.close(),
                marketdata == null ? null : marketdata.dailyReturnPct());
    }
}
