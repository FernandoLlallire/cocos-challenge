package com.cocos.llallire.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.SaveOrderPort;
import com.cocos.llallire.domain.model.Order;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.infrastructure.adapter.out.persistence.mapper.EntityMapper;
import com.cocos.llallire.infrastructure.adapter.out.persistence.repository.OrderRepository;

/**
 * JPA adapter for {@link LoadOrderPort} and {@link SaveOrderPort}.
 */
@Component
public class OrderPersistenceAdapter implements LoadOrderPort, SaveOrderPort {
    private final OrderRepository orderRepository;

    /**
     * Creates the adapter.
     *
     * @param orderRepository order repository
     */
    public OrderPersistenceAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Order> loadOrderById(Long orderId) {
        return orderRepository.findById(orderId).map(EntityMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Order> loadOrdersByUserIdAndStatuses(Long userId, Collection<OrderStatus> statuses) {
        return orderRepository.findByUserIdAndStatusIn(userId, statuses).stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order save(Order order) {
        return EntityMapper.toDomain(orderRepository.save(EntityMapper.toEntity(order)));
    }
}
