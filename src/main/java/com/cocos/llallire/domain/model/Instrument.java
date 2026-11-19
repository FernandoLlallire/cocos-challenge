package com.cocos.llallire.domain.model;

/**
 * Tradable instrument (stock or currency).
 *
 * @param id     identifier
 * @param ticker market symbol
 * @param name   descriptive name
 * @param type   instrument type
 */
public record Instrument(Long id, String ticker, String name, InstrumentType type) {

    /**
     * Tells whether the instrument represents cash.
     *
     * @return {@code true} if its type is {@link InstrumentType#MONEDA}
     */
    public boolean isCash() {
        return type == InstrumentType.MONEDA;
    }
}
