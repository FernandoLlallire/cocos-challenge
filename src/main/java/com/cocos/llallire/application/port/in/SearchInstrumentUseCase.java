package com.cocos.llallire.application.port.in;

import java.util.List;

import com.cocos.llallire.application.dto.InstrumentInformation;

public interface SearchInstrumentUseCase {
    List<InstrumentInformation> execute(String query);
}
