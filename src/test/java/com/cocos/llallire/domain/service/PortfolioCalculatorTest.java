package com.cocos.llallire.domain.service;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.METR;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.marketdata;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cocos.llallire.domain.exception.InstrumentNotFoundException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.Portfolio;
import com.cocos.llallire.domain.model.PositionValuation;

/**
 * Tests of {@link PortfolioCalculator}.
 */
class PortfolioCalculatorTest {

    private static final Map<Long, Instrument> INSTRUMENTS = Map.of(PAMP.id(), PAMP, METR.id(), METR, ARS.id(), ARS);
    private static final Map<Long, Marketdata> PRICES = Map.of(
            PAMP.id(), marketdata(PAMP, "1000", "990"),
            METR.id(), marketdata(METR, "250", "240"));

    private final PortfolioCalculator calculator = new PortfolioCalculator();

    @Test
    void calculatesTotalValueAvailableCashAndPositions() {
        Portfolio portfolio = calculator.calculate(List.of(
                cashIn(100_000),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 10, "900", 1),
                order(METR, OrderSide.BUY, OrderStatus.FILLED, 100, "200", 2),
                order(METR, OrderSide.BUY, OrderStatus.NEW, 10, "190", 3)),
                INSTRUMENTS, PRICES);

        // balance = 100000 - 9000 - 20000 = 71000; reserved = 1900; positions = 10000 + 25000
        assertThat(portfolio.availableCash()).isEqualByComparingTo("69100.00");
        assertThat(portfolio.totalAccountValue()).isEqualByComparingTo("106000.00");
        assertThat(portfolio.positions())
                .extracting(PositionValuation::instrument, PositionValuation::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(METR, 100L),
                        org.assertj.core.groups.Tuple.tuple(PAMP, 10L));
        assertThat(portfolio.positions().get(0).totalReturnPct()).isEqualByComparingTo("25.00");
    }

    @Test
    void excludesCashAndPositionsWithoutHoldings() {
        Portfolio portfolio = calculator.calculate(List.of(
                cashIn(10_000),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 5, "900", 1),
                order(PAMP, OrderSide.SELL, OrderStatus.FILLED, 5, "950", 2)),
                INSTRUMENTS, PRICES);

        assertThat(portfolio.positions()).isEmpty();
        assertThat(portfolio.totalAccountValue()).isEqualByComparingTo("10250.00");
    }

    @Test
    void emptyOrdersProduceAnEmptyPortfolio() {
        Portfolio portfolio = calculator.calculate(List.of(), Map.of(), Map.of());

        assertThat(portfolio.totalAccountValue()).isEqualByComparingTo("0");
        assertThat(portfolio.availableCash()).isEqualByComparingTo("0");
        assertThat(portfolio.positions()).isEmpty();
    }

    @Test
    void failsWhenAHeldPositionHasNoMarketdata() {
        var orders = List.of(order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 1, "900", 1));

        assertThatThrownBy(() -> calculator.calculate(orders, INSTRUMENTS, Map.of()))
                .isInstanceOf(MarketDataNotFoundException.class);
    }

    @Test
    void failsWhenAnOrderReferencesAnUnknownInstrument() {
        var orders = List.of(order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 1, "900", 1));

        assertThatThrownBy(() -> calculator.calculate(orders, Map.of(), PRICES))
                .isInstanceOf(InstrumentNotFoundException.class);
    }
}
