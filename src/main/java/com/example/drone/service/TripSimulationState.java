package com.example.drone.service;

import com.example.drone.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TripSimulationState(
        Long tripId,
        Long droneId,
        TripStatus status,
        @Schema(description = "Posição atual do drone em coordenadas X/Y em quilômetros (km) a partir da base.")
        Coordinate currentLocation,
        @Schema(description = "Distância já percorrida em quilômetros (km).", example = "5.0")
        double travelledDistance,
        @Schema(description = "Distância total da viagem em quilômetros (km).", example = "20.0")
        double totalDistance,
        double progress,
        Long nextOrderId,
        Integer nextRoutePosition,
        boolean moving,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant updatedAt
) {
}
