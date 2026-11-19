package com.cocos.llallire.application.port.out;

import com.cocos.llallire.domain.model.Order;

/**
 * Outbound port: order writes.
 */
public interface SaveOrderPort {

    /**
     * Inserts the order if it has no id, or updates it if it already exists.
     *
     * @param order order to save
     * @return the saved order, with its assigned id
     */
    Order save(Order order);
}
