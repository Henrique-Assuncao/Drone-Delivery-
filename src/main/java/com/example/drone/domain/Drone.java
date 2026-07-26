package com.example.drone.domain;

import com.example.drone.exception.*;

public record Drone(
        String identifier,
        double maxWeightCapacity,
        double maxRange,
        double batteryLevel,
        double batteryConsumptionPerDistanceUnit,
        double minimumReturnBattery,
        double speed,
        double chargingRate
) {

    public static final double DEFAULT_BATTERY_LEVEL = 100.0;
    public static final double DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT = 1.0;
    public static final double DEFAULT_MINIMUM_RETURN_BATTERY = 20.0;
    public static final double DEFAULT_SPEED = 1.0;
    public static final double DEFAULT_CHARGING_RATE = 10.0;

    public Drone(String identifier, double maxWeightCapacity, double maxRange) {
        this(
                identifier,
                maxWeightCapacity,
                maxRange,
                DEFAULT_BATTERY_LEVEL,
                DEFAULT_BATTERY_CONSUMPTION_PER_DISTANCE_UNIT,
                DEFAULT_MINIMUM_RETURN_BATTERY,
                DEFAULT_SPEED,
                DEFAULT_CHARGING_RATE
        );
    }

    public Drone {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidInputException("identifier must not be blank");
        }

        if (maxWeightCapacity <= 0) {
            throw new InvalidInputException("maxWeightCapacity must be greater than zero");
        }

        if (maxRange <= 0) {
            throw new InvalidInputException("maxRange must be greater than zero");
        }

        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new InvalidInputException("batteryLevel must be between 0 and 100");
        }

        if (batteryConsumptionPerDistanceUnit <= 0) {
            throw new InvalidInputException("batteryConsumptionPerDistanceUnit must be greater than zero");
        }

        if (minimumReturnBattery < 0 || minimumReturnBattery > 100) {
            throw new InvalidInputException("minimumReturnBattery must be between 0 and 100");
        }

        if (speed <= 0) {
            throw new InvalidInputException("speed must be greater than zero");
        }

        if (chargingRate <= 0) {
            throw new InvalidInputException("chargingRate must be greater than zero");
        }
    }

    public boolean supportsWeightOf(Order order) {
        if (order == null) {
            throw new InvalidInputException("order must not be null");
        }

        return order.weight() <= maxWeightCapacity;
    }

    public double batteryRequiredForDistance(double distance) {
        if (distance < 0) {
            throw new InvalidInputException("distance must not be negative");
        }

        return distance * batteryConsumptionPerDistanceUnit + minimumReturnBattery;
    }

    public boolean canCompleteTripWithSafeReturn(double distance) {
        return batteryRequiredForDistance(distance) <= batteryLevel;
    }
}
