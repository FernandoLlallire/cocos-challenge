package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

import com.cocos.llallire.domain.model.PositionValuation;

/**
 * Position of an asset within the portfolio.
 *
 * @param instrumentId   instrument identifier
 * @param ticker         market symbol
 * @param name           name
 * @param quantity       number of shares
 * @param marketValue    total value of the position in pesos
 * @param totalReturnPct total return (%)
 */
public record InstrumentPositionInformation(
        Long instrumentId,
        String ticker,
        String name,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPct) {

    /**
     * Converts a domain valuation into the output DTO.
     *
     * @param valuation valued position
     * @return equivalent DTO
     */
    public static InstrumentPositionInformation from(PositionValuation valuation) {
        return new InstrumentPositionInformation(
                valuation.instrument().id(),
                valuation.instrument().ticker(),
                valuation.instrument().name(),
                valuation.quantity(),
                valuation.marketValue(),
                valuation.totalReturnPct());
    }
}
