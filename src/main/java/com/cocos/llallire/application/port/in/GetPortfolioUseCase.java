package com.cocos.llallire.application.port.in;

import com.cocos.llallire.application.dto.PortfolioInformation;

/**
 * Inbound port: query a user's portfolio.
 */
public interface GetPortfolioUseCase {

    /**
     * Calculates the user's portfolio.
     *
     * @param userId user
     * @return total account value, available pesos and positions
     * @throws com.cocos.llallire.domain.exception.UserNotFoundException if the user does not exist
     */
    PortfolioInformation execute(Long userId);
}
