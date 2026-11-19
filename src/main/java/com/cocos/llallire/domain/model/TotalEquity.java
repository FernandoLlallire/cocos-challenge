package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

public class TotalEquity {
    private BigDecimal amount;

    public TotalEquity(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal amount() {
        return amount;
    }

    public void processOrder(Order order) {
        BigDecimal orderValue = order.price().multiply(BigDecimal.valueOf(order.size()));
        switch (order.side()) {
            case CASH_IN, SELL -> this.amount.add(orderValue);
            case CASH_OUT, BUY -> this.amount.subtract(orderValue);
            default -> throw new IllegalArgumentException("Invalid order side for TotalEquity: " + order.side());
        };
    }
}