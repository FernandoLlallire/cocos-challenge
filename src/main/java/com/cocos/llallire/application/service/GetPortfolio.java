package com.cocos.llallire.application.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.cocos.llallire.application.dto.InstrumentPositionInformation;
import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.exception.InstrumentNotFoundException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.exception.UserNotFoundException;
import com.cocos.llallire.domain.model.AvailableCash;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.InstrumentPosition;
import com.cocos.llallire.domain.model.InstrumentType;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.TotalEquity;
import com.cocos.llallire.domain.service.PortfolioCalculator;

public class GetPortfolio implements GetPortfolioUseCase {
    private final LoadUserPort userPort;
    private final LoadOrderPort orderPort;
    private final PortfolioCalculator portfolioCalculator;
    private final LoadInstrumentPort instrumentPort;
    private final LoadMarketdataPort marketdataPort;

    public GetPortfolio(LoadUserPort userPort, LoadOrderPort orderPort, PortfolioCalculator portfolioCalculator,
            LoadInstrumentPort loadInstrumentPort, LoadMarketdataPort loadMarketdataPort) {
        this.userPort = userPort;
        this.orderPort = orderPort;
        this.portfolioCalculator = portfolioCalculator;
        this.instrumentPort = loadInstrumentPort;
        this.marketdataPort = loadMarketdataPort;
    }

    @Override
    public PortfolioInformation execute(Long userId) {
        userPort.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        List<Order> filledOrders = orderPort.getFilledOrdersByUserId(userId);
        TotalEquity totalEquity = portfolioCalculator.getTotalEquity(userId, filledOrders);
        AvailableCash availableCash = portfolioCalculator.getUserAvailableCash(userId, filledOrders);
        List<InstrumentPosition> instrumentPositions = getInstrumenPositions(filledOrders);
        List<InstrumentPositionInformation> instrumentPositionInformations = instrumentPositions.stream()
                .map(instrumentPosition -> new InstrumentPositionInformation(instrumentPosition.instrument().id(),
                        instrumentPosition.instrument().ticker(), instrumentPosition.instrument().name(),
                        instrumentPosition.quantity(), instrumentPosition.marketValue(),
                        instrumentPosition.totalReturnPct()))
                .toList();
        return new PortfolioInformation(totalEquity.amount(), availableCash.amount(), instrumentPositionInformations);
    }

    private List<InstrumentPosition> getInstrumenPositions(List<Order> filledOrders) {
        Map<Long, List<Order>> ordersByInstrument = filledOrders.stream()
                .collect(Collectors.groupingBy(Order::instrumentId));
        return ordersByInstrument.entrySet().stream()
                .map(entry -> getInstrumentPosition(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .filter(instrumentPosition -> instrumentPosition.quantity() > 0)
                .toList();
    }

    private Optional<InstrumentPosition> getInstrumentPosition(Long instrumentId, List<Order> filledOrders) {
        Instrument instrument = instrumentPort.loadInstrumentById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException(instrumentId));
        if (instrument.type() == InstrumentType.MONEDA) {
            return Optional.empty();
        }
        Marketdata marketdata = marketdataPort.loadLastMarketdataByInstrumentId(instrumentId)
                .orElseThrow(() -> new MarketDataNotFoundException(instrumentId));
        InstrumentPosition instrumentPosition = new InstrumentPosition(instrument, marketdata);
        filledOrders.forEach(filledOrder -> instrumentPosition.processOrder(filledOrder));
        return Optional.of(instrumentPosition);
    }
}
