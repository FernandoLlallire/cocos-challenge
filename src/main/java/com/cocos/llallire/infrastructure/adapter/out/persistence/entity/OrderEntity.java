package com.cocos.llallire.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;

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
 * JPA mapping of the {@code orders} table. FKs are mapped as ids: the domain only needs the
 * identifiers, which avoids lazy loading and extra queries.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid")
    private Long instrumentId;

    @Column(name = "userid")
    private Long userId;

    @Column(name = "size")
    private Integer size;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "type", length = 10)
    @Enumerated(EnumType.STRING)
    private OrderType type;

    @Column(name = "side", length = 10)
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "datetime")
    private LocalDateTime dateTime;
}
