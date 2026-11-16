package com.cocos.llallire.aplication.port.in;

import java.util.List;

import com.cocos.llallire.domain.model.Instrument;

public interface SearchInstrumentUseCase {
    List<Instrument> searchInstrumentsByName(String name);
    List<Instrument> searchInstrumentsByTicker(String ticker);
}
