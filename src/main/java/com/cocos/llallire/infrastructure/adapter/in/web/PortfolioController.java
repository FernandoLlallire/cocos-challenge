package com.cocos.llallire.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;

/**
 * REST adapter for the portfolio.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/portfolio")
public class PortfolioController {
    private final GetPortfolioUseCase getPortfolioUseCase;

    /**
     * Creates the controller.
     *
     * @param getPortfolioUseCase portfolio use case
     */
    public PortfolioController(GetPortfolioUseCase getPortfolioUseCase) {
        this.getPortfolioUseCase = getPortfolioUseCase;
    }

    /**
     * {@code GET /api/v1/portfolio/{userId}}: returns the user's portfolio.
     *
     * @param userId user
     * @return 200 with the portfolio, or 404 if the user does not exist
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioInformation> getPortfolio(@PathVariable Long userId) {
        return ResponseEntity.ok(getPortfolioUseCase.execute(userId));
    }
}
