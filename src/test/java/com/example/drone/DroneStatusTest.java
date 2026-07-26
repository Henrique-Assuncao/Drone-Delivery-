package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DroneStatusTest {

    @Test
    void shouldContainAllSupportedDroneStatuses() {
        assertArrayEquals(
                new DroneStatus[]{
                        DroneStatus.AVAILABLE,
                        DroneStatus.IN_ROUTE,
                        DroneStatus.UNAVAILABLE,
                        DroneStatus.CHARGING
                },
                DroneStatus.values()
        );
    }

    @Test
    void shouldRepresentDroneStatusByName() {
        assertEquals("AVAILABLE", DroneStatus.AVAILABLE.name());
        assertEquals("IN_ROUTE", DroneStatus.IN_ROUTE.name());
        assertEquals("UNAVAILABLE", DroneStatus.UNAVAILABLE.name());
        assertEquals("CHARGING", DroneStatus.CHARGING.name());
    }
}
