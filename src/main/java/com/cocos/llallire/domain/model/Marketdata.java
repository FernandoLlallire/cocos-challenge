package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Marketdata(
    Long id,
    Long instrumentId,
    BigDecimal high,
    BigDecimal low,
    BigDecimal open,
    BigDecimal close,
    BigDecimal previousClose,
    LocalDate date
) {
}
