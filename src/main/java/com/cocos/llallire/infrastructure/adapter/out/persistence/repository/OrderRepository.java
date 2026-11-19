package com.cocos.llallire.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.infrastructure.adapter.out.persistence.entity.OrderEntity;

/**
 * Spring Data repository for orders.
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Returns a user's orders in the given statuses.
     *
     * @param userId   user
     * @param statuses statuses to include
     * @return orders found
     */
    List<OrderEntity> findByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);
}
