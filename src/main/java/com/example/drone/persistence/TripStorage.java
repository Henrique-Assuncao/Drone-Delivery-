package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;
import java.util.Optional;

public interface TripStorage {

    List<TripEntity> findAll();

    List<TripEntity> findByStatus(TripStatus status);

    Optional<TripEntity> findById(Long id);

    TripEntity save(TripEntity trip);

    default boolean existsByDroneId(Long droneId) {
        return false;
    }
}
