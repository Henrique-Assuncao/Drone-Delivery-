package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DroneRegistrationService {

    private final DroneStorage storage;

    public DroneRegistrationService(DroneStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public DroneEntity register(String identifier, double maxWeightCapacity, double maxRange) {
        return register(
                identifier,
                maxWeightCapacity,
                maxRange,
                Drone.DEFAULT_BATTERY_LEVEL,
                Drone.DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT,
                Drone.DEFAULT_MINIMUM_RETURN_BATTERY,
                Drone.DEFAULT_SPEED,
                Drone.DEFAULT_CHARGING_RATE
        );
    }

    @Transactional
    public DroneEntity register(
            String identifier,
            double maxWeightCapacity,
            double maxRange,
            double batteryLevel,
            double batteryConsumptionPerDistanceUnit,
            double minimumReturnBattery,
            double speed,
            double chargingRate
    ) {
        Drone drone = new Drone(
                identifier,
                maxWeightCapacity,
                maxRange,
                batteryLevel,
                batteryConsumptionPerDistanceUnit,
                minimumReturnBattery,
                speed,
                chargingRate
        );

        if (storage.existsByIdentifier(drone.identifier())) {
            throw new DuplicateResourceException("drone identifier already exists");
        }

        DroneEntity entity = new DroneEntity(
                null,
                drone.identifier(),
                drone.maxWeightCapacity(),
                drone.maxRange(),
                DroneStatus.AVAILABLE,
                drone.batteryLevel(),
                drone.batteryConsumptionPerDistanceUnit(),
                drone.minimumReturnBattery(),
                drone.speed(),
                drone.chargingRate()
        );

        return storage.save(entity);
    }
}
