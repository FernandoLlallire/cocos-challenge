package com.cocos.llallire.application.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cocos.llallire.domain.model.Portfolio;

/**
 * Response of the portfolio use case.
 *
 * @param totalAccountValue total account value in pesos
 * @param availableCash     pesos available to trade
 * @param positions         assets held by the user
 */
public record PortfolioInformation(
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        List<InstrumentPositionInformation> positions) {

    /**
     * Converts the domain portfolio into the output DTO.
     *
     * @param portfolio calculated portfolio
     * @return equivalent DTO
     */
    public static PortfolioInformation from(Portfolio portfolio) {
        return new PortfolioInformation(
                portfolio.totalAccountValue(),
                portfolio.availableCash(),
                portfolio.positions().stream().map(InstrumentPositionInformation::from).toList());
    }
}
