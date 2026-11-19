package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.MarketdataEntity;

/**
 * Spring Data repository for marketdata.
 */
public interface MarketdataRepository extends JpaRepository<MarketdataEntity, Long> {

    /**
     * Returns the most recent marketdata of an instrument (latest date; on ties, highest id).
     *
     * @param instrumentId instrument id
     * @return the latest marketdata, or empty if there is none
     */
    Optional<MarketdataEntity> findFirstByInstrumentIdOrderByDateDescIdDesc(Long instrumentId);

    /**
     * Returns the marketdata of the most recent date of each instrument. It may return more than one row
     * per instrument if there are duplicates on that date; the adapter keeps the one with the highest id.
     *
     * @param instrumentIds instrument ids
     * @return marketdata of the latest date of each instrument
     */
    @Query("""
            select m from MarketdataEntity m
            where m.instrumentId in :instrumentIds
              and m.date = (select max(m2.date) from MarketdataEntity m2 where m2.instrumentId = m.instrumentId)
            """)
    List<MarketdataEntity> findLatestByInstrumentIds(@Param("instrumentIds") Collection<Long> instrumentIds);
}
