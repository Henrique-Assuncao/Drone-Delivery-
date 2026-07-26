package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DroneAvailabilityService {

    private final DroneStorage storage;

    public DroneAvailabilityService(DroneStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public DroneEntity markUnavailable(Long id) {
        DroneEntity drone = findById(id);

        if (drone.getStatus() != DroneStatus.AVAILABLE) {
            throw new InvalidInputException("drone must be AVAILABLE to mark unavailable");
        }

        drone.changeStatus(DroneStatus.UNAVAILABLE);

        return drone;
    }

    @Transactional
    public DroneEntity markAvailable(Long id) {
        DroneEntity drone = findById(id);

        if (drone.getStatus() != DroneStatus.UNAVAILABLE) {
            throw new InvalidInputException("drone must be UNAVAILABLE to mark available");
        }

        drone.changeStatus(DroneStatus.AVAILABLE);

        return drone;
    }

    private DroneEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("drone not found"));
    }
}
