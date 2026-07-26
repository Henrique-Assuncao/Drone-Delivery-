package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderQueryService {

    private final OrderStorage storage;

    public OrderQueryService(OrderStorage storage) {
        this.storage = storage;
    }

    public List<OrderEntity> findAll() {
        return storage.findAll();
    }

    public OrderEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));
    }

    public List<OrderEntity> findByStatus(OrderStatus status) {
        return storage.findByStatus(status);
    }

    public List<OrderEntity> findDeliveryQueue() {
        return storage.findDeliveryQueue();
    }
}
