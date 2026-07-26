package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;
import java.util.Optional;

public interface OrderStorage {

    boolean existsByIdentifier(String identifier);

    List<OrderEntity> findAll();

    Optional<OrderEntity> findById(Long id);

    List<OrderEntity> findByStatus(OrderStatus status);

    List<OrderEntity> findByClientUserId(Long clientUserId);

    List<OrderEntity> findDeliveryQueue();

    OrderEntity save(OrderEntity order);
}
