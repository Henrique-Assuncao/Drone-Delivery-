package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderRegistrationService {

    private final OrderStorage storage;

    public OrderRegistrationService(OrderStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public OrderEntity register(String identifier, Coordinate location, double weight, Priority priority) {
        return register(identifier, location, weight, priority, Instant.now());
    }

    @Transactional
    public OrderEntity register(String identifier, Coordinate location, double weight, Priority priority, Instant confirmedDeliveryTime) {
        return register(identifier, location, weight, priority, confirmedDeliveryTime, null);
    }

    @Transactional
    public OrderEntity register(
            String identifier,
            Coordinate location,
            double weight,
            Priority priority,
            Instant confirmedDeliveryTime,
            ClientUserEntity clientUser
    ) {
        Order order = new Order(identifier, location, weight, priority);

        if (confirmedDeliveryTime == null) {
            throw new InvalidInputException("confirmedDeliveryTime must not be null");
        }

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
                OrderStatus.REQUESTED,
                java.time.Instant.now(),
                order.identifier(),
                confirmedDeliveryTime,
                clientUser
        );

        return storage.save(entity);
    }
}
