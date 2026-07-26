package com.example.drone.domain;

import com.example.drone.exception.*;

public enum TripStatus {
    PLANNED,
    IN_ROUTE,
    RETURNED_EARLY,
    COMPLETED,
    CANCELLED
}
