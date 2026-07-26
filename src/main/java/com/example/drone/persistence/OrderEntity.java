package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(name = "location_x", nullable = false)
    private double locationX;

    @Column(name = "location_y", nullable = false)
    private double locationY;

    @Column(nullable = false)
    private double weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    protected OrderEntity() {
    }

    public OrderEntity(
            Long id,
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            OrderStatus status
    ) {
        this(id, identifier, locationX, locationY, weight, priority, status, Instant.now());
    }

    public OrderEntity(
            Long id,
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            OrderStatus status,
            Instant queuedAt
    ) {
        if (queuedAt == null) {
            throw new InvalidInputException("queuedAt must not be null");
        }

        this.id = id;
        this.identifier = identifier;
        this.locationX = locationX;
        this.locationY = locationY;
        this.weight = weight;
        this.priority = priority;
        this.status = status;
        this.queuedAt = queuedAt;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getLocationX() {
        return locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public double getWeight() {
        return weight;
    }

    public Priority getPriority() {
        return priority;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void changeStatus(OrderStatus status) {
        if (status == null) {
            throw new InvalidInputException("status must not be null");
        }

        this.status = status;
    }
}
