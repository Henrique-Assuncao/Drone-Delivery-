package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drone_id", nullable = false)
    private DroneEntity drone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(name = "total_weight", nullable = false)
    private double totalWeight;

    @Column(name = "total_distance", nullable = false)
    private double totalDistance;

    @Column(name = "planned_at", nullable = false)
    private Instant plannedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "simulation_current_x", nullable = false)
    private double simulationCurrentX;

    @Column(name = "simulation_current_y", nullable = false)
    private double simulationCurrentY;

    @Column(name = "simulation_travelled_distance", nullable = false)
    private double simulationTravelledDistance;

    @Column(name = "simulation_updated_at")
    private Instant simulationUpdatedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("routePosition ASC")
    private List<TripOrderEntity> tripOrders = new ArrayList<>();

    protected TripEntity() {
    }

    public TripEntity(
            Long id,
            DroneEntity drone,
            TripStatus status,
            double totalWeight,
            double totalDistance
    ) {
        if (drone == null) {
            throw new InvalidInputException("drone must not be null");
        }

        if (status == null) {
            throw new InvalidInputException("status must not be null");
        }

        this.id = id;
        this.drone = drone;
        this.status = status;
        this.totalWeight = totalWeight;
        this.totalDistance = totalDistance;
    }

    public Long getId() {
        return id;
    }

    public DroneEntity getDrone() {
        return drone;
    }

    public TripStatus getStatus() {
        return status;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public Instant getPlannedAt() {
        return plannedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public double getSimulationCurrentX() {
        return simulationCurrentX;
    }

    public double getSimulationCurrentY() {
        return simulationCurrentY;
    }

    public double getSimulationTravelledDistance() {
        return simulationTravelledDistance;
    }

    public Instant getSimulationUpdatedAt() {
        return simulationUpdatedAt;
    }

    public double getEstimatedDuration() {
        return MeasurementUnits.minutesForDistance(totalDistance, drone.getSpeed());
    }

    public double getAverageDeliveryTime() {
        return tripOrders.stream()
                .mapToDouble(TripOrderEntity::getEstimatedDeliveryTime)
                .average()
                .orElse(0.0);
    }

    public List<TripOrderEntity> getTripOrders() {
        return List.copyOf(tripOrders);
    }

    public void changeStatus(TripStatus status) {
        if (status == null) {
            throw new InvalidInputException("status must not be null");
        }

        this.status = status;
    }

    public void markStarted(Instant startedAt) {
        if (startedAt == null) {
            throw new InvalidInputException("startedAt must not be null");
        }

        this.startedAt = startedAt;
        this.cancelledAt = null;
    }

    public void markEnded(Instant endedAt) {
        if (endedAt == null) {
            throw new InvalidInputException("endedAt must not be null");
        }

        this.endedAt = endedAt;
    }

    public void markCancelled(Instant cancelledAt) {
        if (cancelledAt == null) {
            throw new InvalidInputException("cancelledAt must not be null");
        }

        this.cancelledAt = cancelledAt;
    }

    public void updateSimulationState(double currentX, double currentY, double travelledDistance, Instant updatedAt) {
        if (travelledDistance < 0) {
            throw new InvalidInputException("travelledDistance must not be negative");
        }

        if (updatedAt == null) {
            throw new InvalidInputException("updatedAt must not be null");
        }

        this.simulationCurrentX = currentX;
        this.simulationCurrentY = currentY;
        this.simulationTravelledDistance = travelledDistance;
        this.simulationUpdatedAt = updatedAt;
    }

    public void addOrder(OrderEntity order, int routePosition) {
        addOrder(order, routePosition, null, 0.0);
    }

    public void addOrder(OrderEntity order, int routePosition, Instant deliveredAt) {
        addOrder(order, routePosition, deliveredAt, 0.0);
    }

    public void addOrder(OrderEntity order, int routePosition, Instant deliveredAt, double estimatedDeliveryTime) {
        addOrder(order, routePosition, deliveredAt, estimatedDeliveryTime, null, null);
    }

    public void addOrder(
            OrderEntity order,
            int routePosition,
            Instant deliveredAt,
            double estimatedDeliveryTime,
            Instant availabilityNotifiedAt,
            Instant availabilityConfirmedAt
    ) {
        addOrder(
                order,
                routePosition,
                deliveredAt,
                estimatedDeliveryTime,
                availabilityNotifiedAt,
                availabilityConfirmedAt,
                null,
                null,
                null
        );
    }

    public void addOrder(
            OrderEntity order,
            int routePosition,
            Instant deliveredAt,
            double estimatedDeliveryTime,
            Instant availabilityNotifiedAt,
            Instant availabilityConfirmedAt,
            Instant deliveryConfirmationRequestedAt,
            Instant deliveryFailedAt,
            String deliveryFailureReason
    ) {
        TripOrderEntity tripOrder = new TripOrderEntity(this, order, routePosition, deliveredAt, estimatedDeliveryTime);
        if (availabilityNotifiedAt != null) {
            tripOrder.markAvailabilityNotified(availabilityNotifiedAt);
        }
        if (availabilityConfirmedAt != null) {
            tripOrder.markAvailabilityConfirmed(availabilityConfirmedAt);
        }
        if (deliveryConfirmationRequestedAt != null) {
            tripOrder.markDeliveryConfirmationRequested(deliveryConfirmationRequestedAt);
        }
        if (deliveryFailedAt != null) {
            tripOrder.markDeliveryFailed(deliveryFailedAt, deliveryFailureReason);
        }

        tripOrders.add(tripOrder);
    }
}
