package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaOrderStorage implements OrderStorage {

    private final OrderJpaRepository repository;

    public JpaOrderStorage(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return repository.existsByIdentifier(identifier);
    }

    @Override
    public List<OrderEntity> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public Optional<OrderEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<OrderEntity> findByStatus(OrderStatus status) {
        return repository.findByStatusOrderByIdAsc(status);
    }

    @Override
    public List<OrderEntity> findByClientUserId(Long clientUserId) {
        return repository.findByClientUser_IdOrderByIdAsc(clientUserId);
    }

    @Override
    public List<OrderEntity> findDeliveryQueue() {
        return repository.findByStatusInOrderByQueuedAtAscIdAsc(List.of(
                OrderStatus.REQUESTED,
                OrderStatus.PENDING_REASSIGNMENT
        ));
    }

    @Override
    public OrderEntity save(OrderEntity order) {
        return repository.save(order);
    }
}
