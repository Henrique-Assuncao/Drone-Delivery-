package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "trip_orders")
public class TripOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "route_position", nullable = false)
    private int routePosition;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "estimated_delivery_time", nullable = false)
    private double estimatedDeliveryTime;

    @Column(name = "availability_notified_at")
    private Instant availabilityNotifiedAt;

    @Column(name = "availability_confirmed_at")
    private Instant availabilityConfirmedAt;

    @Column(name = "delivery_confirmation_requested_at")
    private Instant deliveryConfirmationRequestedAt;

    @Column(name = "delivery_failed_at")
    private Instant deliveryFailedAt;

    @Column(name = "delivery_failure_reason")
    private String deliveryFailureReason;

    protected TripOrderEntity() {
    }

    public TripOrderEntity(TripEntity trip, OrderEntity order, int routePosition) {
        this(trip, order, routePosition, null, 0.0);
    }

    public TripOrderEntity(TripEntity trip, OrderEntity order, int routePosition, Instant deliveredAt) {
        this(trip, order, routePosition, deliveredAt, 0.0);
    }

    public TripOrderEntity(
            TripEntity trip,
            OrderEntity order,
            int routePosition,
            Instant deliveredAt,
            double estimatedDeliveryTime
    ) {
        if (trip == null) {
            throw new InvalidInputException("trip must not be null");
        }

        if (order == null) {
            throw new InvalidInputException("order must not be null");
        }

        if (routePosition < 0) {
            throw new InvalidInputException("routePosition must not be negative");
        }

        if (estimatedDeliveryTime < 0) {
            throw new InvalidInputException("estimatedDeliveryTime must not be negative");
        }

        this.trip = trip;
        this.order = order;
        this.routePosition = routePosition;
        this.deliveredAt = deliveredAt;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public Long getId() {
        return id;
    }

    public TripEntity getTrip() {
        return trip;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public int getRoutePosition() {
        return routePosition;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public double getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public Instant getAvailabilityNotifiedAt() {
        return availabilityNotifiedAt;
    }

    public Instant getAvailabilityConfirmedAt() {
        return availabilityConfirmedAt;
    }

    public Instant getDeliveryConfirmationRequestedAt() {
        return deliveryConfirmationRequestedAt;
    }

    public Instant getDeliveryFailedAt() {
        return deliveryFailedAt;
    }

    public String getDeliveryFailureReason() {
        return deliveryFailureReason;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public boolean isDeliveryFailed() {
        return deliveryFailedAt != null;
    }

    public boolean isResolved() {
        return isDelivered() || isDeliveryFailed();
    }

    public boolean isAvailabilityConfirmed() {
        return availabilityConfirmedAt != null;
    }

    public void markAvailabilityNotified(Instant notifiedAt) {
        if (notifiedAt == null) {
            throw new InvalidInputException("notifiedAt must not be null");
        }

        if (availabilityNotifiedAt == null) {
            availabilityNotifiedAt = notifiedAt;
        }
    }

    public void markAvailabilityConfirmed(Instant confirmedAt) {
        if (confirmedAt == null) {
            throw new InvalidInputException("confirmedAt must not be null");
        }

        if (availabilityNotifiedAt == null) {
            availabilityNotifiedAt = confirmedAt;
        }

        availabilityConfirmedAt = confirmedAt;
    }

    public void markDeliveryConfirmationRequested(Instant requestedAt) {
        if (requestedAt == null) {
            throw new InvalidInputException("requestedAt must not be null");
        }

        if (deliveryConfirmationRequestedAt == null) {
            deliveryConfirmationRequestedAt = requestedAt;
        }
    }

    public void markDeliveryFailed(Instant failedAt, String reason) {
        if (failedAt == null) {
            throw new InvalidInputException("failedAt must not be null");
        }

        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("delivery failure reason must not be blank");
        }

        if (deliveryFailedAt == null) {
            deliveryFailedAt = failedAt;
            deliveryFailureReason = reason.trim();
        }
    }

    public void markDelivered() {
        if (deliveryFailedAt != null) {
            throw new InvalidInputException("route position already marked not delivered");
        }

        if (deliveredAt == null) {
            deliveredAt = Instant.now();
        }
    }
}
