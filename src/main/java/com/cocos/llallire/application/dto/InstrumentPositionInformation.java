package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

public record InstrumentPositionInformation(
        Long instrumentId,
        String ticker,
        String name,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPct) {
}