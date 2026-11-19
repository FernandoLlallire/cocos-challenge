package com.cocos.llallire.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cocos.llallire.domain.service.OrderPlacementService;
import com.cocos.llallire.domain.service.PortfolioCalculator;

/**
 * Registers the domain services as beans. The domain does not know Spring: it is instantiated here.
 */
@Configuration
public class DomainConfig {

    /**
     * Portfolio calculation service.
     *
     * @return stateless instance
     */
    @Bean
    public PortfolioCalculator portfolioCalculator() {
        return new PortfolioCalculator();
    }

    /**
     * Order placement rules service.
     *
     * @return stateless instance
     */
    @Bean
    public OrderPlacementService orderPlacementService() {
        return new OrderPlacementService();
    }

    /**
     * System clock used to timestamp orders. Injected so tests can fix it.
     *
     * @return clock in the default time zone
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
