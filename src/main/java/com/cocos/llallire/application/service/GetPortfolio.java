package com.cocos.llallire.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.port.out.PortfolioCachePort;
import com.cocos.llallire.domain.exception.UserNotFoundException;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.Portfolio;
import com.cocos.llallire.domain.service.PortfolioCalculator;

/**
 * Queries a user's portfolio: loads their orders, the instruments and prices involved
 * and delegates the calculation to {@link PortfolioCalculator}.
 *
 * <p>The result is cached per user through {@link PortfolioCachePort}; order use cases invalidate it.</p>
 */
public class GetPortfolio implements GetPortfolioUseCase {

    private final LoadUserPort userPort;
    private final LoadOrderPort orderPort;
    private final LoadInstrumentPort instrumentPort;
    private final LoadMarketdataPort marketdataPort;
    private final PortfolioCalculator portfolioCalculator;
    private final PortfolioCachePort portfolioCache;

    /**
     * Creates the use case.
     *
     * @param userPort            user reads
     * @param orderPort           order reads
     * @param instrumentPort      instrument reads
     * @param marketdataPort      price reads
     * @param portfolioCalculator portfolio calculation
     * @param portfolioCache      cache of calculated portfolios
     */
    public GetPortfolio(LoadUserPort userPort, LoadOrderPort orderPort, LoadInstrumentPort instrumentPort,
            LoadMarketdataPort marketdataPort, PortfolioCalculator portfolioCalculator,
            PortfolioCachePort portfolioCache) {
        this.userPort = userPort;
        this.orderPort = orderPort;
        this.instrumentPort = instrumentPort;
        this.marketdataPort = marketdataPort;
        this.portfolioCalculator = portfolioCalculator;
        this.portfolioCache = portfolioCache;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On a cache hit no query is executed. On a miss it runs one query per table (orders, instruments and
     * marketdata), regardless of the number of positions; users, instruments and marketdata may also come from
     * their own caches.</p>
     */
    @Override
    public PortfolioInformation execute(Long userId) {
        return portfolioCache.getOrCompute(userId, () -> calculate(userId));
    }

    /**
     * Calculates the portfolio from the persisted data.
     *
     * @param userId user
     * @return calculated portfolio
     * @throws UserNotFoundException if the user does not exist
     */
    private PortfolioInformation calculate(Long userId) {
        userPort.loadUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        List<Order> orders = orderPort.loadOrdersByUserIdAndStatuses(userId, OrderStatus.affectingHoldings());
        List<Long> instrumentIds = orders.stream().map(Order::instrumentId).distinct().toList();

        Map<Long, Instrument> instruments = instrumentIds.isEmpty()
                ? Map.of()
                : instrumentPort.loadInstrumentsByIds(instrumentIds).stream()
                        .collect(Collectors.toMap(Instrument::id, Function.identity()));
        Map<Long, Marketdata> lastMarketdata = instrumentIds.isEmpty()
                ? Map.of()
                : marketdataPort.loadLastMarketdataByInstrumentIds(instrumentIds);

        Portfolio portfolio = portfolioCalculator.calculate(orders, instruments, lastMarketdata);
        return PortfolioInformation.from(portfolio);
    }
}
