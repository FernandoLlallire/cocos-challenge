package com.cocos.llallire.application.dto;

import java.math.BigDecimal;

import com.cocos.llallire.domain.model.InstrumentType;

public record InstrumentInformation(Long id, String ticker, String name, InstrumentType type, BigDecimal performanceRate) {
}