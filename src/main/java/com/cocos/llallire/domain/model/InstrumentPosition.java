package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

import com.cocos.llallire.domain.shared.Precision;

public class InstrumentPosition {
    private Instrument instrument;
    private Marketdata lastMarketdata;
    private long quantity;
    private BigDecimal marketValue;
    private BigDecimal totalReturnPct;

    public InstrumentPosition(Instrument instrument, Marketdata lastMarketdata) {
        this.instrument = instrument;
        this.lastMarketdata = lastMarketdata;
        this.quantity = 0;
        this.marketValue = BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE);
        this.totalReturnPct = BigDecimal.ZERO.setScale(Precision.PERCENT_SCALE);
    }

    public Instrument instrument() {
        return instrument;
    }

    public Marketdata lastMarketdata() {
        return lastMarketdata;
    }

    public long quantity() {
        return quantity;
    }

    public BigDecimal marketValue() {
        return marketValue;
    }

    public BigDecimal totalReturnPct() {
        return totalReturnPct;
    }

    public void processOrder(Order order) {
    }

}
