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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "confirmed_delivery_time", nullable = false)
    private Instant confirmedDeliveryTime;

    @Column(name = "delivery_confirmation_code", nullable = false)
    private String deliveryConfirmationCode;

    @Column(name = "status_reason")
    private String statusReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id")
    private ClientUserEntity clientUser;

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
        this(id, identifier, locationX, locationY, weight, priority, status, queuedAt, identifier, queuedAt);
    }

    public OrderEntity(
            Long id,
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            OrderStatus status,
            Instant queuedAt,
            String deliveryConfirmationCode
    ) {
        this(id, identifier, locationX, locationY, weight, priority, status, queuedAt, deliveryConfirmationCode, queuedAt);
    }

    public OrderEntity(
            Long id,
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            OrderStatus status,
            Instant queuedAt,
            String deliveryConfirmationCode,
            Instant confirmedDeliveryTime
    ) {
        this(id, identifier, locationX, locationY, weight, priority, status, queuedAt, deliveryConfirmationCode, confirmedDeliveryTime, null);
    }

    public OrderEntity(
            Long id,
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            OrderStatus status,
            Instant queuedAt,
            String deliveryConfirmationCode,
            Instant confirmedDeliveryTime,
            ClientUserEntity clientUser
    ) {
        if (queuedAt == null) {
            throw new InvalidInputException("queuedAt must not be null");
        }

        if (confirmedDeliveryTime == null) {
            throw new InvalidInputException("confirmedDeliveryTime must not be null");
        }

        if (deliveryConfirmationCode == null || deliveryConfirmationCode.isBlank()) {
            throw new InvalidInputException("deliveryConfirmationCode must not be blank");
        }

        this.id = id;
        this.identifier = identifier;
        this.locationX = locationX;
        this.locationY = locationY;
        this.weight = weight;
        this.priority = priority;
        this.status = status;
        this.queuedAt = queuedAt;
        this.confirmedDeliveryTime = confirmedDeliveryTime;
        this.deliveryConfirmationCode = deliveryConfirmationCode.trim();
        this.statusReason = null;
        this.clientUser = clientUser;
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

    public Instant getConfirmedDeliveryTime() {
        return confirmedDeliveryTime;
    }

    public String getDeliveryConfirmationCode() {
        return deliveryConfirmationCode;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public ClientUserEntity getClientUser() {
        return clientUser;
    }

    public void changeStatus(OrderStatus status) {
        changeStatus(status, null);
    }

    public void changeStatus(OrderStatus status, String statusReason) {
        if (status == null) {
            throw new InvalidInputException("status must not be null");
        }

        this.status = status;
        this.statusReason = normalizeStatusReason(statusReason);
    }

    private String normalizeStatusReason(String statusReason) {
        if (statusReason == null || statusReason.isBlank()) {
            return null;
        }

        return statusReason.trim();
    }
}
