package com.example.drone.service;

import com.example.drone.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record TripSimulationState(
        Long tripId,
        Long droneId,
        TripStatus status,
        Coordinate currentLocation,
        double travelledDistance,
        double totalDistance,
        double progress,
        Long nextOrderId,
        Integer nextRoutePosition,
        boolean moving,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant updatedAt
) {
}
