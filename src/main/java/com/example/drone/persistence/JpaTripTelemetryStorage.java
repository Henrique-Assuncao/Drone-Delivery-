package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTripTelemetryStorage implements TripTelemetryStorage {

    private final TripTelemetryJpaRepository repository;

    public JpaTripTelemetryStorage(TripTelemetryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TripTelemetryEntity> findByTripId(Long tripId) {
        return repository.findByTripIdOrderByReportedAtAscIdAsc(tripId);
    }

    @Override
    public TripTelemetryEntity save(TripTelemetryEntity telemetry) {
        return repository.save(telemetry);
    }
}
