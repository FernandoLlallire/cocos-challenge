package com.cocos.llallire.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST adapter for instrument search.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/instruments")
@Tag(name = "Instruments", description = "Search of tradable assets")
public class InstrumentController {
    private final SearchInstrumentUseCase searchInstrumentUseCase;

    /**
     * Creates the controller.
     *
     * @param searchInstrumentUseCase search use case
     */
    public InstrumentController(SearchInstrumentUseCase searchInstrumentUseCase) {
        this.searchInstrumentUseCase = searchInstrumentUseCase;
    }

    /**
     * {@code GET /api/v1/instruments?query=...}: searches instruments by ticker or name.
     *
     * @param query text to search; optional
     * @return 200 with the instruments found
     */
    @GetMapping
    @Operation(
            summary = "Search instruments by ticker or name",
            description = """
                    Partial, case-insensitive match on the ticker **or** the name. Without `query` (or with an
                    empty one) returns all instruments. Results are sorted by ticker.

                    `dailyReturnPct = (close - previousClose) / previousClose * 100`. Instruments without
                    marketdata (for example ARS) return `lastPrice` and `dailyReturnPct` as `null`.""")
    @ApiResponse(responseCode = "200", description = "Matching instruments (possibly empty)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InstrumentInformation.class)),
                    examples = {
                            @ExampleObject(name = "query=pamp", value = OpenApiExamples.INSTRUMENTS_PAMP),
                            @ExampleObject(name = "query=pesos (no marketdata)",
                                    value = OpenApiExamples.INSTRUMENTS_ARS),
                            @ExampleObject(name = "No matches", value = OpenApiExamples.INSTRUMENTS_EMPTY)}))
    public ResponseEntity<List<InstrumentInformation>> searchInstruments(
            @Parameter(description = "Text to search in the ticker or the name; omit it to list all instruments",
                    example = "pamp")
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(searchInstrumentUseCase.execute(query));
    }
}
