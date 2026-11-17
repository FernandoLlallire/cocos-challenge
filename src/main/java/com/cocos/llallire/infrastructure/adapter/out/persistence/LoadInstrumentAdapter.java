package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.List;

import com.cocos.llallire.aplication.port.out.LoadInstrumentPort;
import com.cocos.llallire.domain.model.Instrument;

public class LoadInstrumentAdapter implements LoadInstrumentPort {

    @Override
    public List<Instrument> loadInstrumentByNameOrTicker(String query) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadInstrumentByNameOrTicker'");
    }
    
}
