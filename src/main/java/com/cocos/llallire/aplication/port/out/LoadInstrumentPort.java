package com.cocos.llallire.aplication.port.out;

import java.util.List;

import com.cocos.llallire.domain.model.Instrument;

public interface LoadInstrumentPort {
    List<Instrument> loadInstrumentByName(String name);
    List<Instrument> loadInstrumentByTicker(String ticker);

}
