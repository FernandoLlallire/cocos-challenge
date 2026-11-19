package com.cocos.llallire.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;

/**
 * REST adapter for instrument search.
 */
@RestController
@RequestMapping(path = ApiPaths.V1 + "/instruments")
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
    public ResponseEntity<List<InstrumentInformation>> searchInstruments(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(searchInstrumentUseCase.execute(query));
    }
}
