package com.example.drone.domain;

import com.example.drone.exception.*;

import java.util.ArrayList;
import java.util.List;

public class Trip {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);
    private static final RouteDistanceCalculator DISTANCE_CALCULATOR = new RouteDistanceCalculator();

    private final Drone drone;
    private final List<Order> orders;
    private final List<Obstacle> obstacles;

    public Trip(Drone drone, List<Order> orders) {
        this(drone, orders, true, List.of());
    }

    public Trip(Drone drone, List<Order> orders, boolean optimizeRoute) {
        this(drone, orders, optimizeRoute, List.of());
    }

    public Trip(Drone drone, List<Order> orders, boolean optimizeRoute, List<Obstacle> obstacles) {
        if (drone == null) {
            throw new InvalidInputException("drone must not be null");
        }

        List<Obstacle> copiedObstacles = copyOfObstacles(obstacles);
        List<Order> route = routeOf(orders, optimizeRoute, copiedObstacles);

        double totalWeight = totalWeightOf(route);
        double totalDistance = totalDistanceOf(route, copiedObstacles);

        if (totalWeight > drone.maxWeightCapacity()) {
            throw new InvalidInputException("trip total weight exceeds drone capacity");
        }

        if (totalDistance > drone.maxRange()) {
            throw new InvalidInputException("trip total distance exceeds drone range");
        }

        if (!drone.canCompleteTripWithSafeReturn(totalDistance)) {
            throw new InvalidInputException("trip requires more battery than drone can safely use");
        }

        this.drone = drone;
        this.orders = route;
        this.obstacles = copiedObstacles;
    }

    public Drone drone() {
        return drone;
    }

    public List<Order> orders() {
        return orders;
    }

    public List<Order> route() {
        return orders;
    }

    public List<Obstacle> obstacles() {
        return obstacles;
    }

    public double totalWeight() {
        return totalWeightOf(orders);
    }

    public double totalDistance() {
        return totalDistanceOf(orders, obstacles);
    }

    public List<Double> estimatedDeliveryTimes() {
        List<Double> estimatedTimes = new ArrayList<>();
        Coordinate currentLocation = BASE_LOCATION;
        double elapsedTime = 0.0;

        for (Order order : orders) {
            double segmentDistance = DISTANCE_CALCULATOR.segmentDistance(currentLocation, order.location(), obstacles);
            elapsedTime += MeasurementUnits.minutesForDistance(segmentDistance, drone.speed());
            estimatedTimes.add(elapsedTime);
            currentLocation = order.location();
        }

        return List.copyOf(estimatedTimes);
    }

    public double averageDeliveryTime() {
        List<Double> estimatedTimes = estimatedDeliveryTimes();
        if (estimatedTimes.isEmpty()) {
            return 0.0;
        }

        return estimatedTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private static double totalWeightOf(List<Order> orders) {
        return orders.stream()
                .mapToDouble(Order::weight)
                .sum();
    }

    private static List<Order> routeOf(List<Order> orders, boolean optimizeRoute, List<Obstacle> obstacles) {
        List<Order> copiedOrders = copyOfOrders(orders);
        if (!optimizeRoute) {
            return copiedOrders;
        }

        return DeliveryOrdering.orderByPriorityWeightAndDistance(copiedOrders, obstacles);
    }

    private static List<Order> copyOfOrders(List<Order> orders) {
        if (orders == null) {
            throw new InvalidInputException("orders must not be null");
        }

        for (Order order : orders) {
            if (order == null) {
                throw new InvalidInputException("orders must not contain null");
            }
        }

        return List.copyOf(orders);
    }

    private static List<Obstacle> copyOfObstacles(List<Obstacle> obstacles) {
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

    private static double totalDistanceOf(List<Order> orders, List<Obstacle> obstacles) {
        return DISTANCE_CALCULATOR.routeDistance(orders, obstacles);
    }
}
