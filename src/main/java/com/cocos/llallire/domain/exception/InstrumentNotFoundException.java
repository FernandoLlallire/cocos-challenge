package com.cocos.llallire.domain.exception;

public class InstrumentNotFoundException extends RuntimeException {
    public InstrumentNotFoundException(long instrumentId) {
        super("Instrument %d not found".formatted(instrumentId));
    }
}
