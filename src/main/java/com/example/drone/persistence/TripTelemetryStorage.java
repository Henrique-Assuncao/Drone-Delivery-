package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;

public interface TripTelemetryStorage {

    List<TripTelemetryEntity> findByTripId(Long tripId);

    TripTelemetryEntity save(TripTelemetryEntity telemetry);
}
