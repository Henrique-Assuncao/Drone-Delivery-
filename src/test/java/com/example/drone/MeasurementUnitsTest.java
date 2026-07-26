package com.example.drone;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeasurementUnitsTest {

    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    @Test
    void shouldCalculateMinutesFromKilometersAndKilometersPerHour() {
        assertEquals(10.0, MeasurementUnits.minutesForDistance(10.0, 60.0), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldCalculateKilometersFromMinutesAndKilometersPerHour() {
        assertEquals(10.0, MeasurementUnits.distanceForMinutes(10.0, 60.0), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldRejectInvalidSpeed() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> MeasurementUnits.minutesForDistance(10.0, 0.0)
        );

        assertEquals("speed must be greater than zero", exception.getMessage());
    }
}
