package com.cocos.llallire.infrastructure.adapter.out.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String type;

    @OneToMany(mappedBy = "instrument")
    private List<OrderEntity> orders = new ArrayList<>();

    @OneToMany(mappedBy = "instrument")
    private List<MarketdataEntity> marketdata = new ArrayList<>();
}
