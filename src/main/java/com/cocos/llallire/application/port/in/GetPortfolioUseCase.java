package com.cocos.llallire.application.port.in;

public interface GetPortfolioUseCase {
    List<PortfolioInformation> execute(String userId);
}
