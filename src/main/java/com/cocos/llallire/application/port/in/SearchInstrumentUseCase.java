package com.cocos.llallire.application.port.in;

import java.util.List;

import com.cocos.llallire.application.dto.InstrumentInformation;

/**
 * Inbound port: search instruments by ticker or name.
 */
public interface SearchInstrumentUseCase {

    /**
     * Searches instruments whose ticker or name contains the text, case-insensitively.
     *
     * @param query text to search; if null or empty all instruments are returned
     * @return instruments found, with their latest price and daily return
     */
    List<InstrumentInformation> execute(String query);
}
