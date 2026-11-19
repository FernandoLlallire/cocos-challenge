package com.cocos.llallire.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cocos.llallire.application.port.in.GetPortfolioUseCase;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.service.GetPortfolio;
import com.cocos.llallire.application.service.SearchInstrument;
import com.cocos.llallire.domain.service.PortfolioCalculator;

@Configuration
public class UseCasesConfig {

    @Bean
    public SearchInstrumentUseCase getSearchInstrumentUseCase(
            LoadInstrumentPort loadInstrumentPort,
            LoadMarketdataPort loadMarketdataPort) {
        return new SearchInstrument(loadInstrumentPort, loadMarketdataPort);
    }

    @Bean 
    public GetPortfolioUseCase getGetPortfolioUseCase(LoadUserPort userPort, LoadOrderPort orderPort, PortfolioCalculator portfolioCalculator,
            LoadInstrumentPort instrumentPort, LoadMarketdataPort marketdataPort)  {
        return new GetPortfolio(userPort, orderPort, portfolioCalculator, instrumentPort, marketdataPort);
    }
}
