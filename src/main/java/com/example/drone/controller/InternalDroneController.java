package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/drones")
public class InternalDroneController {

    private final DroneQueryService queryService;

    public InternalDroneController(DroneQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}/battery")
    public DroneBatteryResponse findBatteryByDroneId(@PathVariable Long id) {
        DroneEntity drone = queryService.findById(id);

        return new DroneBatteryResponse(
                drone.getId(),
                drone.getIdentifier(),
                drone.getStatus(),
                drone.getBatteryLevel(),
                drone.getBatteryConsumptionPerDistanceUnit(),
                drone.getMinimumReturnBattery(),
                drone.getChargingRate()
        );
    }

    public record DroneBatteryResponse(
            Long id,
            String identifier,
            DroneStatus status,
            double batteryLevel,
            double batteryConsumptionPerDistanceUnit,
            double minimumReturnBattery,
            double chargingRate
    ) {
    }
}
