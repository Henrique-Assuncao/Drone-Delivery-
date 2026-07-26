package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TripTelemetryJpaRepository extends JpaRepository<TripTelemetryEntity, Long> {

    @EntityGraph(attributePaths = "trip")
    List<TripTelemetryEntity> findByTripIdOrderByReportedAtAscIdAsc(Long tripId);
}
