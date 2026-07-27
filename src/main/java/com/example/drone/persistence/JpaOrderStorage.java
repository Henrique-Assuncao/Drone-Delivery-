package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
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
        return repository.findByStatusIn(List.of(
                OrderStatus.REQUESTED,
                OrderStatus.PENDING_REASSIGNMENT
        )).stream()
                .sorted(deliveryQueueComparator())
                .toList();
    }

    @Override
    public OrderEntity save(OrderEntity order) {
        return repository.save(order);
    }

    private Comparator<OrderEntity> deliveryQueueComparator() {
        return Comparator.comparing(OrderEntity::getConfirmedDeliveryTime)
                .thenComparing(Comparator.comparingInt((OrderEntity order) -> priorityRank(order.getPriority())).reversed())
                .thenComparing(OrderEntity::getQueuedAt)
                .thenComparing(OrderEntity::getId);
    }

    private int priorityRank(Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
