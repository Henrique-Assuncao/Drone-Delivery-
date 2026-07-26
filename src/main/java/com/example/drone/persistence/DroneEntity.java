package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "drones")
public class DroneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(name = "max_weight_capacity", nullable = false)
    private double maxWeightCapacity;

    @Column(name = "max_range", nullable = false)
    private double maxRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DroneStatus status;

    @Column(name = "battery_level", nullable = false)
    private double batteryLevel = Drone.DEFAULT_BATTERY_LEVEL;

    @Column(name = "battery_consumption_per_distance_unit", nullable = false)
    private double batteryConsumptionPerDistanceUnit = Drone.DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT;

    @Column(name = "minimum_return_battery", nullable = false)
    private double minimumReturnBattery = Drone.DEFAULT_MINIMUM_RETURN_BATTERY;

    @Column(nullable = false)
    private double speed = Drone.DEFAULT_SPEED;

    @Column(name = "charging_rate", nullable = false)
    private double chargingRate = Drone.DEFAULT_CHARGING_RATE;

    @Column(name = "recharge_queued_at")
    private Instant rechargeQueuedAt;

    @Column(name = "recharge_reason")
    private String rechargeReason;

    protected DroneEntity() {
    }

    public DroneEntity(
            Long id,
            String identifier,
            double maxWeightCapacity,
            double maxRange,
            DroneStatus status
    ) {
        this(
                id,
                identifier,
                maxWeightCapacity,
                maxRange,
                status,
                Drone.DEFAULT_BATTERY_LEVEL,
                Drone.DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT,
                Drone.DEFAULT_MINIMUM_RETURN_BATTERY,
                Drone.DEFAULT_SPEED,
                Drone.DEFAULT_CHARGING_RATE
        );
    }

    public DroneEntity(
            Long id,
            String identifier,
            double maxWeightCapacity,
            double maxRange,
            DroneStatus status,
            double batteryLevel,
            double batteryConsumptionPerDistanceUnit,
            double minimumReturnBattery,
            double speed,
            double chargingRate
    ) {
        this.id = id;
        this.identifier = identifier;
        this.maxWeightCapacity = maxWeightCapacity;
        this.maxRange = maxRange;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.batteryConsumptionPerDistanceUnit = batteryConsumptionPerDistanceUnit;
        this.minimumReturnBattery = minimumReturnBattery;
        this.speed = speed;
        this.chargingRate = chargingRate;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getMaxWeightCapacity() {
        return maxWeightCapacity;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public double getBatteryConsumptionPerDistanceUnit() {
        return batteryConsumptionPerDistanceUnit;
    }

    public double getMinimumReturnBattery() {
        return minimumReturnBattery;
    }

    public double getSpeed() {
        return speed;
    }

    public double getChargingRate() {
        return chargingRate;
    }

    public Instant getRechargeQueuedAt() {
        return rechargeQueuedAt;
    }

    public String getRechargeReason() {
        return rechargeReason;
    }

    public void changeStatus(DroneStatus status) {
        if (status == null) {
            throw new InvalidInputException("status must not be null");
        }

        this.status = status;
    }

    public void consumeBatteryForDistance(double distance) {
        if (distance < 0) {
            throw new InvalidInputException("distance must not be negative");
        }

        this.batteryLevel = Math.max(0.0, batteryLevel - distance * batteryConsumptionPerDistanceUnit);
    }

    public void updateBatteryLevel(double batteryLevel) {
        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new InvalidInputException("batteryLevel must be between 0 and 100");
        }

        this.batteryLevel = batteryLevel;
    }

    public void enqueueForRecharge(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("recharge reason must not be blank");
        }

        this.status = DroneStatus.CHARGING;
        this.rechargeQueuedAt = Instant.now();
        this.rechargeReason = reason;
    }

    public void completeRecharge() {
        this.status = DroneStatus.AVAILABLE;
        this.batteryLevel = Drone.DEFAULT_BATTERY_LEVEL;
        this.rechargeQueuedAt = null;
        this.rechargeReason = null;
    }
}
