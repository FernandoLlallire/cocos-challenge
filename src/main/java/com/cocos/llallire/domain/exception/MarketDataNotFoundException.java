package com.cocos.llallire.domain.exception;

public class MarketDataNotFoundException extends RuntimeException {
    public MarketDataNotFoundException(long instrumentId) {
        super("MarketData for instrumenId %d not found".formatted(instrumentId));
    }
}
