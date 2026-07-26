package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusTest {

    @Test
    void shouldContainAllSupportedOrderStatuses() {
        assertArrayEquals(
                new OrderStatus[]{
                        OrderStatus.REQUESTED,
                        OrderStatus.ALLOCATED,
                        OrderStatus.IN_ROUTE,
                        OrderStatus.PENDING_REASSIGNMENT,
                        OrderStatus.DELIVERED,
                        OrderStatus.NOT_DELIVERED,
                        OrderStatus.CANCELLED,
                        OrderStatus.UNALLOCATED
                },
                OrderStatus.values()
        );
    }

    @Test
    void shouldRepresentOrderStatusByName() {
        assertEquals("REQUESTED", OrderStatus.REQUESTED.name());
        assertEquals("ALLOCATED", OrderStatus.ALLOCATED.name());
        assertEquals("IN_ROUTE", OrderStatus.IN_ROUTE.name());
        assertEquals("PENDING_REASSIGNMENT", OrderStatus.PENDING_REASSIGNMENT.name());
        assertEquals("DELIVERED", OrderStatus.DELIVERED.name());
        assertEquals("NOT_DELIVERED", OrderStatus.NOT_DELIVERED.name());
        assertEquals("CANCELLED", OrderStatus.CANCELLED.name());
        assertEquals("UNALLOCATED", OrderStatus.UNALLOCATED.name());
    }
}
