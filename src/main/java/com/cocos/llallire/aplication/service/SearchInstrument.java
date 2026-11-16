package com.cocos.llallire.aplication.service;

import java.util.List;

import com.cocos.llallire.aplication.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.domain.model.Instrument;

/**
 * SearchInstrument
 */
public class SearchInstrument implements SearchInstrumentUseCase {

    @Override
    public List<Instrument> searchInstrumentsByName(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchInstrumentsByName'");
    }

    @Override
    public List<Instrument> searchInstrumentsByTicker(String ticker) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchInstrumentsByTicker'");
    }
    
}