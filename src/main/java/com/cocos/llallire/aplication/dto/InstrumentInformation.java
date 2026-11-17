package com.cocos.llallire.aplication.dto;

import java.math.BigDecimal;

public record InstrumentInformation(Long id, String ticker, String name, String type, BigDecimal performanceRate) {
}