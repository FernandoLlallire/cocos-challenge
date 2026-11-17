package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.cocos.llallire.aplication.port.out.LoadMarketdataPort;
import com.cocos.llallire.domain.model.Marketdata;

public class LoadMarketdataAdapter implements LoadMarketdataPort {

    @Override
    public Optional<Marketdata> loadMarketdataByInstrumentId(String instrumentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadMarketdataByInstrumentId'");
    }
    
}
