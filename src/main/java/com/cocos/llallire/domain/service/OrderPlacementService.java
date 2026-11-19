package com.cocos.llallire.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.cocos.llallire.domain.exception.InvalidOrderRequestException;
import com.cocos.llallire.domain.exception.MarketDataNotFoundException;
import com.cocos.llallire.domain.model.CashPosition;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.InstrumentPosition;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderRequest;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.OrderType;
import com.cocos.llallire.domain.shared.Precision;

/**
 * Domain service that decides the price, quantity and status of a new order.
 *
 * <ol>
 *   <li><b>Price</b>: LIMIT uses the price sent; MARKET uses the latest {@code close}; transfers
 *       use 1 (the size is the amount in pesos).</li>
 *   <li><b>Quantity</b>: the {@code size} sent or, if {@code amount} was sent, the maximum whole number
 *       of shares that amount can buy: {@code floor(amount / price)}.</li>
 *   <li><b>Status</b>: REJECTED if the resulting quantity is zero or if there are not enough available
 *       pesos/shares; otherwise NEW for LIMIT and FILLED for MARKET.</li>
 * </ol>
 *
 * <p>It is stateless and has no infrastructure dependencies.</p>
 */
public class OrderPlacementService {

    /**
     * Builds the order to persist from the request.
     *
     * @param request     validated request
     * @param instrument  instrument to trade
     * @param marketPrice latest price of the instrument; required only for MARKET BUY/SELL
     * @param userOrders  current user orders (FILLED and NEW), used to compute what is available
     * @param now         submission time
     * @return the order with resolved price, quantity and status
     * @throws InvalidOrderRequestException if the side does not match the instrument type
     * @throws MarketDataNotFoundException  if it is a MARKET stock order and there is no market price
     */
    public Order place(OrderRequest request, Instrument instrument, BigDecimal marketPrice, List<Order> userOrders,
            LocalDateTime now) {
        validateInstrument(request, instrument);
        BigDecimal price = resolvePrice(request, instrument, marketPrice)
                .setScale(Precision.MONEY_SCALE, Precision.ROUNDING);
        long size = resolveSize(request, price);
        OrderStatus status = resolveStatus(request, instrument, size, price, userOrders);
        return new Order(null, instrument.id(), request.userId(), size, price, request.type(), request.side(),
                status, now);
    }

    /**
     * Checks that the order side matches the instrument type: transfers operate on the currency and
     * buys/sells on stocks.
     *
     * @param request    request
     * @param instrument instrument to trade
     * @throws InvalidOrderRequestException if they do not match
     */
    private void validateInstrument(OrderRequest request, Instrument instrument) {
        if (request.side().isCashTransfer() && !instrument.isCash()) {
            throw new InvalidOrderRequestException(
                    "%s orders must use a currency instrument".formatted(request.side()));
        }
        if (!request.side().isCashTransfer() && instrument.isCash()) {
            throw new InvalidOrderRequestException(
                    "%s orders cannot use a currency instrument; use CASH_IN or CASH_OUT".formatted(request.side()));
        }
    }

    /**
     * Determines the unit price of the order.
     *
     * @param request     request
     * @param instrument  instrument to trade
     * @param marketPrice latest price of the instrument
     * @return price to use
     * @throws MarketDataNotFoundException if a market price is required and there is none
     */
    private BigDecimal resolvePrice(OrderRequest request, Instrument instrument, BigDecimal marketPrice) {
        if (request.side().isCashTransfer()) {
            return BigDecimal.ONE;
        }
        if (request.type() == OrderType.LIMIT) {
            return request.price();
        }
        if (marketPrice == null || marketPrice.signum() <= 0) {
            throw new MarketDataNotFoundException(instrument.id());
        }
        return marketPrice;
    }

    /**
     * Determines the quantity: the one sent or the maximum whole number the amount can cover.
     *
     * @param request request
     * @param price   unit price
     * @return number of shares (may be zero if the amount does not cover one share)
     */
    private long resolveSize(OrderRequest request, BigDecimal price) {
        if (request.size() != null) {
            return request.size();
        }
        return request.amount().divide(price, 0, RoundingMode.FLOOR).longValueExact();
    }

    /**
     * Determines the order status by validating the available pesos or shares.
     *
     * @param request    request
     * @param instrument instrument to trade
     * @param size       resolved quantity
     * @param price      resolved price
     * @param userOrders current user orders
     * @return REJECTED if the order cannot be covered; NEW for LIMIT; FILLED for MARKET
     */
    private OrderStatus resolveStatus(OrderRequest request, Instrument instrument, long size, BigDecimal price,
            List<Order> userOrders) {
        if (size <= 0 || !hasEnoughFunds(request.side(), instrument, size, price, userOrders)) {
            return OrderStatus.REJECTED;
        }
        return request.type() == OrderType.LIMIT ? OrderStatus.NEW : OrderStatus.FILLED;
    }

    /**
     * Checks that the user has available pesos (buy, withdrawal) or shares (sell).
     *
     * @param side       order side
     * @param instrument instrument to trade
     * @param size       quantity
     * @param price      unit price
     * @param userOrders current user orders
     * @return {@code true} if the order can be covered
     */
    private boolean hasEnoughFunds(OrderSide side, Instrument instrument, long size, BigDecimal price,
            List<Order> userOrders) {
        return switch (side) {
            case CASH_IN -> true;
            case BUY, CASH_OUT -> {
                CashPosition cash = new CashPosition();
                cash.processOrders(userOrders);
                yield price.multiply(BigDecimal.valueOf(size)).compareTo(cash.available()) <= 0;
            }
            case SELL -> {
                InstrumentPosition position = new InstrumentPosition(instrument);
                position.processOrders(userOrders);
                yield size <= position.availableQuantity();
            }
        };
    }
}
