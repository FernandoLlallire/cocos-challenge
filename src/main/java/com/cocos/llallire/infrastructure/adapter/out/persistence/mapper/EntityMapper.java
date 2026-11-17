package com.cocos.llallire.infrastructure.adapter.out.persistence.mapper;

import java.util.Objects;

import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.InstrumentEntity;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.MarketdataEntity;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.OrderEntity;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.UserEntity;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity, "User entity cannot be null");
        return new User(entity.getId(), entity.getEmail(), entity.getAccountNumber());
    }

    public static Instrument toDomain(InstrumentEntity entity) {
        Objects.requireNonNull(entity, "Instrument entity cannot be null");
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType());
    }

    public static Order toDomain(OrderEntity entity) {
        Objects.requireNonNull(entity, "Order entity cannot be null");
        return new Order(
            entity.getId(),
            entity.getInstrument() == null ? null : entity.getInstrument().getId(),
            entity.getUser() == null ? null : entity.getUser().getId(),
            entity.getSize(),
            entity.getPrice(),
            entity.getType(),
            entity.getSide(),
            entity.getStatus(),
            entity.getDateTime()
        );
    }

    public static Marketdata toDomain(MarketdataEntity entity) {
        Objects.requireNonNull(entity, "Marketdata entity cannot be null");
        return new Marketdata(
            entity.getId(),
            entity.getInstrument() == null ? null : entity.getInstrument().getId(),
            entity.getHigh(),
            entity.getLow(),
            entity.getOpen(),
            entity.getClose(),
            entity.getPreviousClose(),
            entity.getDate()
        );
    }
}
