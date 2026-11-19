package com.cocos.llallire.application.port.out;

import java.util.List;

import com.cocos.llallire.domain.model.Order;

public interface LoadOrderPort {
    List<Order> getFilledOrdersByUserId(Long userId);
}
