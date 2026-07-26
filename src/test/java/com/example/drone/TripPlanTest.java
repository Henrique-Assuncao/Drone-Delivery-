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

class TripPlanTest {

    @Test
    void shouldRejectNullTrips() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new TripPlan(null, List.of())
        );

        assertEquals("trips must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullUnallocatedOrders() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new TripPlan(List.of(), null)
        );

        assertEquals("unallocatedOrders must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullTripInTrips() {
        List<Trip> trips = new ArrayList<>();
        trips.add(null);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new TripPlan(trips, List.of())
        );

        assertEquals("trips must not contain null", exception.getMessage());
    }

    @Test
    void shouldRejectNullUnallocatedOrderInUnallocatedOrders() {
        List<UnallocatedOrder> unallocatedOrders = new ArrayList<>();
        unallocatedOrders.add(null);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new TripPlan(List.of(), unallocatedOrders)
        );

        assertEquals("unallocatedOrders must not contain null", exception.getMessage());
    }
}
