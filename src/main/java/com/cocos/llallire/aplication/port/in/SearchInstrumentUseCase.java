package com.cocos.llallire.aplication.port.in;

import java.util.List;

import com.cocos.llallire.aplication.dto.InstrumentInformation;

public interface SearchInstrumentUseCase {
    List<InstrumentInformation> searchInstruments(String query);
}
