package com.cocos.llallire.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cocos.llallire.application.dto.InstrumentInformation;
import com.cocos.llallire.application.port.in.SearchInstrumentUseCase;

@RestController
@RequestMapping(path = ApiPaths.V1 + "/instruments")
public class InstrumentController {
    private final SearchInstrumentUseCase searchInstrumentUseCase;

    public InstrumentController(SearchInstrumentUseCase searchInstrumentUseCase) {
        this.searchInstrumentUseCase = searchInstrumentUseCase;
    }

    @GetMapping
    public ResponseEntity<List<InstrumentInformation>> getInstruments(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(searchInstrumentUseCase.execute(query));
    }
}
