package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TripTelemetryService {

    private final TripStorage tripStorage;
    private final TripTelemetryStorage telemetryStorage;

    public TripTelemetryService(TripStorage tripStorage, TripTelemetryStorage telemetryStorage) {
        this.tripStorage = tripStorage;
        this.telemetryStorage = telemetryStorage;
    }

    public TripTelemetryEntity record(TripEntity trip, double batteryLevel) {
        return telemetryStorage.save(new TripTelemetryEntity(null, trip, batteryLevel, Instant.now()));
    }

    public List<TripTelemetryEntity> findByTripId(Long tripId) {
        tripStorage.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("trip not found"));

        return telemetryStorage.findByTripId(tripId);
    }
}
