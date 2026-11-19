package com.cocos.llallire.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;

@RestController 
@RequestMapping(path = ApiPaths.V1 + "/portfolio")
public class PortfolioController {
    private final GetPortfolioUseCase getPortfolioUseCase;

    PortfolioController(GetPortfolioUseCase getPortfolioUseCase) {
        this.getPortfolioUseCase = getPortfolioUseCase;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioInformation> getPortfolio(@PathVariable Long userId) {
        return ResponseEntity.ok(getPortfolioUseCase.execute(userId));
    } 
}
