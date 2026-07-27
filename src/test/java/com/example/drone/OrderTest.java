package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void shouldCreateOrderWithRequiredFields() {
        Coordinate location = new Coordinate(3.0, 4.0);

        Order order = new Order("ORDER-1", location, 2.5, Priority.HIGH);

        assertEquals("ORDER-1", order.identifier());
        assertEquals(location, order.location());
        assertEquals(2.5, order.weight());
        assertEquals(Priority.HIGH, order.priority());
        assertEquals(Instant.MAX, order.confirmedDeliveryTime());
    }

    @Test
    void shouldBeImmutable() {
        boolean allFieldsAreFinal = Arrays.stream(Order.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers()));

        boolean hasSetter = Arrays.stream(Order.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().startsWith("set"));

        assertTrue(allFieldsAreFinal);
        assertFalse(hasSetter);
    }

    @Test
    void shouldRejectNullIdentifier() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order(null, new Coordinate(1.0, 1.0), 1.0, Priority.LOW)
        );

        assertEquals("identifier must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankIdentifier() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order(" ", new Coordinate(1.0, 1.0), 1.0, Priority.LOW)
        );

        assertEquals("identifier must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectNullLocation() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order("ORDER-1", null, 1.0, Priority.LOW)
        );

        assertEquals("location must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectZeroWeight() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order("ORDER-1", new Coordinate(1.0, 1.0), 0.0, Priority.LOW)
        );

        assertEquals("weight must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldRejectNegativeWeight() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order("ORDER-1", new Coordinate(1.0, 1.0), -1.0, Priority.LOW)
        );

        assertEquals("weight must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldRejectNullPriority() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order("ORDER-1", new Coordinate(1.0, 1.0), 1.0, null)
        );

        assertEquals("priority must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullConfirmedDeliveryTime() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order("ORDER-1", new Coordinate(1.0, 1.0), 1.0, Priority.LOW, null)
        );

        assertEquals("confirmedDeliveryTime must not be null", exception.getMessage());
    }
}
