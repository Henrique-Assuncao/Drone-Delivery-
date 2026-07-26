package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DroneRemovalService {

    private final DroneStorage droneStorage;
    private final TripStorage tripStorage;

    public DroneRemovalService(DroneStorage droneStorage, TripStorage tripStorage) {
        this.droneStorage = droneStorage;
        this.tripStorage = tripStorage;
    }

    @Transactional
    public DroneEntity remove(Long id) {
        DroneEntity drone = droneStorage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("drone not found"));

        if (drone.getStatus() == DroneStatus.IN_ROUTE) {
            throw new InvalidInputException("drone must not be IN_ROUTE to delete");
        }

        if (tripStorage.existsByDroneId(id)) {
            throw new InvalidInputException("drone with trips cannot be deleted");
        }

        droneStorage.delete(drone);

        return drone;
    }
}
