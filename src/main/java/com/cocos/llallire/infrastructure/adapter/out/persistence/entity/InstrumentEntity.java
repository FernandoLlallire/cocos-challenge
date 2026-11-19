package com.cocos.llallire.infrastructure.adapter.out.persistence.entity;

import com.cocos.llallire.domain.model.InstrumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of the {@code instruments} table.
 */
@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
public class InstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", length = 10)
    private String ticker;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "type", length = 10)
    @Enumerated(EnumType.STRING)
    private InstrumentType type;
}
