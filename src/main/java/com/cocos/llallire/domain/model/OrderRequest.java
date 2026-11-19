package com.cocos.llallire.domain.model;

import java.math.BigDecimal;

import com.cocos.llallire.domain.exception.InvalidOrderRequestException;
import com.cocos.llallire.domain.shared.Precision;

/**
 * Order request with a validated structure. It is built only if it meets the format rules:
 *
 * <ul>
 *   <li>user, instrument, side and type are mandatory;</li>
 *   <li>exactly one of {@code size} (exact quantity) or {@code amount} (amount in pesos) is sent,
 *       and it must be positive;</li>
 *   <li>LIMIT orders require a positive {@code price} with up to 2 decimals and MARKET orders do not
 *       accept it;</li>
 *   <li>transfers ({@code CASH_IN}/{@code CASH_OUT}) are always MARKET.</li>
 * </ul>
 *
 * @param userId       user sending the order
 * @param instrumentId instrument to trade
 * @param side         order side
 * @param type         order type
 * @param size         exact number of shares; mutually exclusive with {@code amount}
 * @param amount       total amount in pesos to invest; mutually exclusive with {@code size}
 * @param price        limit price (LIMIT only)
 */
public record OrderRequest(
        Long userId,
        Long instrumentId,
        OrderSide side,
        OrderType type,
        Long size,
        BigDecimal amount,
        BigDecimal price) {

    /**
     * Validates the structure of the request.
     *
     * @throws InvalidOrderRequestException if any format rule is not met
     */
    public OrderRequest {
        require(userId != null, "userId is required");
        require(instrumentId != null, "instrumentId is required");
        require(side != null, "side is required");
        require(type != null, "type is required");
        require((size == null) != (amount == null), "exactly one of size or amount must be sent");
        require(size == null || size > 0, "size must be positive");
        require(amount == null || amount.signum() > 0, "amount must be positive");
        if (type == OrderType.LIMIT) {
            require(price != null && price.signum() > 0, "LIMIT orders require a positive price");
            require(price.stripTrailingZeros().scale() <= Precision.MONEY_SCALE,
                    "price must have at most %d decimals".formatted(Precision.MONEY_SCALE));
        } else {
            require(price == null, "MARKET orders must not include a price; the last market price is used");
        }
        require(!side.isCashTransfer() || type == OrderType.MARKET, "cash transfers must be MARKET orders");
    }

    /**
     * Throws {@link InvalidOrderRequestException} if the condition is not met.
     *
     * @param condition expected condition
     * @param message   error message
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidOrderRequestException(message);
        }
    }
}
