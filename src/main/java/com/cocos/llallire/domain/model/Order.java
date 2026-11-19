package com.cocos.llallire.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

import com.cocos.llallire.domain.exception.InvalidOrderDataException;
import com.cocos.llallire.domain.exception.OrderNotCancellableException;

/**
 * Market order or cash transfer. Immutable: status changes return a new instance.
 *
 * <p>For transfers ({@code CASH_IN}/{@code CASH_OUT}) the instrument is ARS, {@code price} is 1
 * and {@code size} is the amount in pesos.</p>
 *
 * @param id           identifier; {@code null} until the order is persisted
 * @param instrumentId traded instrument
 * @param userId       user sending the order
 * @param size         number of shares (or pesos, for transfers)
 * @param price        unit price in pesos
 * @param type         order type
 * @param side         order side
 * @param status       order status
 * @param dateTime     submission time
 */
public record Order(
        Long id,
        Long instrumentId,
        Long userId,
        Long size,
        BigDecimal price,
        OrderType type,
        OrderSide side,
        OrderStatus status,
        LocalDateTime dateTime) {

    /** Chronological order; ties on date are broken by id (orders without id go last). */
    public static final Comparator<Order> CHRONOLOGICAL = Comparator
            .comparing(Order::dateTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Order::id, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Calculates the total amount of the order ({@code size * price}).
     *
     * @return amount in pesos
     * @throws InvalidOrderDataException if the order has no valid size or price
     */
    public BigDecimal notional() {
        long validSize = validSize();
        if (price == null || price.signum() <= 0) {
            throw new InvalidOrderDataException(id, "price must be positive");
        }
        return price.multiply(BigDecimal.valueOf(validSize));
    }

    /**
     * Returns the size, checking that it is positive.
     *
     * @return order size
     * @throws InvalidOrderDataException if the size is null or not positive
     */
    public long validSize() {
        if (size == null || size <= 0) {
            throw new InvalidOrderDataException(id, "size must be positive");
        }
        return size;
    }

    /**
     * Tells whether the order was executed.
     *
     * @return {@code true} if the status is {@link OrderStatus#FILLED}
     */
    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }

    /**
     * Tells whether the order is pending in the market.
     *
     * @return {@code true} if the status is {@link OrderStatus#NEW}
     */
    public boolean isPending() {
        return status == OrderStatus.NEW;
    }

    /**
     * Cancels the order. Only orders in {@link OrderStatus#NEW} status can be cancelled.
     *
     * @return a copy of the order with status {@link OrderStatus#CANCELLED}
     * @throws OrderNotCancellableException if the order is not in NEW status
     */
    public Order cancel() {
        if (!isPending()) {
            throw new OrderNotCancellableException(id, status);
        }
        return new Order(id, instrumentId, userId, size, price, type, side, OrderStatus.CANCELLED, dateTime);
    }
}
