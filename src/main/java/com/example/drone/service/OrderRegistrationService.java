package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRegistrationService {

    private final OrderStorage storage;

    public OrderRegistrationService(OrderStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public OrderEntity register(String identifier, Coordinate location, double weight, Priority priority) {
        Order order = new Order(identifier, location, weight, priority);

        if (storage.existsByIdentifier(order.identifier())) {
            throw new DuplicateResourceException("order identifier already exists");
        }

        OrderEntity entity = new OrderEntity(
                null,
                order.identifier(),
                order.location().x(),
                order.location().y(),
                order.weight(),
                order.priority(),
                OrderStatus.REQUESTED
        );

        return storage.save(entity);
    }
}
