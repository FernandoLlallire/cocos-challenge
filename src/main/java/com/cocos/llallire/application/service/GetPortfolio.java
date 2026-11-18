package com.cocos.llallire.application.service;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;

public class GetPortfolio implements GetPortfolioUseCase {
    @Override
    public List<PortfolioInformation> execute(String userId) {
        if (!users.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        // Implementation logic to retrieve portfolio information for the given userId
        // This is a placeholder implementation and should be replaced with actual logic
        return List.of(); // Return an empty list for now
    }
    
}
