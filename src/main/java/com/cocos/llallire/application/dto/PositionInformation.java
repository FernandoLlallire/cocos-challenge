package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

public record PositionInformation(
        Long instrumentId,
        String ticker,
        String name,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPct) {
}
