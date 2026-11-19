package com.cocos.llallire.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.cocos.llallire.application.port.in.CancelOrderUseCase;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;
import com.cocos.llallire.application.port.in.PlaceOrderUseCase;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.port.out.PortfolioCachePort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.application.service.CancelOrder;
import com.cocos.llallire.application.service.GetPortfolio;
import com.cocos.llallire.application.service.PlaceOrder;
import com.cocos.llallire.application.service.SearchInstrument;
import com.cocos.llallire.domain.service.OrderPlacementService;
import com.cocos.llallire.domain.service.PortfolioCalculator;
import com.cocos.llallire.infrastructure.observability.ObservedCancelOrderUseCase;
import com.cocos.llallire.infrastructure.observability.ObservedPlaceOrderUseCase;
import com.cocos.llallire.infrastructure.observability.OrderMetrics;
import com.cocos.llallire.infrastructure.observability.UseCaseTelemetry;

/**
 * Wires the use cases and wraps them in transactions.
 *
 * <p>The application layer does not depend on Spring, so the transaction boundary is added here with a
 * decorator based on {@link TransactionTemplate}: write use cases use a read/write transaction and query
 * use cases a read-only one, to read a consistent state.</p>
 *
 * <p>The outbound ports injected here are the cached decorators registered in {@link CacheConfig}.</p>
 *
 * <p>Each use case is also wrapped, outside the transaction, with telemetry (a span, a duration metric and, for
 * orders, business metrics and an audit log). The resulting chain is
 * {@code telemetry → transaction → use case → (cached) ports}.</p>
 */
@Configuration
public class UseCasesConfig {

    /**
     * Instrument search use case.
     *
     * @param loadInstrumentPort instrument reads
     * @param loadMarketdataPort price reads
     * @param transactionManager transaction manager
     * @param telemetry          use case telemetry
     * @return observed, read-only transactional use case
     */
    @Bean
    public SearchInstrumentUseCase searchInstrumentUseCase(LoadInstrumentPort loadInstrumentPort,
            LoadMarketdataPort loadMarketdataPort, PlatformTransactionManager transactionManager,
            UseCaseTelemetry telemetry) {
        SearchInstrument useCase = new SearchInstrument(loadInstrumentPort, loadMarketdataPort);
        TransactionTemplate readOnly = readOnlyTransaction(transactionManager);
        return query -> telemetry.observe("SearchInstrument",
                () -> readOnly.execute(status -> useCase.execute(query)));
    }

    /**
     * Portfolio use case.
     *
     * @param userPort            user reads
     * @param orderPort           order reads
     * @param instrumentPort      instrument reads
     * @param marketdataPort      price reads
     * @param portfolioCalculator portfolio calculation
     * @param portfolioCache      portfolio cache
     * @param transactionManager  transaction manager
     * @param telemetry           use case telemetry
     * @return observed, read-only transactional use case
     */
    @Bean
    public GetPortfolioUseCase getPortfolioUseCase(LoadUserPort userPort, LoadOrderPort orderPort,
            LoadInstrumentPort instrumentPort, LoadMarketdataPort marketdataPort,
            PortfolioCalculator portfolioCalculator, PortfolioCachePort portfolioCache,
            PlatformTransactionManager transactionManager, UseCaseTelemetry telemetry) {
        GetPortfolio useCase = new GetPortfolio(userPort, orderPort, instrumentPort, marketdataPort,
                portfolioCalculator, portfolioCache);
        TransactionTemplate readOnly = readOnlyTransaction(transactionManager);
        return userId -> telemetry.observe("GetPortfolio",
                () -> readOnly.execute(status -> useCase.execute(userId)));
    }

    /**
     * Order placement use case.
     *
     * @param userPort              user reads
     * @param instrumentPort        instrument reads
     * @param marketdataPort        price reads
     * @param loadOrderPort         order reads
     * @param saveOrderPort         order writes
     * @param orderPlacementService placement rules
     * @param portfolioCache        portfolio cache
     * @param clock                 clock
     * @param transactionManager    transaction manager
     * @param telemetry             use case telemetry
     * @param orderMetrics          order business metrics
     * @return observed transactional use case
     */
    @Bean
    public PlaceOrderUseCase placeOrderUseCase(LoadUserPort userPort, LoadInstrumentPort instrumentPort,
            LoadMarketdataPort marketdataPort, LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort,
            OrderPlacementService orderPlacementService, PortfolioCachePort portfolioCache, Clock clock,
            PlatformTransactionManager transactionManager, UseCaseTelemetry telemetry, OrderMetrics orderMetrics) {
        PlaceOrder useCase = new PlaceOrder(userPort, instrumentPort, marketdataPort, loadOrderPort, saveOrderPort,
                orderPlacementService, portfolioCache, clock);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        PlaceOrderUseCase transactional = command -> transaction.execute(status -> useCase.execute(command));
        return new ObservedPlaceOrderUseCase(transactional, telemetry, orderMetrics);
    }

    /**
     * Order cancellation use case.
     *
     * @param loadOrderPort      order reads
     * @param saveOrderPort      order writes
     * @param portfolioCache     portfolio cache
     * @param transactionManager transaction manager
     * @param telemetry          use case telemetry
     * @param orderMetrics       order business metrics
     * @return observed transactional use case
     */
    @Bean
    public CancelOrderUseCase cancelOrderUseCase(LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort,
            PortfolioCachePort portfolioCache, PlatformTransactionManager transactionManager,
            UseCaseTelemetry telemetry, OrderMetrics orderMetrics) {
        CancelOrder useCase = new CancelOrder(loadOrderPort, saveOrderPort, portfolioCache);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CancelOrderUseCase transactional = orderId -> transaction.execute(status -> useCase.execute(orderId));
        return new ObservedCancelOrderUseCase(transactional, telemetry, orderMetrics);
    }

    /**
     * Creates a read-only transaction template.
     *
     * @param transactionManager transaction manager
     * @return template configured as read-only
     */
    private TransactionTemplate readOnlyTransaction(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }
}
