package com.cocos.llallire.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cocos.llallire.domain.model.Instrument;

/**
 * Outbound port: instrument reads.
 */
public interface LoadInstrumentPort {

    /**
     * Searches instruments whose ticker or name contains the text, case-insensitively.
     *
     * @param query text to search; if null or empty returns all
     * @return instruments sorted by ticker
     */
    List<Instrument> searchByTickerOrName(String query);

    /**
     * Finds an instrument by id.
     *
     * @param instrumentId instrument id
     * @return the instrument, or empty if it does not exist
     */
    Optional<Instrument> loadInstrumentById(Long instrumentId);

    /**
     * Finds several instruments in a single query.
     *
     * @param instrumentIds ids to find
     * @return the instruments found (missing ids are skipped)
     */
    List<Instrument> loadInstrumentsByIds(Collection<Long> instrumentIds);
}
