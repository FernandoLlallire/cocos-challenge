package com.cocos.llallire.aplication.port.out;

import java.util.Optional;

import com.cocos.llallire.domain.model.Marketdata;

public interface LoadMarketdataPort {
    Optional<Marketdata> loadMarketdataByInstrumentId(String instrumentId);
}
