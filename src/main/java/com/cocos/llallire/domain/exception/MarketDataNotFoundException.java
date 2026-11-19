package com.cocos.llallire.domain.exception;

/**
 * There is no marketdata for the instrument, so it cannot be priced.
 */
public class MarketDataNotFoundException extends ResourceNotFoundException {

    /**
     * Creates the exception for the given instrument.
     *
     * @param instrumentId instrument without marketdata
     */
    public MarketDataNotFoundException(Long instrumentId) {
        super("Marketdata for instrument %d not found".formatted(instrumentId));
    }
}
