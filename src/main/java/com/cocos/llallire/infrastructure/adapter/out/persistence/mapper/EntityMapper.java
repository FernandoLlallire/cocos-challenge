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

/**
 * Conversions between JPA entities and domain models. Keeps the domain free of persistence
 * annotations.
 */
public final class EntityMapper {

    /**
     * Utility class; not instantiable.
     */
    private EntityMapper() {
    }

    /**
     * Converts a JPA user to domain.
     *
     * @param entity entity
     * @return domain user
     */
    public static User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity, "User entity cannot be null");
        return new User(entity.getId(), entity.getEmail(), entity.getAccountNumber());
    }

    /**
     * Converts a JPA instrument to domain.
     *
     * @param entity entity
     * @return domain instrument
     */
    public static Instrument toDomain(InstrumentEntity entity) {
        Objects.requireNonNull(entity, "Instrument entity cannot be null");
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType());
    }

    /**
     * Converts a JPA marketdata to domain.
     *
     * @param entity entity
     * @return domain marketdata
     */
    public static Marketdata toDomain(MarketdataEntity entity) {
        Objects.requireNonNull(entity, "Marketdata entity cannot be null");
        return new Marketdata(entity.getId(), entity.getInstrumentId(), entity.getHigh(), entity.getLow(),
                entity.getOpen(), entity.getClose(), entity.getPreviousClose(), entity.getDate());
    }

    /**
     * Converts a JPA order to domain. The {@code size} column is INT in the database and {@code Long} in the
     * domain.
     *
     * @param entity entity
     * @return domain order
     */
    public static Order toDomain(OrderEntity entity) {
        Objects.requireNonNull(entity, "Order entity cannot be null");
        return new Order(
                entity.getId(),
                entity.getInstrumentId(),
                entity.getUserId(),
                entity.getSize() == null ? null : entity.getSize().longValue(),
                entity.getPrice(),
                entity.getType(),
                entity.getSide(),
                entity.getStatus(),
                entity.getDateTime());
    }

    /**
     * Converts a domain order to a JPA entity.
     *
     * @param order domain order
     * @return entity ready to persist
     * @throws ArithmeticException if the size does not fit in the INT column
     */
    public static OrderEntity toEntity(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id());
        entity.setInstrumentId(order.instrumentId());
        entity.setUserId(order.userId());
        entity.setSize(order.size() == null ? null : Math.toIntExact(order.size()));
        entity.setPrice(order.price());
        entity.setType(order.type());
        entity.setSide(order.side());
        entity.setStatus(order.status());
        entity.setDateTime(order.dateTime());
        return entity;
    }
}
