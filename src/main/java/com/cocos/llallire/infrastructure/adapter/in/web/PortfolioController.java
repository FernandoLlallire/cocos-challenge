package com.cocos.llallire.infrastructure.adapter.in.web;

import static com.cocos.llallire.infrastructure.adapter.in.web.OpenApiExamples.PROBLEM_JSON;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.in.GetPortfolioUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST adapter for the portfolio.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/portfolio")
@Tag(name = "Portfolio", description = "Cash and positions of a user, derived from their orders")
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
    @Operation(
            summary = "Get a user's portfolio",
            description = """
                    Returns the total account value, the cash available to trade and the positions held.

                    - Cash balance = FILLED `CASH_IN`/`SELL` minus FILLED `CASH_OUT`/`BUY` (`size * price`).
                    - `availableCash` = balance minus the cash reserved by pending (`NEW`) LIMIT buys.
                    - `totalAccountValue` = balance (including reserved cash) + market value of the positions.
                    - Positions use FILLED BUY/SELL orders with weighted average cost; positions with zero shares
                      are omitted. Sorted by ticker.""")
    @ApiResponse(responseCode = "200", description = "Portfolio of the user",
            content = @Content(schema = @Schema(implementation = PortfolioInformation.class), examples = {
                    @ExampleObject(name = "User 1 (seed)", value = OpenApiExamples.PORTFOLIO_USER_1),
                    @ExampleObject(name = "User without orders", value = OpenApiExamples.PORTFOLIO_EMPTY)}))
    @ApiResponse(responseCode = "400", description = "`userId` is not a number",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Invalid id", value = OpenApiExamples.PROBLEM_INVALID_USER_ID)))
    @ApiResponse(responseCode = "404", description = "The user does not exist",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown user", value = OpenApiExamples.PROBLEM_USER_NOT_FOUND)))
    public ResponseEntity<PortfolioInformation> getPortfolio(
            @Parameter(description = "User identifier", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(getPortfolioUseCase.execute(userId));
    }
}
