package com.cocos.llallire.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderStatus;

/**
 * Outbound port: order reads.
 */
public interface LoadOrderPort {

    /**
     * Finds an order by id.
     *
     * @param orderId order id
     * @return the order, or empty if it does not exist
     */
    Optional<Order> loadOrderById(Long orderId);

    /**
     * Returns a user's orders filtered by status.
     *
     * @param userId   user
     * @param statuses statuses to include
     * @return the user's orders in those statuses
     */
    List<Order> loadOrdersByUserIdAndStatuses(Long userId, Collection<OrderStatus> statuses);
}
