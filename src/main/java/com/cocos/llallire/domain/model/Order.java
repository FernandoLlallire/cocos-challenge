package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
    Long id,
    Long instrumentId,
    Long userId,
    Integer size,
    BigDecimal price,
    String type,
    String side,
    String status,
    LocalDateTime dateTime
) {
}
