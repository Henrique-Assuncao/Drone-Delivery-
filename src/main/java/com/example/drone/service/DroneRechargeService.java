package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DroneRechargeService {

    public static final String MANUAL_RECHARGE_REASON = "manual recharge requested";
    public static final String INSUFFICIENT_BATTERY_REASON =
            "drone battery is insufficient for requested orders";

    private final DroneStorage storage;

    public DroneRechargeService(DroneStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public DroneEntity enqueue(Long id) {
        DroneEntity drone = findById(id);

        if (drone.getStatus() != DroneStatus.AVAILABLE) {
            throw new InvalidInputException("drone must be AVAILABLE to enter recharge queue");
        }

        if (drone.getBatteryLevel() >= Drone.DEFAULT_BATTERY_LEVEL) {
            throw new InvalidInputException("drone battery must be below 100 to enter recharge queue");
        }

        drone.enqueueForRecharge(MANUAL_RECHARGE_REASON);

        return drone;
    }

    @Transactional
    public DroneEntity complete(Long id) {
        DroneEntity drone = findById(id);

        if (drone.getStatus() != DroneStatus.CHARGING) {
            throw new InvalidInputException("drone must be CHARGING to complete recharge");
        }

        drone.completeRecharge();

        return drone;
    }

    public List<DroneEntity> findQueue() {
        return storage.findRechargeQueue();
    }

    private DroneEntity findById(Long id) {
        return storage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("drone not found"));
    }
}
