package com.cocos.llallire.application.port.out;

import java.util.Optional;

import com.cocos.llallire.domain.model.Marketdata;

public interface LoadMarketdataPort {
    Optional<Marketdata> loadMarketdataByInstrumentId(Long instrumentId);
}
