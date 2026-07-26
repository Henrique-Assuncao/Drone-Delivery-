package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TripSimulationService {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);
    private static final String SIMULATION_EARLY_RETURN_RECHARGE_REASON =
            "drone returned early during automatic simulation";

    private final TripStorage tripStorage;

    public TripSimulationService(TripStorage tripStorage) {
        this.tripStorage = tripStorage;
    }

    @Transactional(readOnly = true)
    public TripSimulationState findState(Long tripId) {
        TripEntity trip = tripStorage.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        return stateFor(trip);
    }

    @Transactional
    public TripSimulationState advance(Long tripId, double elapsedMinutes) {
        if (elapsedMinutes <= 0) {
            throw new InvalidInputException("elapsedMinutes must be greater than zero");
        }

        TripEntity trip = tripStorage.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() == TripStatus.PLANNED) {
            startTrip(trip);
        }

        if (trip.getStatus() != TripStatus.IN_ROUTE) {
            return stateFor(trip);
        }

        double currentDistance = trip.getSimulationTravelledDistance();
        double distanceToAdvance = elapsedMinutes * trip.getDrone().getSpeed();
        double nextDistance = Math.min(trip.getTotalDistance(), currentDistance + distanceToAdvance);
        double advancedDistance = Math.max(0.0, nextDistance - currentDistance);
        Instant now = Instant.now();

        if (advancedDistance > 0) {
            trip.getDrone().consumeBatteryForDistance(advancedDistance);
        }

        Coordinate currentLocation = locationAt(trip, nextDistance);
        trip.updateSimulationState(currentLocation.x(), currentLocation.y(), nextDistance, now);
        deliverReachedOrders(trip, nextDistance);

        if (nextDistance >= trip.getTotalDistance()) {
            completeTrip(trip, now);
            return stateFor(trip);
        }

        if (mustReturnEarly(trip, nextDistance)) {
            returnEarly(trip, currentLocation, now);
        }

        return stateFor(trip);
    }

    private void startTrip(TripEntity trip) {
        if (trip.getDrone().getStatus() != DroneStatus.AVAILABLE) {
            throw new InvalidInputException("drone must be AVAILABLE to start trip");
        }

        if (!canCompleteRemainingWithSafeReturn(trip, 0.0)) {
            throw new InvalidInputException("drone battery is insufficient for complete trip and safe return");
        }

        trip.markStarted(Instant.now());
        trip.changeStatus(TripStatus.IN_ROUTE);
        trip.getDrone().changeStatus(DroneStatus.IN_ROUTE);
        trip.updateSimulationState(BASE_LOCATION.x(), BASE_LOCATION.y(), 0.0, Instant.now());

        for (TripOrderEntity tripOrder : orderedTripOrders(trip)) {
            tripOrder.getOrder().changeStatus(OrderStatus.IN_ROUTE);
        }
    }

    private void deliverReachedOrders(TripEntity trip, double travelledDistance) {
        for (TripOrderEntity tripOrder : orderedTripOrders(trip)) {
            if (tripOrder.isDelivered()) {
                continue;
            }

            double deliveryDistance = tripOrder.getEstimatedDeliveryTime() * trip.getDrone().getSpeed();
            if (travelledDistance + 1.0E-9 < deliveryDistance) {
                break;
            }

            tripOrder.markDelivered();
            tripOrder.getOrder().changeStatus(OrderStatus.DELIVERED);
        }
    }

    private void completeTrip(TripEntity trip, Instant now) {
        trip.changeStatus(TripStatus.COMPLETED);
        trip.markEnded(now);
        trip.getDrone().changeStatus(DroneStatus.AVAILABLE);
        trip.updateSimulationState(BASE_LOCATION.x(), BASE_LOCATION.y(), trip.getTotalDistance(), now);

        for (TripOrderEntity tripOrder : orderedTripOrders(trip)) {
            tripOrder.markDelivered();
            tripOrder.getOrder().changeStatus(OrderStatus.DELIVERED);
        }
    }

    private void returnEarly(TripEntity trip, Coordinate currentLocation, Instant now) {
        DroneEntity drone = trip.getDrone();
        double batteryAfterReturn = Math.max(
                drone.getMinimumReturnBattery(),
                drone.getBatteryLevel() - currentLocation.distanceTo(BASE_LOCATION) * drone.getBatteryConsumptionPerDistanceUnit()
        );

        drone.updateBatteryLevel(Math.min(100.0, batteryAfterReturn));
        trip.changeStatus(TripStatus.RETURNED_EARLY);
        trip.markEnded(now);
        trip.updateSimulationState(BASE_LOCATION.x(), BASE_LOCATION.y(), trip.getSimulationTravelledDistance(), now);
        drone.enqueueForRecharge(SIMULATION_EARLY_RETURN_RECHARGE_REASON);

        for (TripOrderEntity tripOrder : orderedTripOrders(trip)) {
            OrderEntity order = tripOrder.getOrder();
            if (tripOrder.isDelivered()) {
                order.changeStatus(OrderStatus.DELIVERED);
            } else {
                order.changeStatus(OrderStatus.PENDING_REASSIGNMENT);
            }
        }
    }

    private boolean mustReturnEarly(TripEntity trip, double travelledDistance) {
        return !canCompleteRemainingWithSafeReturn(trip, travelledDistance);
    }

    private boolean canCompleteRemainingWithSafeReturn(TripEntity trip, double travelledDistance) {
        DroneEntity drone = trip.getDrone();
        double remainingDistance = Math.max(0.0, trip.getTotalDistance() - travelledDistance);
        double requiredBattery = remainingDistance * drone.getBatteryConsumptionPerDistanceUnit()
                + drone.getMinimumReturnBattery();

        return requiredBattery <= drone.getBatteryLevel();
    }

    public static TripSimulationState stateFor(TripEntity trip) {
        TripOrderEntity nextTripOrder = orderedTripOrders(trip).stream()
                .filter(tripOrder -> !tripOrder.isDelivered())
                .findFirst()
                .orElse(null);
        double progress = trip.getTotalDistance() <= 0
                ? 1.0
                : Math.min(1.0, trip.getSimulationTravelledDistance() / trip.getTotalDistance());

        return new TripSimulationState(
                trip.getId(),
                trip.getDrone().getId(),
                trip.getStatus(),
                new Coordinate(trip.getSimulationCurrentX(), trip.getSimulationCurrentY()),
                trip.getSimulationTravelledDistance(),
                trip.getTotalDistance(),
                progress,
                nextTripOrder == null ? null : nextTripOrder.getOrder().getId(),
                nextTripOrder == null ? null : nextTripOrder.getRoutePosition(),
                trip.getStatus() == TripStatus.IN_ROUTE,
                trip.getSimulationUpdatedAt()
        );
    }

    private Coordinate locationAt(TripEntity trip, double travelledDistance) {
        List<Coordinate> routePoints = routePoints(trip);
        double visualRouteDistance = routeDistance(routePoints);

        if (visualRouteDistance <= 0) {
            return BASE_LOCATION;
        }

        double progress = trip.getTotalDistance() <= 0
                ? 1.0
                : Math.min(1.0, Math.max(0.0, travelledDistance / trip.getTotalDistance()));
        double visualDistance = progress * visualRouteDistance;
        double consumedDistance = 0.0;

        for (int index = 0; index < routePoints.size() - 1; index++) {
            Coordinate start = routePoints.get(index);
            Coordinate end = routePoints.get(index + 1);
            double segmentDistance = start.distanceTo(end);

            if (segmentDistance <= 0) {
                continue;
            }

            if (consumedDistance + segmentDistance >= visualDistance) {
                double segmentProgress = (visualDistance - consumedDistance) / segmentDistance;
                return new Coordinate(
                        start.x() + (end.x() - start.x()) * segmentProgress,
                        start.y() + (end.y() - start.y()) * segmentProgress
                );
            }

            consumedDistance += segmentDistance;
        }

        return BASE_LOCATION;
    }

    private double routeDistance(List<Coordinate> routePoints) {
        double distance = 0.0;

        for (int index = 0; index < routePoints.size() - 1; index++) {
            distance += routePoints.get(index).distanceTo(routePoints.get(index + 1));
        }

        return distance;
    }

    private List<Coordinate> routePoints(TripEntity trip) {
        List<Coordinate> points = new ArrayList<>();
        points.add(BASE_LOCATION);

        for (TripOrderEntity tripOrder : orderedTripOrders(trip)) {
            OrderEntity order = tripOrder.getOrder();
            points.add(new Coordinate(order.getLocationX(), order.getLocationY()));
        }

        points.add(BASE_LOCATION);
        return points;
    }

    private static List<TripOrderEntity> orderedTripOrders(TripEntity trip) {
        return trip.getTripOrders().stream()
                .sorted(Comparator.comparingInt(TripOrderEntity::getRoutePosition))
                .toList();
    }
}
