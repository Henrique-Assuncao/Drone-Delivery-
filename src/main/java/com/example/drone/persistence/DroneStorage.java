package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import java.util.List;
import java.util.Optional;

public interface DroneStorage {

    boolean existsByIdentifier(String identifier);

    List<DroneEntity> findAll();

    Optional<DroneEntity> findById(Long id);

    List<DroneEntity> findByStatus(DroneStatus status);

    List<DroneEntity> findRechargeQueue();

    DroneEntity save(DroneEntity drone);

    default void delete(DroneEntity drone) {
        throw new UnsupportedOperationException("delete not implemented");
    }
}
