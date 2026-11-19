package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

import com.cocos.llallire.domain.shared.Precision;

public class AvailableCash {
    private BigDecimal amount;
    public AvailableCash(BigDecimal amount) {
        this.amount = amount;
    }
    public BigDecimal amount() {
        return amount;
    }    

    public void processOrder(Order order) {
        BigDecimal orderValue = order.price().multiply(BigDecimal.valueOf(order.size())).setScale(Precision.PERCENT_SCALE, Precision.ROUNDING);
        switch (order.side()) {
            case CASH_IN -> this.amount.add(orderValue);
            case CASH_OUT -> this.amount.subtract(orderValue);
            default -> throw new IllegalArgumentException("Invalid order side for AvailableCash: " + order.side());
        };
    }
}
