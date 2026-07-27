package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TripTransitionService {

    public static final String EARLY_RETURN_RECHARGE_REASON =
            "drone returned early to preserve minimum return battery";

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);

    private final TripStorage storage;
    private final TripTelemetryService telemetryService;

    public TripTransitionService(TripStorage storage, TripTelemetryService telemetryService) {
        this.storage = storage;
        this.telemetryService = telemetryService;
    }

    @Transactional
    public TripEntity start(Long id) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new InvalidInputException("trip must be PLANNED to start");
        }

        if (trip.getDrone().getStatus() != DroneStatus.AVAILABLE) {
            throw new InvalidInputException("drone must be AVAILABLE to start trip");
        }

        Instant now = Instant.now();
        if (!TripDispatchPolicy.isDispatchWindowOpen(trip, now)) {
            throw new InvalidInputException("trip must wait until ideal dispatch time");
        }

        if (!canCompleteTripWithSafeReturn(trip.getDrone(), trip.getTotalDistance())) {
            throw new InvalidInputException("drone battery is insufficient for complete trip and safe return");
        }

        trip.markStarted(now);
        trip.changeStatus(TripStatus.IN_ROUTE);
        trip.getDrone().changeStatus(DroneStatus.IN_ROUTE);

        for (TripOrderEntity tripOrder : trip.getTripOrders()) {
            tripOrder.getOrder().changeStatus(OrderStatus.IN_ROUTE);
        }

        return trip;
    }

    @Transactional
    public TripEntity complete(Long id) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.IN_ROUTE) {
            throw new InvalidInputException("trip must be IN_ROUTE to complete");
        }

        if (!canCompleteTripWithSafeReturn(trip.getDrone(), trip.getTotalDistance())) {
            return returnEarly(trip);
        }

        if (orderedTripOrders(trip).stream().anyMatch(tripOrder -> !tripOrder.isResolved())) {
            throw new InvalidInputException("all route positions must be resolved before completing trip");
        }

        completeTrip(trip);

        return trip;
    }

    @Transactional
    public TripEntity reportTelemetry(Long id, double batteryLevel) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.IN_ROUTE) {
            throw new InvalidInputException("trip must be IN_ROUTE to report telemetry");
        }

        trip.getDrone().updateBatteryLevel(batteryLevel);
        telemetryService.record(trip, batteryLevel);

        if (!canCompleteTripWithSafeReturn(trip.getDrone(), trip.getTotalDistance())) {
            return returnEarly(trip);
        }

        return trip;
    }

    @Transactional
    public TripEntity deliverRoutePosition(Long id, int routePosition, String confirmationCode) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.IN_ROUTE) {
            throw new InvalidInputException("trip must be IN_ROUTE to report delivery");
        }

        if (routePosition < 0) {
            throw new InvalidInputException("routePosition must not be negative");
        }

        List<TripOrderEntity> tripOrders = orderedTripOrders(trip);
        TripOrderEntity routeOrder = tripOrders.stream()
                .filter(tripOrder -> tripOrder.getRoutePosition() == routePosition)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("trip route position not found"));

        boolean hasUndeliveredPreviousRoutePosition = tripOrders.stream()
                .filter(tripOrder -> tripOrder.getRoutePosition() < routePosition)
                .anyMatch(tripOrder -> !tripOrder.isResolved());

        if (hasUndeliveredPreviousRoutePosition) {
            throw new InvalidInputException("previous route positions must be delivered first");
        }

        if (routeOrder.isDelivered()) {
            throw new InvalidInputException("route position already delivered");
        }

        if (routeOrder.isDeliveryFailed()) {
            throw new InvalidInputException("route position already marked not delivered");
        }

        if (confirmationCode == null || confirmationCode.isBlank()) {
            throw new InvalidInputException("delivery confirmation code must not be blank");
        }

        if (!routeOrder.isAvailabilityConfirmed()) {
            throw new InvalidInputException("delivery availability must be confirmed before delivery");
        }

        if (!hasReachedRoutePosition(trip, routeOrder)) {
            throw new InvalidInputException("drone has not reached route position yet");
        }

        routeOrder.markDeliveryConfirmationRequested(Instant.now());
        if (DeliveryAvailabilityPolicy.hasDeliveryConfirmationExpired(routeOrder, Instant.now())) {
            throw new InvalidInputException("delivery confirmation window expired");
        }

        if (!routeOrder.getOrder().getDeliveryConfirmationCode().equalsIgnoreCase(confirmationCode.trim())) {
            throw new InvalidInputException("delivery confirmation code is invalid");
        }

        routeOrder.markDelivered();
        routeOrder.getOrder().changeStatus(OrderStatus.DELIVERED);

        return trip;
    }

    @Transactional
    public TripEntity confirmRouteAvailability(Long id, int routePosition, boolean available) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.IN_ROUTE) {
            throw new InvalidInputException("trip must be IN_ROUTE to confirm delivery availability");
        }

        if (routePosition < 0) {
            throw new InvalidInputException("routePosition must not be negative");
        }

        List<TripOrderEntity> tripOrders = orderedTripOrders(trip);
        TripOrderEntity routeOrder = tripOrders.stream()
                .filter(tripOrder -> tripOrder.getRoutePosition() == routePosition)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("trip route position not found"));

        boolean hasUndeliveredPreviousRoutePosition = tripOrders.stream()
                .filter(tripOrder -> tripOrder.getRoutePosition() < routePosition)
                .anyMatch(tripOrder -> !tripOrder.isResolved());

        if (hasUndeliveredPreviousRoutePosition) {
            throw new InvalidInputException("previous route positions must be delivered first");
        }

        if (routeOrder.isDelivered()) {
            throw new InvalidInputException("route position already delivered");
        }

        if (routeOrder.isDeliveryFailed()) {
            throw new InvalidInputException("route position already marked not delivered");
        }

        if (routeOrder.getAvailabilityNotifiedAt() == null) {
            throw new InvalidInputException("delivery availability has not been requested yet");
        }

        Instant now = Instant.now();
        if (DeliveryAvailabilityPolicy.hasResponseExpired(routeOrder, now)) {
            TripDeliveryReturnOperations.returnToBaseWithUndeliveredPackage(
                    trip,
                    routeOrder,
                    DeliveryAvailabilityPolicy.UNCONFIRMED_AVAILABILITY_REASON,
                    now
            );
            return trip;
        }

        if (!available) {
            TripDeliveryReturnOperations.returnToBaseWithUndeliveredPackage(
                    trip,
                    routeOrder,
                    DeliveryAvailabilityPolicy.DECLINED_AVAILABILITY_REASON,
                    now
            );
            return trip;
        }

        routeOrder.markAvailabilityConfirmed(now);
        if (hasReachedRoutePosition(trip, routeOrder)) {
            routeOrder.markDeliveryConfirmationRequested(now);
        }

        return trip;
    }

    private TripEntity returnEarly(TripEntity trip) {
        DroneEntity drone = trip.getDrone();
        List<TripOrderEntity> tripOrders = orderedTripOrders(trip);
        Instant now = Instant.now();
        double travelledDistance = 0.0;
        Coordinate currentLocation = BASE_LOCATION;

        for (TripOrderEntity tripOrder : tripOrders) {
            if (!tripOrder.isDelivered()) {
                break;
            }

            Coordinate orderLocation = locationOf(tripOrder.getOrder());
            double distanceToOrder = currentLocation.distanceTo(orderLocation);

            travelledDistance += distanceToOrder;
            currentLocation = orderLocation;
        }

        travelledDistance += currentLocation.distanceTo(BASE_LOCATION);
        drone.consumeBatteryForDistance(travelledDistance);
        trip.changeStatus(TripStatus.RETURNED_EARLY);
        trip.markEnded(now);
        drone.enqueueForRecharge(EARLY_RETURN_RECHARGE_REASON);

        for (TripOrderEntity tripOrder : tripOrders) {
            OrderEntity order = tripOrder.getOrder();
            if (tripOrder.isDelivered()) {
                order.changeStatus(OrderStatus.DELIVERED);
            } else if (tripOrder.isDeliveryFailed()) {
                order.changeStatus(OrderStatus.NOT_DELIVERED, tripOrder.getDeliveryFailureReason());
            } else {
                order.changeStatus(OrderStatus.PENDING_REASSIGNMENT);
            }
        }

        return trip;
    }

    private void completeTrip(TripEntity trip) {
        trip.changeStatus(TripStatus.COMPLETED);
        trip.markEnded(Instant.now());
        trip.getDrone().changeStatus(DroneStatus.AVAILABLE);
    }

    @Transactional
    public TripEntity cancel(Long id) {
        TripEntity trip = storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        if (trip.getStatus() != TripStatus.PLANNED && trip.getStatus() != TripStatus.IN_ROUTE) {
            throw new InvalidInputException("trip must be PLANNED or IN_ROUTE to cancel");
        }

        trip.changeStatus(TripStatus.CANCELLED);
        trip.markCancelled(Instant.now());
        trip.getDrone().changeStatus(DroneStatus.AVAILABLE);

        for (TripOrderEntity tripOrder : trip.getTripOrders()) {
            OrderEntity order = tripOrder.getOrder();
            if (tripOrder.isDeliveryFailed()) {
                order.changeStatus(OrderStatus.NOT_DELIVERED, tripOrder.getDeliveryFailureReason());
            } else if (order.getStatus() != OrderStatus.DELIVERED) {
                order.changeStatus(OrderStatus.REQUESTED);
            }
        }

        return trip;
    }

    private boolean canCompleteTripWithSafeReturn(DroneEntity drone, double totalDistance) {
        double requiredBattery = totalDistance * drone.getBatteryConsumptionPerDistanceUnit()
                + drone.getMinimumReturnBattery();

        return requiredBattery <= drone.getBatteryLevel();
    }

    private Coordinate locationOf(OrderEntity order) {
        return new Coordinate(order.getLocationX(), order.getLocationY());
    }

    private boolean hasReachedRoutePosition(TripEntity trip, TripOrderEntity routeOrder) {
        double deliveryDistance = MeasurementUnits.distanceForMinutes(
                routeOrder.getEstimatedDeliveryTime(),
                trip.getDrone().getSpeed()
        );
        return trip.getSimulationTravelledDistance() + 1.0E-9 >= deliveryDistance;
    }

    private List<TripOrderEntity> orderedTripOrders(TripEntity trip) {
        return trip.getTripOrders().stream()
                .sorted(Comparator.comparingInt(TripOrderEntity::getRoutePosition))
                .toList();
    }
}
