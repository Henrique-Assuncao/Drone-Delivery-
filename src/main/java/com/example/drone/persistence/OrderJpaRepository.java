package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    boolean existsByIdentifier(String identifier);

    List<OrderEntity> findByStatusOrderByIdAsc(OrderStatus status);

    List<OrderEntity> findByClientUser_IdOrderByIdAsc(Long clientUserId);

    List<OrderEntity> findByStatusInOrderByQueuedAtAscIdAsc(Collection<OrderStatus> statuses);
}
