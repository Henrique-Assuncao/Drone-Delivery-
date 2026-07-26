package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripTest {

    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    @Test
    void shouldCreateValidTripWithDeliveryRoute() {
        Drone drone = new Drone("DRONE-1", 12.0, 20.0);
        Order firstOrder = order("ORDER-1", 3.0, 4.0, 5.0);
        Order secondOrder = order("ORDER-2", 6.0, 8.0, 7.0);

        Trip trip = new Trip(drone, List.of(firstOrder, secondOrder));

        assertEquals(drone, trip.drone());
        assertEquals(List.of(secondOrder, firstOrder), trip.orders());
        assertEquals(List.of(secondOrder, firstOrder), trip.route());
        assertEquals(12.0, trip.totalWeight(), DECIMAL_TOLERANCE);
        assertEquals(20.0, trip.totalDistance(), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldOptimizeDeliverySequenceWithinTrip() {
        Drone drone = new Drone("DRONE-1", 10.0, 35.0);
        Order farEastOrder = order("ORDER-A", 10.0, 0.0, 1.0);
        Order northOrder = order("ORDER-C", 0.0, 10.0, 1.0);
        Order nearEastOrder = order("ORDER-B", 9.0, 0.0, 1.0);

        Trip trip = new Trip(drone, List.of(farEastOrder, northOrder, nearEastOrder));

        assertEquals(List.of(nearEastOrder, farEastOrder, northOrder), trip.route());
        assertEquals(20.0 + Math.sqrt(200.0), trip.totalDistance(), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldOrderDeliverySequenceByPriorityWeightAndDistance() {
        Drone drone = new Drone("DRONE-1", 30.0, 100.0, 100.0, 1.0, 20.0, 120.0, 10.0);
        Order lowPriorityOrder = order("ORDER-LOW", 1.0, 0.0, 9.0, Priority.LOW);
        Order mediumPriorityOrder = order("ORDER-MEDIUM", 2.0, 0.0, 9.0, Priority.MEDIUM);
        Order highLightOrder = order("ORDER-HIGH-LIGHT", 10.0, 0.0, 1.0, Priority.HIGH);
        Order highHeavyFarOrder = order("ORDER-HIGH-HEAVY-FAR", 8.0, 0.0, 5.0, Priority.HIGH);
        Order highHeavyNearOrder = order("ORDER-HIGH-HEAVY-NEAR", 3.0, 0.0, 5.0, Priority.HIGH);

        Trip trip = new Trip(
                drone,
                List.of(lowPriorityOrder, mediumPriorityOrder, highLightOrder, highHeavyFarOrder, highHeavyNearOrder)
        );

        assertEquals(
                List.of(highHeavyNearOrder, highHeavyFarOrder, highLightOrder, mediumPriorityOrder, lowPriorityOrder),
                trip.route()
        );
        assertEquals(List.of(1.5, 4.0, 5.0, 9.0, 9.5), trip.estimatedDeliveryTimes());
        assertEquals(5.8, trip.averageDeliveryTime(), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldKeepDeliverySequenceWhenRouteOptimizationIsDisabled() {
        Drone drone = new Drone("DRONE-1", 10.0, 60.0);
        Order farEastOrder = order("ORDER-A", 10.0, 0.0, 1.0);
        Order northOrder = order("ORDER-C", 0.0, 10.0, 1.0);
        Order nearEastOrder = order("ORDER-B", 9.0, 0.0, 1.0);

        Trip trip = new Trip(drone, List.of(farEastOrder, northOrder, nearEastOrder), false);

        assertEquals(List.of(farEastOrder, northOrder, nearEastOrder), trip.route());
        assertEquals(19.0 + Math.sqrt(200.0) + Math.sqrt(181.0), trip.totalDistance(), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldUseObstacleAdjustedDistance() {
        Drone drone = new Drone("DRONE-1", 10.0, 21.0);
        Order order = order("ORDER-1", 10.0, 0.0, 5.0);

        Trip trip = new Trip(
                drone,
                List.of(order),
                true,
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        assertEquals(20.401349627035764, trip.totalDistance(), 1.0E-8);
    }

    @Test
    void shouldRejectTripWhenObstacleAdjustedDistanceExceedsRange() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.3);
        Order order = order("ORDER-1", 10.0, 0.0, 5.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Trip(
                        drone,
                        List.of(order),
                        true,
                        List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
                )
        );

        assertEquals("trip total distance exceeds drone range", exception.getMessage());
    }

    @Test
    void shouldRejectTripAboveDroneWeightCapacity() {
        Drone drone = new Drone("DRONE-1", 9.0, 100.0);
        Order firstOrder = order("ORDER-1", 1.0, 1.0, 5.0);
        Order secondOrder = order("ORDER-2", 2.0, 2.0, 5.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Trip(drone, List.of(firstOrder, secondOrder))
        );

        assertEquals("trip total weight exceeds drone capacity", exception.getMessage());
    }

    @Test
    void shouldRejectTripAboveDroneRange() {
        Drone drone = new Drone("DRONE-1", 10.0, 9.9);
        Order order = order("ORDER-1", 3.0, 4.0, 5.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Trip(drone, List.of(order))
        );

        assertEquals("trip total distance exceeds drone range", exception.getMessage());
    }

    @Test
    void shouldRejectTripWithoutEnoughBatteryForCompleteRouteAndSafeReturn() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0, 29.9, 1.0, 20.0, 1.0, 10.0);
        Order order = order("ORDER-1", 3.0, 4.0, 5.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Trip(drone, List.of(order))
        );

        assertEquals("trip requires more battery than drone can safely use", exception.getMessage());
    }

    @Test
    void shouldRejectNullDrone() {
        Order order = order("ORDER-1", 1.0, 1.0, 1.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Trip(null, List.of(order))
        );

        assertEquals("drone must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOrders() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Trip(drone, null)
        );

        assertEquals("orders must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOrderInTrip() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);
        List<Order> orders = new ArrayList<>();
        orders.add(null);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Trip(drone, orders)
        );

        assertEquals("orders must not contain null", exception.getMessage());
    }

    private Order order(String identifier, double x, double y, double weight) {
        return new Order(identifier, new Coordinate(x, y), weight, Priority.HIGH);
    }

    private Order order(String identifier, double x, double y, double weight, Priority priority) {
        return new Order(identifier, new Coordinate(x, y), weight, priority);
    }
}
