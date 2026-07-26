package com.example.drone.domain;

import com.example.drone.exception.*;

public enum OrderStatus {
    REQUESTED,
    ALLOCATED,
    IN_ROUTE,
    PENDING_REASSIGNMENT,
    DELIVERED,
    CANCELLED,
    UNALLOCATED
}
