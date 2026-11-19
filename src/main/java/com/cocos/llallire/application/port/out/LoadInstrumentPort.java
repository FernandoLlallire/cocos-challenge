package com.cocos.llallire.application.port.out;

import java.util.List;
import java.util.Optional;

import com.cocos.llallire.domain.model.Instrument;

public interface LoadInstrumentPort {
    List<Instrument> loadInstrumentByNameOrTicker(String query);
    Optional<Instrument> loadInstrumentById(Long instrumentId);
}
