package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnallocatedOrderTest {

    @Test
    void shouldRejectNullOrder() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new UnallocatedOrder(null, "reason")
        );

        assertEquals("order must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectBlankReason() {
        Order order = new Order("ORDER-1", new Coordinate(1.0, 1.0), 1.0, Priority.HIGH);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new UnallocatedOrder(order, " ")
        );

        assertEquals("reason must not be blank", exception.getMessage());
    }
}
