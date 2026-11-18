package com.cocos.llallire.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioInformation(
        BigDecimal totalValue,
        BigDecimal availableCash,
        List<PositionInformation> positions) {
}