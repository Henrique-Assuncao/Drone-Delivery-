package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.persistence.*;

import java.time.Instant;
import java.util.Comparator;

final class TripDeliveryReturnOperations {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);

    private TripDeliveryReturnOperations() {
    }

    static void returnToBaseWithUndeliveredPackage(
            TripEntity trip,
            TripOrderEntity failedRouteOrder,
            String failedOrderReason,
            Instant now
    ) {
        DroneEntity drone = trip.getDrone();
        Coordinate currentLocation = new Coordinate(trip.getSimulationCurrentX(), trip.getSimulationCurrentY());
        double returnDistance = currentLocation.distanceTo(BASE_LOCATION);

        if (returnDistance > 0) {
            drone.consumeBatteryForDistance(returnDistance);
        }

        trip.changeStatus(TripStatus.RETURNED_EARLY);
        trip.markEnded(now);
        trip.updateSimulationState(BASE_LOCATION.x(), BASE_LOCATION.y(), trip.getSimulationTravelledDistance(), now);
        drone.changeStatus(DroneStatus.AVAILABLE);

        for (TripOrderEntity tripOrder : trip.getTripOrders().stream()
                .sorted(Comparator.comparingInt(TripOrderEntity::getRoutePosition))
                .toList()) {
            OrderEntity order = tripOrder.getOrder();
            if (tripOrder.isDelivered()) {
                order.changeStatus(OrderStatus.DELIVERED);
            } else if (tripOrder.isDeliveryFailed()) {
                order.changeStatus(OrderStatus.NOT_DELIVERED, tripOrder.getDeliveryFailureReason());
            } else if (tripOrder.getRoutePosition() == failedRouteOrder.getRoutePosition()) {
                tripOrder.markDeliveryFailed(now, failedOrderReason);
                order.changeStatus(OrderStatus.NOT_DELIVERED, failedOrderReason);
            } else {
                order.changeStatus(OrderStatus.PENDING_REASSIGNMENT, DeliveryAvailabilityPolicy.ROUTE_INTERRUPTED_REASON);
            }
        }
    }
}
