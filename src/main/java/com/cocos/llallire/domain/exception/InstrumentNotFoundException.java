package com.cocos.llallire.domain.exception;

/**
 * The requested instrument does not exist.
 */
public class InstrumentNotFoundException extends ResourceNotFoundException {

    /**
     * Creates the exception for the given identifier.
     *
     * @param instrumentId requested identifier
     */
    public InstrumentNotFoundException(Long instrumentId) {
        super("Instrument %d not found".formatted(instrumentId));
    }
}
