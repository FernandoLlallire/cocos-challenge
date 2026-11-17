package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.InstrumentEntity;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    List<InstrumentEntity> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(
        String ticker,
        String name
    );
}
