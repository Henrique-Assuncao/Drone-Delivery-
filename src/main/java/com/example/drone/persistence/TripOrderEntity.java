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

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public void markDelivered() {
        if (deliveredAt == null) {
            deliveredAt = Instant.now();
        }
    }
}
