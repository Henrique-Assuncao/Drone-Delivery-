package com.example.drone.domain;

import com.example.drone.exception.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TripPlanner {

    private static final String GENERIC_UNALLOCATED_REASON = "order cannot be served by any drone";
    private static final String WEIGHT_UNALLOCATED_REASON = "order exceeds max drone weight capacity";
    private static final String RANGE_UNALLOCATED_REASON = "order exceeds max drone range";
    private static final String BATTERY_UNALLOCATED_REASON =
            "order exceeds drone battery for complete trip and safe return";
    private static final String WEIGHT_AND_RANGE_UNALLOCATED_REASON =
            "order exceeds max drone weight capacity and max drone range";
    private static final String IMMEDIATE_DRONE_UNALLOCATED_REASON =
            "order requires another drone but no immediate drone is available";
    private static final RouteDistanceCalculator DISTANCE_CALCULATOR = new RouteDistanceCalculator();

    public TripPlan plan(List<Drone> drones, List<Order> orders) {
        return plan(drones, orders, true, List.of());
    }

    public TripPlan plan(List<Drone> drones, List<Order> orders, boolean optimizeRoute) {
        return plan(drones, orders, optimizeRoute, List.of());
    }

    public TripPlan plan(List<Drone> drones, List<Order> orders, boolean optimizeRoute, List<Obstacle> obstacles) {
        return plan(drones, List.of(), orders, optimizeRoute, obstacles);
    }

    public TripPlan plan(
            List<Drone> drones,
            List<Trip> existingTrips,
            List<Order> orders,
            boolean optimizeRoute,
            List<Obstacle> obstacles
    ) {
        validateInput(drones, orders);
        validateExistingTrips(existingTrips);
        List<Obstacle> copiedObstacles = copyOfObstacles(obstacles);

        List<Drone> sortedDrones = sortDrones(drones);
        List<Trip> trips = new ArrayList<>(existingTrips);
        List<UnallocatedOrder> unallocatedOrders = new ArrayList<>();

        if (!optimizeRoute) {
            for (Order order : orders) {
                if (!canBeServedByAnyDrone(sortedDrones, order, false, copiedObstacles)) {
                    unallocatedOrders.add(new UnallocatedOrder(
                            order,
                            unallocatedReasonFor(sortedDrones, order, copiedObstacles)
                    ));
                    continue;
                }

                if (!addToExistingTrip(trips, order, false, copiedObstacles)) {
                    Trip newTrip = createTripWithFirstUnusedCapableDrone(sortedDrones, trips, order, false, copiedObstacles);
                    if (newTrip == null) {
                        unallocatedOrders.add(new UnallocatedOrder(order, IMMEDIATE_DRONE_UNALLOCATED_REASON));
                    } else {
                        trips.add(newTrip);
                    }
                }
            }

            return new TripPlan(trips, unallocatedOrders);
        }

        for (Order order : DeliveryOrdering.orderByDeliveryTimePriorityWeightAndDistance(orders, copiedObstacles)) {
            if (!canBeServedByAnyDrone(sortedDrones, order, true, copiedObstacles)) {
                unallocatedOrders.add(new UnallocatedOrder(
                        order,
                        unallocatedReasonFor(sortedDrones, order, copiedObstacles)
                ));
                continue;
            }

            if (!addToExistingTrip(trips, order, true, copiedObstacles)) {
                Trip newTrip = createTripWithFirstUnusedCapableDrone(sortedDrones, trips, order, true, copiedObstacles);
                if (newTrip == null) {
                    unallocatedOrders.add(new UnallocatedOrder(order, IMMEDIATE_DRONE_UNALLOCATED_REASON));
                } else {
                    trips.add(newTrip);
                }
            }
        }

        return new TripPlan(trips, unallocatedOrders);
    }

    private void validateExistingTrips(List<Trip> existingTrips) {
        if (existingTrips == null) {
            throw new InvalidInputException("existingTrips must not be null");
        }

        for (Trip trip : existingTrips) {
            if (trip == null) {
                throw new InvalidInputException("existingTrips must not contain null");
            }
        }
    }

    private void validateInput(List<Drone> drones, List<Order> orders) {
        if (drones == null) {
            throw new InvalidInputException("drones must not be null");
        }

        if (orders == null) {
            throw new InvalidInputException("orders must not be null");
        }

        for (Drone drone : drones) {
            if (drone == null) {
                throw new InvalidInputException("drones must not contain null");
            }
        }

        for (Order order : orders) {
            if (order == null) {
                throw new InvalidInputException("orders must not contain null");
            }
        }
    }

    private List<Obstacle> copyOfObstacles(List<Obstacle> obstacles) {
        if (obstacles == null) {
            throw new InvalidInputException("obstacles must not be null");
        }

        for (Obstacle obstacle : obstacles) {
            if (obstacle == null) {
                throw new InvalidInputException("obstacles must not contain null");
            }
        }

        return List.copyOf(obstacles);
    }

    private List<Drone> sortDrones(List<Drone> drones) {
        return drones.stream()
                .sorted(Comparator.comparingDouble(Drone::maxWeightCapacity)
                        .thenComparing(Comparator.comparingDouble(Drone::maxRange))
                        .thenComparing(Drone::identifier))
                .toList();
    }

    private boolean canBeServedByAnyDrone(
            List<Drone> drones,
            Order order,
            boolean optimizeRoute,
            List<Obstacle> obstacles
    ) {
        for (Drone drone : drones) {
            if (canCreateTrip(drone, List.of(order), optimizeRoute, obstacles)) {
                return true;
            }
        }

        return false;
    }

    private String unallocatedReasonFor(List<Drone> drones, Order order, List<Obstacle> obstacles) {
        if (drones.isEmpty()) {
            return GENERIC_UNALLOCATED_REASON;
        }

        boolean supportedByWeight = false;
        boolean supportedByRange = false;
        boolean supportedByBattery = false;
        double requiredRange = roundTripDistanceFromBase(order, obstacles);

        for (Drone drone : drones) {
            if (drone.supportsWeightOf(order)) {
                supportedByWeight = true;
            }

            if (requiredRange <= drone.maxRange()) {
                supportedByRange = true;
            }

            if (drone.canCompleteTripWithSafeReturn(requiredRange)) {
                supportedByBattery = true;
            }
        }

        if (!supportedByWeight && !supportedByRange) {
            return WEIGHT_AND_RANGE_UNALLOCATED_REASON;
        }

        if (!supportedByWeight) {
            return WEIGHT_UNALLOCATED_REASON;
        }

        if (!supportedByRange) {
            return RANGE_UNALLOCATED_REASON;
        }

        if (!supportedByBattery) {
            return BATTERY_UNALLOCATED_REASON;
        }

        return GENERIC_UNALLOCATED_REASON;
    }

    private boolean addToExistingTrip(
            List<Trip> trips,
            Order order,
            boolean optimizeRoute,
            List<Obstacle> obstacles
    ) {
        for (int index = 0; index < trips.size(); index++) {
            Trip trip = trips.get(index);
            List<Order> candidateOrders = new ArrayList<>(trip.orders());
            candidateOrders.add(order);

            if (canCreateTrip(trip.drone(), candidateOrders, optimizeRoute, obstacles)) {
                trips.set(index, new Trip(trip.drone(), candidateOrders, optimizeRoute, obstacles));
                return true;
            }
        }

        return false;
    }

    private Trip createTripWithFirstUnusedCapableDrone(
            List<Drone> drones,
            List<Trip> trips,
            Order order,
            boolean optimizeRoute,
            List<Obstacle> obstacles
    ) {
        for (Drone drone : drones) {
            if (hasPlannedTripForDrone(trips, drone)) {
                continue;
            }

            if (canCreateTrip(drone, List.of(order), optimizeRoute, obstacles)) {
                return new Trip(drone, List.of(order), optimizeRoute, obstacles);
            }
        }

        return null;
    }

    private boolean hasPlannedTripForDrone(List<Trip> trips, Drone drone) {
        return trips.stream()
                .anyMatch(trip -> trip.drone().equals(drone));
    }

    private boolean canCreateTrip(Drone drone, List<Order> orders, boolean optimizeRoute, List<Obstacle> obstacles) {
        try {
            new Trip(drone, orders, optimizeRoute, obstacles);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private double roundTripDistanceFromBase(Order order, List<Obstacle> obstacles) {
        return DISTANCE_CALCULATOR.roundTripDistanceFromBase(order, obstacles);
    }
}
