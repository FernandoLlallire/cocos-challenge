package com.cocos.llallire.application.port.in;

import com.cocos.llallire.application.dto.PortfolioInformation;

public interface GetPortfolioUseCase {
    PortfolioInformation execute(Long userId);
}
