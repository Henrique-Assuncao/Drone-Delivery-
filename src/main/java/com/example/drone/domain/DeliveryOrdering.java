package com.example.drone.domain;

import com.example.drone.exception.*;

import java.util.Comparator;
import java.util.List;

final class DeliveryOrdering {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);
    private static final RouteDistanceCalculator DISTANCE_CALCULATOR = new RouteDistanceCalculator();

    private DeliveryOrdering() {
    }

    static List<Order> orderByPriorityWeightAndDistance(List<Order> orders, List<Obstacle> obstacles) {
        return orders.stream()
                .sorted(Comparator.comparingInt((Order order) -> priorityRank(order.priority())).reversed()
                        .thenComparing(Comparator.comparingDouble(Order::weight).reversed())
                        .thenComparingDouble(order -> distanceFromBase(order, obstacles))
                        .thenComparing(Order::identifier))
                .toList();
    }

    private static int priorityRank(Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private static double distanceFromBase(Order order, List<Obstacle> obstacles) {
        return DISTANCE_CALCULATOR.segmentDistance(BASE_LOCATION, order.location(), obstacles);
    }
}
