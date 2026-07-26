package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateTest {

    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    @Test
    void shouldRepresentXAndYValues() {
        Coordinate coordinate = new Coordinate(10.5, -3.25);

        assertEquals(10.5, coordinate.x());
        assertEquals(-3.25, coordinate.y());
    }

    @Test
    void shouldHaveValueEquality() {
        Coordinate first = new Coordinate(2.0, 4.0);
        Coordinate second = new Coordinate(2.0, 4.0);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldBeImmutable() {
        boolean allFieldsAreFinal = Arrays.stream(Coordinate.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers()));

        boolean hasSetter = Arrays.stream(Coordinate.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().startsWith("set"));

        assertTrue(allFieldsAreFinal);
        assertFalse(hasSetter);
    }

    @Test
    void shouldCalculateEuclideanDistanceBetweenTwoCoordinates() {
        Coordinate origin = new Coordinate(0.0, 0.0);
        Coordinate destination = new Coordinate(3.0, 4.0);

        assertEquals(5.0, origin.distanceTo(destination), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldCalculateEuclideanDistanceWithNegativeCoordinates() {
        Coordinate origin = new Coordinate(-1.0, -2.0);
        Coordinate destination = new Coordinate(2.0, 2.0);

        assertEquals(5.0, origin.distanceTo(destination), DECIMAL_TOLERANCE);
    }

    @Test
    void shouldRejectNullCoordinateWhenCalculatingDistance() {
        Coordinate coordinate = new Coordinate(0.0, 0.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> coordinate.distanceTo(null)
        );

        assertEquals("other coordinate must not be null", exception.getMessage());
    }
}
