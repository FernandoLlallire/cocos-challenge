package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.InstrumentEntity;

/**
 * Spring Data repository for instruments.
 */
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    /**
     * Searches instruments whose ticker or name contains the text, case-insensitively.
     *
     * @param ticker text to search in the ticker
     * @param name   text to search in the name
     * @return instruments sorted by ticker
     */
    List<InstrumentEntity> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByTickerAsc(
            String ticker, String name);
}
