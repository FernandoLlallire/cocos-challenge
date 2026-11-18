package com.cocos.llallire.application.service;

import java.util.List;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.exception.UserNotFoundException;

public class GetPortfolio implements GetPortfolioUseCase {
    private final LoadUserPort userPort;

    public GetPortfolio(LoadUserPort userPort) {
        this.userPort = userPort;
    }
    @Override
    public PortfolioInformation execute(Long userId) {
        if (userPort.getUserById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        // Implementation logic to retrieve portfolio information for the given userId
        // This is a placeholder implementation and should be replaced with actual logic
        return List.of(); // Return an empty list for now
    }
    
}
