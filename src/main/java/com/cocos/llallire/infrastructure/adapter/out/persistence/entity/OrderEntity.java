package com.cocos.llallire.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumentid")
    private InstrumentEntity instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid")
    private UserEntity user;

    @Column(name = "size")
    private Integer size;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "type", length = 10)
    private String type;

    @Column(name = "side", length = 10)
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "datetime")
    private LocalDateTime dateTime;
}
