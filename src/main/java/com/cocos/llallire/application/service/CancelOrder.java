package com.cocos.llallire.application.service;

import com.cocos.llallire.application.dto.OrderInformation;
import com.cocos.llallire.application.port.in.CancelOrderUseCase;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.PortfolioCachePort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.domain.exception.OrderNotFoundException;
import com.cocos.llallire.domain.model.Order;

/**
 * Cancels a pending order. The rule (NEW orders only) lives in {@link Order#cancel()}.
 */
public class CancelOrder implements CancelOrderUseCase {
    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final PortfolioCachePort portfolioCache;

    /**
     * Creates the use case.
     *
     * @param loadOrderPort order reads
     * @param saveOrderPort  order writes
     * @param portfolioCache cache of portfolios, invalidated because the cancellation releases reserved funds
     */
    public CancelOrder(LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort, PortfolioCachePort portfolioCache) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.portfolioCache = portfolioCache;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The order is always read from the database. The user's cached portfolio is invalidated.</p>
     */
    @Override
    public OrderInformation execute(Long orderId) {
        Order order = loadOrderPort.loadOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Order cancelled = saveOrderPort.save(order.cancel());
        portfolioCache.invalidate(cancelled.userId());
        return OrderInformation.from(cancelled);
    }
}
