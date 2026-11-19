package com.cocos.llallire.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.dto.PlaceOrderCommand;
import com.cocos.llallire.application.port.in.PlaceOrderUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.port.out.PortfolioCachePort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.domain.exception.InstrumentNotFoundException;
import com.cocos.llallire.domain.exception.UserNotFoundException;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderRequest;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;
import com.cocos.llallire.domain.service.OrderPlacementService;

/**
 * Sends an order to the market: loads the required data, delegates the decision to {@link OrderPlacementService}
 * and persists the resulting order.
 *
 * <p>Must run inside a transaction: the user is locked first so two concurrent orders
 * cannot use the same funds.</p>
 */
public class PlaceOrder implements PlaceOrderUseCase {
    private final LoadUserPort userPort;
    private final LoadInstrumentPort instrumentPort;
    private final LoadMarketdataPort marketdataPort;
    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final OrderPlacementService orderPlacementService;
    private final PortfolioCachePort portfolioCache;
    private final Clock clock;

    /**
     * Creates the use case.
     *
     * @param userPort              user reads
     * @param instrumentPort        instrument reads
     * @param marketdataPort        price reads
     * @param loadOrderPort         order reads
     * @param saveOrderPort         order writes
     * @param orderPlacementService order placement rules
     * @param portfolioCache        cache of portfolios, invalidated when the order changes the user's holdings
     * @param clock                 clock used to timestamp the order
     */
    public PlaceOrder(LoadUserPort userPort, LoadInstrumentPort instrumentPort, LoadMarketdataPort marketdataPort,
            LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort, OrderPlacementService orderPlacementService,
            PortfolioCachePort portfolioCache, Clock clock) {
        this.userPort = userPort;
        this.instrumentPort = instrumentPort;
        this.marketdataPort = marketdataPort;
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.orderPlacementService = orderPlacementService;
        this.portfolioCache = portfolioCache;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cash and holdings are always read from the database (never from a cache) because the order is
     * validated against them. Instrument and market price may come from their caches. FILLED and NEW orders
     * invalidate the user's cached portfolio; REJECTED orders do not change it.</p>
     */
    @Override
    public OrderInformation execute(PlaceOrderCommand command) {
        OrderRequest request = command.toOrderRequest();
        userPort.loadUserByIdForUpdate(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        Instrument instrument = instrumentPort.loadInstrumentById(request.instrumentId())
                .orElseThrow(() -> new InstrumentNotFoundException(request.instrumentId()));
        BigDecimal marketPrice = needsMarketPrice(request, instrument) ? loadMarketPrice(instrument.id()) : null;
        List<Order> userOrders = loadOrderPort.loadOrdersByUserIdAndStatuses(request.userId(),
                OrderStatus.affectingHoldings());

        Order order = orderPlacementService.place(request, instrument, marketPrice, userOrders,
                LocalDateTime.now(clock));
        Order saved = saveOrderPort.save(order);
        if (saved.status() != OrderStatus.REJECTED) {
            portfolioCache.invalidate(saved.userId());
        }
        return OrderInformation.from(saved);
    }

    /**
     * Tells whether the order runs at market price (MARKET buy or sell of a stock).
     *
     * @param request    request
     * @param instrument instrument to trade
     * @return {@code true} if the latest price is needed
     */
    private boolean needsMarketPrice(OrderRequest request, Instrument instrument) {
        return request.type() == OrderType.MARKET && !request.side().isCashTransfer() && !instrument.isCash();
    }

    /**
     * Gets the latest price ({@code close}) of the instrument. If there is no marketdata it returns {@code null}:
     * the domain service decides whether that is an error, after validating the request.
     *
     * @param instrumentId instrument
     * @return latest price, or {@code null} if there is no marketdata
     */
    private BigDecimal loadMarketPrice(Long instrumentId) {
        return marketdataPort.loadLastMarketdataByInstrumentId(instrumentId)
                .map(Marketdata::close)
                .orElse(null);
    }
}
