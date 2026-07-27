package com.example.drone;

import com.example.drone.domain.DroneStatus;
import com.example.drone.domain.OrderStatus;
import com.example.drone.domain.Priority;
import com.example.drone.domain.TripStatus;
import com.example.drone.persistence.DroneEntity;
import com.example.drone.persistence.OrderEntity;
import com.example.drone.persistence.TripEntity;
import com.example.drone.service.TripDispatchPolicy;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripDispatchPolicyTest {

    private static final Instant QUEUED_AT = Instant.parse("2026-07-26T10:00:00Z");
    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    @Test
    void shouldCalculateIdealDispatchTimeFromEarliestDeliveryDeadline() {
        TripEntity trip = plannedTrip();
        trip.addOrder(order(1L, "2026-07-26T13:00:00Z"), 0, null, 30.0);
        trip.addOrder(order(2L, "2026-07-26T12:50:00Z"), 1, null, 10.0);

        assertEquals(
                Instant.parse("2026-07-26T12:30:00Z"),
                TripDispatchPolicy.idealDispatchTimeFor(trip).orElseThrow()
        );
    }

    @Test
    void shouldIgnoreResolvedOrdersWhenCalculatingIdealDispatchTime() {
        TripEntity trip = plannedTrip();
        trip.addOrder(order(1L, "2026-07-26T12:15:00Z"), 0, Instant.parse("2026-07-26T11:50:00Z"), 30.0);
        trip.addOrder(order(2L, "2026-07-26T13:00:00Z"), 1, null, 30.0);

        assertEquals(
                Instant.parse("2026-07-26T12:30:00Z"),
                TripDispatchPolicy.idealDispatchTimeFor(trip).orElseThrow()
        );
    }

    @Test
    void shouldOpenDispatchWindowOnlyAtIdealTime() {
        TripEntity trip = plannedTrip();
        trip.addOrder(order(1L, "2026-07-26T13:00:00Z"), 0, null, 30.0);

        assertFalse(TripDispatchPolicy.isDispatchWindowOpen(trip, Instant.parse("2026-07-26T12:29:59Z")));
        assertTrue(TripDispatchPolicy.isDispatchWindowOpen(trip, Instant.parse("2026-07-26T12:30:00Z")));
        assertTrue(TripDispatchPolicy.isDispatchWindowOpen(trip, Instant.parse("2026-07-26T12:31:00Z")));
    }

    @Test
    void shouldCalculateRemainingMinutesUntilIdealDispatch() {
        TripEntity trip = plannedTrip();
        trip.addOrder(order(1L, "2026-07-26T13:00:00Z"), 0, null, 30.0);

        assertEquals(
                15.5,
                TripDispatchPolicy.minutesUntilIdealDispatch(trip, Instant.parse("2026-07-26T12:14:30Z")),
                DECIMAL_TOLERANCE
        );
        assertEquals(
                0.0,
                TripDispatchPolicy.minutesUntilIdealDispatch(trip, Instant.parse("2026-07-26T12:30:00Z")),
                DECIMAL_TOLERANCE
        );
    }

    @Test
    void shouldTreatTripWithoutPendingOrdersAsReadyToDispatch() {
        TripEntity trip = plannedTrip();
        trip.addOrder(order(1L, "2026-07-26T13:00:00Z"), 0, Instant.parse("2026-07-26T12:40:00Z"), 30.0);

        assertTrue(TripDispatchPolicy.idealDispatchTimeFor(trip).isEmpty());
        assertTrue(TripDispatchPolicy.isDispatchWindowOpen(trip, Instant.parse("2026-07-26T12:00:00Z")));
        assertEquals(
                0.0,
                TripDispatchPolicy.minutesUntilIdealDispatch(trip, Instant.parse("2026-07-26T12:00:00Z")),
                DECIMAL_TOLERANCE
        );
    }

    private static TripEntity plannedTrip() {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 20.0, 200.0, DroneStatus.AVAILABLE);
        return new TripEntity(1L, drone, TripStatus.PLANNED, 0.0, 0.0);
    }

    private static OrderEntity order(Long id, String confirmedDeliveryTime) {
        String identifier = "ORDER-" + id;
        return new OrderEntity(
                id,
                identifier,
                1.0,
                1.0,
                1.0,
                Priority.MEDIUM,
                OrderStatus.ALLOCATED,
                QUEUED_AT,
                identifier,
                Instant.parse(confirmedDeliveryTime)
        );
    }
}
