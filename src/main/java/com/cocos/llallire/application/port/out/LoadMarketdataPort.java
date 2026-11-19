package com.cocos.llallire.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.cocos.llallire.domain.model.Marketdata;

/**
 * Outbound port: market price reads.
 */
public interface LoadMarketdataPort {

    /**
     * Returns the most recent marketdata of an instrument.
     *
     * @param instrumentId instrument id
     * @return the latest marketdata, or empty if there is none
     */
    Optional<Marketdata> loadLastMarketdataByInstrumentId(Long instrumentId);

    /**
     * Returns the most recent marketdata of several instruments in a single query.
     *
     * @param instrumentIds instrument ids
     * @return latest marketdata indexed by instrument id (instruments without prices are skipped)
     */
    Map<Long, Marketdata> loadLastMarketdataByInstrumentIds(Collection<Long> instrumentIds);
}
