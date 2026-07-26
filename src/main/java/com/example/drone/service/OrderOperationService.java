package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderOperationService {

    private final OrderStorage storage;

    public OrderOperationService(OrderStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public OrderEntity cancelUnallocated(Long id, String reason) {
        OrderEntity order = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));

        if (order.getStatus() != OrderStatus.UNALLOCATED) {
            throw new InvalidInputException("order must be UNALLOCATED to cancel");
        }

        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("cancel reason must not be blank");
        }

        order.changeStatus(OrderStatus.CANCELLED, reason);

        return order;
    }

    @Transactional
    public OrderEntity requeueUnallocated(Long id) {
        OrderEntity order = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));

        if (order.getStatus() != OrderStatus.UNALLOCATED) {
            throw new InvalidInputException("order must be UNALLOCATED to requeue");
        }

        order.changeStatus(OrderStatus.REQUESTED);

        return order;
    }
}
