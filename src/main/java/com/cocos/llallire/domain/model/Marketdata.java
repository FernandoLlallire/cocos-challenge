package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cocos.llallire.domain.shared.Returns;

/**
 * Daily prices of an instrument, in pesos.
 *
 * @param id            identifier
 * @param instrumentId  instrument the prices belong to
 * @param high          daily high
 * @param low           daily low
 * @param open          opening price
 * @param close         latest price of the instrument
 * @param previousClose previous day's close
 * @param date          date of the prices
 */
public record Marketdata(
        Long id,
        Long instrumentId,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        BigDecimal close,
        BigDecimal previousClose,
        LocalDate date) {

    /**
     * Calculates the daily return: {@code (close - previousClose) / previousClose * 100}.
     *
     * @return percentage return, or zero if any price is missing
     */
    public BigDecimal dailyReturnPct() {
        return Returns.percentChange(previousClose, close);
    }
}
