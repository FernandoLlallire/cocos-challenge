package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.MarketdataEntity;

public interface MarketdataRepository extends JpaRepository<MarketdataEntity, Long> {

    Optional<MarketdataEntity> findFirstByInstrument_IdOrderByDateDescIdDesc(Long instrumentId);
}
