package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleOrderDeliveryEligibilityTest {

    private final SingleOrderDeliveryEligibility eligibility = new SingleOrderDeliveryEligibility();

    @Test
    void shouldAllowServiceableOrder() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order order = new Order("ORDER-1", new Coordinate(3.0, 4.0), 5.0, Priority.HIGH);

        assertTrue(eligibility.canServe(drone, order));
    }

    @Test
    void shouldRejectOrderAboveWeightCapacity() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order order = new Order("ORDER-1", new Coordinate(1.0, 1.0), 10.1, Priority.HIGH);

        assertFalse(eligibility.canServe(drone, order));
    }

    @Test
    void shouldRejectOrderAboveRange() {
        Drone drone = new Drone("DRONE-1", 10.0, 9.9);
        Order order = new Order("ORDER-1", new Coordinate(3.0, 4.0), 5.0, Priority.HIGH);

        assertFalse(eligibility.canServe(drone, order));
    }

    @Test
    void shouldAllowOrderExactlyAtWeightLimit() {
        Drone drone = new Drone("DRONE-1", 10.0, 20.0);
        Order order = new Order("ORDER-1", new Coordinate(1.0, 1.0), 10.0, Priority.HIGH);

        assertTrue(eligibility.canServe(drone, order));
    }

    @Test
    void shouldAllowOrderExactlyAtRangeLimit() {
        Drone drone = new Drone("DRONE-1", 10.0, 10.0);
        Order order = new Order("ORDER-1", new Coordinate(3.0, 4.0), 5.0, Priority.HIGH);

        assertTrue(eligibility.canServe(drone, order));
    }

    @Test
    void shouldRejectNullDrone() {
        Order order = new Order("ORDER-1", new Coordinate(3.0, 4.0), 5.0, Priority.HIGH);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> eligibility.canServe(null, order)
        );

        assertEquals("drone must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullOrder() {
        Drone drone = new Drone("DRONE-1", 10.0, 10.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> eligibility.canServe(drone, null)
        );

        assertEquals("order must not be null", exception.getMessage());
    }
}
