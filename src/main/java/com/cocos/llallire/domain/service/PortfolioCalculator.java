package com.cocos.llallire.domain.service;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.llallire.domain.model.TotalEquity;
import com.cocos.llallire.domain.shared.Precision;
import com.cocos.llallire.domain.exception.InvalidInstrumentException;
import com.cocos.llallire.domain.model.AvailableCash;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.InstrumentPosition;
import com.cocos.llallire.domain.model.InstrumentType;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;

public class PortfolioCalculator {

    public TotalEquity getTotalEquity(Long userId, List<Order> filledOrders) {
        TotalEquity totalEquity = new TotalEquity(BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE));
        filledOrders.stream()
                .filter(order -> order.userId().equals(userId) && order.status() == OrderStatus.FILLED)
                .forEach(order -> totalEquity.processOrder(order));
        return totalEquity;
    }

    public AvailableCash getUserAvailableCash(Long userId, List<Order> filledOrders) {
        AvailableCash availableCash = new AvailableCash(BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE));
        filledOrders.stream()
                .filter(order -> order.userId().equals(userId)
                        && order.status() == OrderStatus.FILLED
                        && (order.side() == OrderSide.CASH_IN || order.side() == OrderSide.CASH_OUT))
                .forEach(order -> availableCash.processOrder(order));
        return availableCash;
    }

    public InstrumentPosition getUserInstrumentPosition(Instrument instrument, Marketdata lastMarketdata, List<Order> filledOrders) {
        if (instrument.type() == InstrumentType.MONEDA) {
            throw new InvalidInstrumentException(instrument.type());
        }
        InstrumentPosition instrumentPosition = new InstrumentPosition(instrument, lastMarketdata);
        filledOrders.stream()
                .filter(order -> order.instrumentId().equals(instrument.id())
                        && order.status() == OrderStatus.FILLED)
                .forEach(order -> {
                    instrumentPosition.processOrder(order);
                });
        return instrumentPosition;
    }
}
