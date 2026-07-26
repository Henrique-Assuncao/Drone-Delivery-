package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TripStatusTest {

    @Test
    void shouldContainAllSupportedTripStatuses() {
        assertArrayEquals(
                new TripStatus[]{
                        TripStatus.PLANNED,
                        TripStatus.IN_ROUTE,
                        TripStatus.RETURNED_EARLY,
                        TripStatus.COMPLETED,
                        TripStatus.CANCELLED
                },
                TripStatus.values()
        );
    }

    @Test
    void shouldRepresentTripStatusByName() {
        assertEquals("PLANNED", TripStatus.PLANNED.name());
        assertEquals("IN_ROUTE", TripStatus.IN_ROUTE.name());
        assertEquals("RETURNED_EARLY", TripStatus.RETURNED_EARLY.name());
        assertEquals("COMPLETED", TripStatus.COMPLETED.name());
        assertEquals("CANCELLED", TripStatus.CANCELLED.name());
    }
}
