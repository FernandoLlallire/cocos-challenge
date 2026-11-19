package com.cocos.llallire.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cocos.llallire.domain.exception.InstrumentNotFoundException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.model.CashPosition;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.InstrumentPosition;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.Portfolio;
import com.cocos.llallire.domain.model.PositionValuation;
import com.cocos.llallire.domain.shared.Precision;

/**
 * Domain service that builds a user's portfolio from their orders and market prices.
 * It is stateless and has no infrastructure dependencies: it receives all data already loaded.
 */
public class PortfolioCalculator {

    /**
     * Calculates the portfolio.
     *
     * @param orders         user orders (FILLED and NEW are used; the rest is ignored)
     * @param instruments    instruments referenced by the orders, indexed by id
     * @param lastMarketdata latest marketdata of each instrument, indexed by instrument id
     * @return total account value, available pesos and positions with holdings greater than zero
     * @throws InstrumentNotFoundException if an order references an instrument missing from the map
     * @throws MarketDataNotFoundException if a held position has no market price
     */
    public Portfolio calculate(List<Order> orders, Map<Long, Instrument> instruments,
            Map<Long, Marketdata> lastMarketdata) {
        CashPosition cash = new CashPosition();
        cash.processOrders(orders);

        List<PositionValuation> positions = orders.stream()
                .map(Order::instrumentId)
                .distinct()
                .map(instrumentId -> findInstrument(instruments, instrumentId))
                .filter(instrument -> !instrument.isCash())
                .map(instrument -> valuatePosition(instrument, orders, lastMarketdata))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(valuation -> valuation.instrument().ticker()))
                .toList();

        BigDecimal positionsValue = positions.stream()
                .map(PositionValuation::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccountValue = cash.balance().add(positionsValue)
                .setScale(Precision.MONEY_SCALE, Precision.ROUNDING);

        return new Portfolio(totalAccountValue, cash.available(), positions);
    }

    /**
     * Calculates the position of an instrument and values it if there is a holding.
     *
     * @param instrument     instrument of the position
     * @param orders         user orders
     * @param lastMarketdata latest marketdata by instrument
     * @return the valuation, or empty if the holding is zero
     */
    private Optional<PositionValuation> valuatePosition(Instrument instrument, List<Order> orders,
            Map<Long, Marketdata> lastMarketdata) {
        InstrumentPosition position = new InstrumentPosition(instrument);
        position.processOrders(orders);
        if (position.quantity() <= 0) {
            return Optional.empty();
        }
        Marketdata marketdata = Optional.ofNullable(lastMarketdata.get(instrument.id()))
                .orElseThrow(() -> new MarketDataNotFoundException(instrument.id()));
        return Optional.of(position.valuate(marketdata));
    }

    /**
     * Looks up an instrument in the map.
     *
     * @param instruments  instruments indexed by id
     * @param instrumentId id to look up
     * @return the instrument
     * @throws InstrumentNotFoundException if it is not in the map
     */
    private Instrument findInstrument(Map<Long, Instrument> instruments, Long instrumentId) {
        return Optional.ofNullable(instruments.get(instrumentId))
                .orElseThrow(() -> new InstrumentNotFoundException(instrumentId));
    }
}
