package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPlannerTest {

    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    private final TripPlanner planner = new TripPlanner();

    @Test
    void shouldReturnEmptyPlanWhenThereAreNoOrders() {
        TripPlan plan = planner.plan(List.of(new Drone("DRONE-1", 10.0, 20.0)), List.of());

        assertTrue(plan.trips().isEmpty());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldRejectNullDronesInput() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> planner.plan(null, List.of())
        );

        assertEquals("drones must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOrdersInput() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> planner.plan(List.of(), null)
        );

        assertEquals("orders must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullDroneInInput() {
        List<Drone> drones = new ArrayList<>();
        drones.add(null);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> planner.plan(drones, List.of())
        );

        assertEquals("drones must not contain null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOrderInInput() {
        List<Order> orders = new ArrayList<>();
        orders.add(null);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> planner.plan(List.of(), orders)
        );

        assertEquals("orders must not contain null", exception.getMessage());
    }

    @Test
    void shouldAllocateOneOrderInOneTrip() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order order = order("ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(order));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(order), plan.trips().get(0).orders());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldAllocateMultipleOrdersInSingleTripWhenCapacityAndRangeAllow() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order firstOrder = order("ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH);
        Order secondOrder = order("ORDER-2", 6.0, 8.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(firstOrder, secondOrder));

        assertEquals(1, plan.trips().size());
        assertEquals(2, plan.trips().get(0).orders().size());
        assertTrue(plan.trips().get(0).orders().containsAll(List.of(firstOrder, secondOrder)));
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldUseOptimizedRouteWhenCheckingWhetherOrdersFitInSingleTrip() {
        Drone drone = new Drone("DRONE-1", 10.0, 35.0);
        Order farEastOrder = order("ORDER-A", 10.0, 0.0, 1.0, Priority.HIGH);
        Order northOrder = order("ORDER-C", 0.0, 10.0, 1.0, Priority.HIGH);
        Order nearEastOrder = order("ORDER-B", 9.0, 0.0, 1.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(farEastOrder, northOrder, nearEastOrder));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(nearEastOrder, farEastOrder, northOrder), plan.trips().get(0).route());
        assertEquals(20.0 + Math.sqrt(200.0), plan.trips().get(0).totalDistance(), DECIMAL_TOLERANCE);
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldRespectInputOrderWhenRouteOptimizationIsDisabled() {
        Drone drone = new Drone("DRONE-1", 10.0, 60.0);
        Order farEastOrder = order("ORDER-A", 10.0, 0.0, 1.0, Priority.LOW);
        Order northOrder = order("ORDER-C", 0.0, 10.0, 1.0, Priority.HIGH);
        Order nearEastOrder = order("ORDER-B", 9.0, 0.0, 1.0, Priority.MEDIUM);

        TripPlan plan = planner.plan(
                List.of(drone),
                List.of(farEastOrder, northOrder, nearEastOrder),
                false
        );

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(farEastOrder, northOrder, nearEastOrder), plan.trips().get(0).route());
        assertEquals(19.0 + Math.sqrt(200.0) + Math.sqrt(181.0), plan.trips().get(0).totalDistance(), DECIMAL_TOLERANCE);
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldAllocateOverflowToAnotherDroneWhenOrdersDoNotFitTogether() {
        Drone firstDrone = new Drone("DRONE-1", 10.0, 20.0);
        Drone secondDrone = new Drone("DRONE-2", 10.0, 20.0);
        Order firstOrder = order("ORDER-1", 1.0, 1.0, 8.0, Priority.HIGH);
        Order secondOrder = order("ORDER-2", 2.0, 2.0, 8.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(firstDrone, secondDrone), List.of(firstOrder, secondOrder));

        assertEquals(2, plan.trips().size());
        assertEquals(firstDrone, plan.trips().get(0).drone());
        assertEquals(secondDrone, plan.trips().get(1).drone());
        assertEquals(1, plan.trips().get(0).orders().size());
        assertEquals(1, plan.trips().get(1).orders().size());
        assertTrue(plannedOrders(plan).containsAll(List.of(firstOrder, secondOrder)));
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldPreserveLargerDroneForHeavierPendingOrder() {
        Drone smallDrone = new Drone("DRONE-SMALL", 5.0, 20.0);
        Drone largeDrone = new Drone("DRONE-LARGE", 10.0, 20.0);
        Order earlierSmallOrder = order(
                "ORDER-SMALL",
                1.0,
                1.0,
                4.0,
                Priority.HIGH,
                Instant.parse("2026-07-26T17:00:00Z")
        );
        Order laterHeavyOrder = order(
                "ORDER-HEAVY",
                2.0,
                2.0,
                8.0,
                Priority.HIGH,
                Instant.parse("2026-07-26T17:05:00Z")
        );

        TripPlan plan = planner.plan(List.of(largeDrone, smallDrone), List.of(earlierSmallOrder, laterHeavyOrder));

        assertEquals(2, plan.trips().size());
        assertEquals(smallDrone, plan.trips().get(0).drone());
        assertEquals(List.of(earlierSmallOrder), plan.trips().get(0).orders());
        assertEquals(largeDrone, plan.trips().get(1).drone());
        assertEquals(List.of(laterHeavyOrder), plan.trips().get(1).orders());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldMarkOverflowAsUnallocatedWhenNoImmediateDroneIsAvailable() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order firstOrder = order("ORDER-1", 1.0, 1.0, 8.0, Priority.HIGH);
        Order secondOrder = order("ORDER-2", 2.0, 2.0, 8.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(firstOrder, secondOrder));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(firstOrder), plan.trips().get(0).orders());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(secondOrder, plan.unallocatedOrders().get(0).order());
        assertEquals(
                "order requires another drone but no immediate drone is available",
                plan.unallocatedOrders().get(0).reason()
        );
    }

    @Test
    void shouldAllocateOverflowToAnotherDroneWhenOrdersDoNotFitTogetherByBattery() {
        Drone firstDrone = new Drone("DRONE-1", 10.0, 100.0, 23.0, 1.0, 20.0, 1.0, 10.0);
        Drone secondDrone = new Drone("DRONE-2", 10.0, 100.0, 23.0, 1.0, 20.0, 1.0, 10.0);
        Order firstOrder = order("ORDER-1", 1.5, 0.0, 1.0, Priority.HIGH);
        Order secondOrder = order("ORDER-2", -1.5, 0.0, 1.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(firstDrone, secondDrone), List.of(firstOrder, secondOrder));

        assertEquals(2, plan.trips().size());
        assertEquals(firstDrone, plan.trips().get(0).drone());
        assertEquals(secondDrone, plan.trips().get(1).drone());
        assertEquals(1, plan.trips().get(0).orders().size());
        assertEquals(1, plan.trips().get(1).orders().size());
        assertTrue(plannedOrders(plan).containsAll(List.of(firstOrder, secondOrder)));
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenWeightExceedsAllDroneCapacities() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order impossibleOrder = order("ORDER-1", 3.0, 4.0, 10.1, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(impossibleOrder));

        assertTrue(plan.trips().isEmpty());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(impossibleOrder, plan.unallocatedOrders().get(0).order());
        assertEquals("order exceeds max drone weight capacity", plan.unallocatedOrders().get(0).reason());
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenRangeExceedsAllDroneRanges() {
        Drone drone = new Drone("DRONE-1", 10.0, 9.9);
        Order impossibleOrder = order("ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(impossibleOrder));

        assertTrue(plan.trips().isEmpty());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(impossibleOrder, plan.unallocatedOrders().get(0).order());
        assertEquals("order exceeds max drone range", plan.unallocatedOrders().get(0).reason());
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenObstacleAdjustedRangeExceedsAllDroneRanges() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.3);
        Order impossibleOrder = order("ORDER-1", 10.0, 0.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(
                List.of(drone),
                List.of(impossibleOrder),
                true,
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        assertTrue(plan.trips().isEmpty());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(impossibleOrder, plan.unallocatedOrders().get(0).order());
        assertEquals("order exceeds max drone range", plan.unallocatedOrders().get(0).reason());
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenBatteryIsInsufficientForSafeReturn() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0, 29.9, 1.0, 20.0, 1.0, 10.0);
        Order impossibleOrder = order("ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(impossibleOrder));

        assertTrue(plan.trips().isEmpty());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(impossibleOrder, plan.unallocatedOrders().get(0).order());
        assertEquals(
                "order exceeds drone battery for complete trip and safe return",
                plan.unallocatedOrders().get(0).reason()
        );
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenWeightAndRangeExceedAllDroneLimits() {
        Drone drone = new Drone("DRONE-1", 10.0, 9.9);
        Order impossibleOrder = order("ORDER-1", 3.0, 4.0, 10.1, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(impossibleOrder));

        assertTrue(plan.trips().isEmpty());
        assertEquals(1, plan.unallocatedOrders().size());
        assertEquals(impossibleOrder, plan.unallocatedOrders().get(0).order());
        assertEquals(
                "order exceeds max drone weight capacity and max drone range",
                plan.unallocatedOrders().get(0).reason()
        );
    }

    @Test
    void shouldOrderPlannedDeliveriesByPriorityWeightAndDistance() {
        Drone drone = new Drone("DRONE-1", 30.0, 100.0);
        Order lowPriorityOrder = order("ORDER-LOW", 1.0, 0.0, 9.0, Priority.LOW);
        Order mediumPriorityOrder = order("ORDER-MEDIUM", 2.0, 0.0, 9.0, Priority.MEDIUM);
        Order highLightOrder = order("ORDER-HIGH-LIGHT", 10.0, 0.0, 1.0, Priority.HIGH);
        Order highHeavyFarOrder = order("ORDER-HIGH-HEAVY-FAR", 8.0, 0.0, 5.0, Priority.HIGH);
        Order highHeavyNearOrder = order("ORDER-HIGH-HEAVY-NEAR", 3.0, 0.0, 5.0, Priority.HIGH);

        TripPlan plan = planner.plan(
                List.of(drone),
                List.of(lowPriorityOrder, mediumPriorityOrder, highLightOrder, highHeavyFarOrder, highHeavyNearOrder)
        );

        assertEquals(1, plan.trips().size());
        assertEquals(
                List.of(highHeavyNearOrder, highHeavyFarOrder, highLightOrder, mediumPriorityOrder, lowPriorityOrder),
                plan.trips().get(0).route()
        );
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldPrioritizeConfirmedDeliveryTimeBeforePriority() {
        Drone drone = new Drone("DRONE-1", 20.0, 100.0);
        Order laterHighPriorityOrder = order(
                "ORDER-HIGH-LATER",
                1.0,
                0.0,
                1.0,
                Priority.HIGH,
                Instant.parse("2026-07-26T18:00:00Z")
        );
        Order earlierLowPriorityOrder = order(
                "ORDER-LOW-EARLIER",
                2.0,
                0.0,
                1.0,
                Priority.LOW,
                Instant.parse("2026-07-26T17:00:00Z")
        );

        TripPlan plan = planner.plan(List.of(drone), List.of(laterHighPriorityOrder, earlierLowPriorityOrder));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(earlierLowPriorityOrder, laterHighPriorityOrder), plan.trips().get(0).route());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldAllocateOrderExactlyAtCapacityAndRangeLimits() {
        Drone drone = new Drone("DRONE-1", 10.0, 10.0);
        Order order = order("ORDER-1", 3.0, 4.0, 10.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(order));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(order), plan.trips().get(0).orders());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    @Test
    void shouldProduceDeterministicResultForOrdersWithSamePriority() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);
        Order secondOrder = order("ORDER-B", -1.0, 0.0, 1.0, Priority.HIGH);
        Order firstOrder = order("ORDER-A", 1.0, 0.0, 1.0, Priority.HIGH);

        TripPlan plan = planner.plan(List.of(drone), List.of(secondOrder, firstOrder));

        assertEquals(1, plan.trips().size());
        assertEquals(List.of(firstOrder, secondOrder), plan.trips().get(0).orders());
        assertTrue(plan.unallocatedOrders().isEmpty());
    }

    private Order order(String identifier, double x, double y, double weight, Priority priority) {
        return new Order(identifier, new Coordinate(x, y), weight, priority);
    }

    private Order order(
            String identifier,
            double x,
            double y,
            double weight,
            Priority priority,
            Instant confirmedDeliveryTime
    ) {
        return new Order(identifier, new Coordinate(x, y), weight, priority, confirmedDeliveryTime);
    }

    private List<Order> plannedOrders(TripPlan plan) {
        return plan.trips().stream()
                .flatMap(trip -> trip.orders().stream())
                .toList();
    }
}
