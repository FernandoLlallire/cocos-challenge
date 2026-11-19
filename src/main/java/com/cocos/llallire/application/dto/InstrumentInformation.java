package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

import com.cocos.llallire.domain.model.InstrumentType;

/**
 * Instrument returned by the search.
 *
 * @param id             identifier
 * @param ticker         market symbol
 * @param name           name
 * @param type           instrument type
 * @param lastPrice      latest price ({@code close}) in pesos; {@code null} if there is no marketdata
 * @param dailyReturnPct daily return (%); {@code null} if there is no marketdata
 */
public record InstrumentInformation(
        Long id,
        String ticker,
        String name,
        InstrumentType type,
        BigDecimal lastPrice,
        BigDecimal dailyReturnPct) {
}
