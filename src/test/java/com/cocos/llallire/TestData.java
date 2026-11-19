package com.cocos.llallire;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.InstrumentType;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;

/**
 * Factory of domain objects for unit tests.
 */
public final class TestData {

    /** User used in the tests. */
    public static final Long USER_ID = 1L;
    /** Test stock. */
    public static final Instrument PAMP = new Instrument(47L, "PAMP", "Pampa Holding S.A.", InstrumentType.ACCIONES);
    /** Another test stock. */
    public static final Instrument METR = new Instrument(54L, "METR", "MetroGAS S.A.", InstrumentType.ACCIONES);
    /** Peso instrument. */
    public static final Instrument ARS = new Instrument(66L, "ARS", "PESOS", InstrumentType.MONEDA);
    /** Base date for orders. */
    public static final LocalDateTime T0 = LocalDateTime.of(2023, 7, 12, 10, 0);

    private static long nextId = 1;

    /**
     * Utility class; not instantiable.
     */
    private TestData() {
    }

    /**
     * Creates an order for the test user.
     *
     * @param instrument instrument
     * @param side       side
     * @param status     status
     * @param size       quantity
     * @param price      price
     * @param minutes    minutes since {@link #T0}
     * @return order
     */
    public static Order order(Instrument instrument, OrderSide side, OrderStatus status, long size, String price,
            int minutes) {
        OrderType type = status == OrderStatus.NEW ? OrderType.LIMIT : OrderType.MARKET;
        return new Order(nextId++, instrument.id(), USER_ID, size, new BigDecimal(price), type, side, status,
                T0.plusMinutes(minutes));
    }

    /**
     * Creates an executed CASH_IN.
     *
     * @param amount amount in pesos
     * @return incoming transfer order
     */
    public static Order cashIn(long amount) {
        return order(ARS, OrderSide.CASH_IN, OrderStatus.FILLED, amount, "1", 0);
    }

    /**
     * Creates a marketdata with the given close and previous close.
     *
     * @param instrument    instrument
     * @param close         latest price
     * @param previousClose previous close
     * @return marketdata
     */
    public static Marketdata marketdata(Instrument instrument, String close, String previousClose) {
        return new Marketdata(1L, instrument.id(), null, null, null, new BigDecimal(close),
                new BigDecimal(previousClose), LocalDate.of(2023, 7, 14));
    }
}
