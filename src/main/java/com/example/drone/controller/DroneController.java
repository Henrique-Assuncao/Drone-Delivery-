package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/drones")
public class DroneController {

    private final DroneRegistrationService registrationService;
    private final DroneQueryService queryService;
    private final DroneAvailabilityService availabilityService;
    private final DroneRechargeService rechargeService;
    private final DroneRemovalService removalService;

    public DroneController(
            DroneRegistrationService registrationService,
            DroneQueryService queryService,
            DroneAvailabilityService availabilityService,
            DroneRechargeService rechargeService,
            DroneRemovalService removalService
    ) {
        this.registrationService = registrationService;
        this.queryService = queryService;
        this.availabilityService = availabilityService;
        this.rechargeService = rechargeService;
        this.removalService = removalService;
    }

    @PostMapping
    public ResponseEntity<DroneResponse> create(@RequestBody CreateDroneRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        DroneEntity drone = registrationService.register(
                request.identifier(),
                request.maxWeightCapacity(),
                request.maxRange(),
                request.batteryLevelOrDefault(),
                request.batteryConsumptionPerDistanceUnitOrDefault(),
                request.minimumReturnBatteryOrDefault(),
                request.speedOrDefault(),
                request.chargingRateOrDefault()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(drone));
    }

    @GetMapping
    public List<DroneResponse> listAll(@RequestParam(required = false) DroneStatus status) {
        List<DroneEntity> drones = status == null
                ? queryService.findAll()
                : queryService.findByStatus(status);

        return drones.stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/available")
    public List<DroneResponse> listAvailable() {
        return queryService.findAvailable().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public DroneResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findById(id));
    }

    @PostMapping("/{id}/unavailable")
    public DroneResponse markUnavailable(@PathVariable Long id) {
        return toResponse(availabilityService.markUnavailable(id));
    }

    @PostMapping("/{id}/available")
    public DroneResponse markAvailable(@PathVariable Long id) {
        return toResponse(availabilityService.markAvailable(id));
    }

    @PostMapping("/{id}/recharge")
    public DroneResponse enqueueForRecharge(@PathVariable Long id) {
        return toResponse(rechargeService.enqueue(id));
    }

    @PostMapping("/{id}/recharge/complete")
    public DroneResponse completeRecharge(@PathVariable Long id) {
        return toResponse(rechargeService.complete(id));
    }

    @DeleteMapping("/{id}")
    public DroneResponse delete(@PathVariable Long id) {
        return toResponse(removalService.remove(id));
    }

    private DroneResponse toResponse(DroneEntity drone) {
        return new DroneResponse(
                drone.getId(),
                drone.getIdentifier(),
                drone.getMaxWeightCapacity(),
                drone.getMaxRange(),
                drone.getStatus(),
                drone.getBatteryLevel(),
                drone.getBatteryConsumptionPerDistanceUnit(),
                drone.getMinimumReturnBattery(),
                drone.getSpeed(),
                drone.getChargingRate(),
                drone.getRechargeQueuedAt(),
                drone.getRechargeReason()
        );
    }

    public record CreateDroneRequest(
            String identifier,
            double maxWeightCapacity,
            double maxRange,
            Double batteryLevel,
            Double batteryConsumptionPerDistanceUnit,
            Double minimumReturnBattery,
            Double speed,
            Double chargingRate
    ) {

        double batteryLevelOrDefault() {
            return batteryLevel == null ? Drone.DEFAULT_BATTERY_LEVEL : batteryLevel;
        }

        double batteryConsumptionPerDistanceUnitOrDefault() {
            return batteryConsumptionPerDistanceUnit == null
                    ? Drone.DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT
                    : batteryConsumptionPerDistanceUnit;
        }

        double minimumReturnBatteryOrDefault() {
            return minimumReturnBattery == null ? Drone.DEFAULT_MINIMUM_RETURN_BATTERY : minimumReturnBattery;
        }

        double speedOrDefault() {
            return speed == null ? Drone.DEFAULT_SPEED : speed;
        }

        double chargingRateOrDefault() {
            return chargingRate == null ? Drone.DEFAULT_CHARGING_RATE : chargingRate;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DroneResponse(
            Long id,
            String identifier,
            double maxWeightCapacity,
            double maxRange,
            DroneStatus status,
            double batteryLevel,
            double batteryConsumptionPerDistanceUnit,
            double minimumReturnBattery,
            double speed,
            double chargingRate,
            Instant rechargeQueuedAt,
            String rechargeReason
    ) {
    }
}
