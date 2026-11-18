package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

public record Position(
        Instrument instrument,
        long quantity,
        BigDecimal marketValue,
        BigDecimal totalReturnPct) {
}
