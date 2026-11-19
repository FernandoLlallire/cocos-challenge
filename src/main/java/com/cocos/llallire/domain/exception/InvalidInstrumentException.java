package com.cocos.llallire.domain.exception;

import com.cocos.llallire.domain.model.InstrumentType;

public class InvalidInstrumentException extends RuntimeException {
    public InvalidInstrumentException(InstrumentType instrumentType) {
        super("Instrument %d is not valid not found".formatted(instrumentType.toString()));
    }
}
