package com.example.drone.domain;

import com.example.drone.exception.*;

public final class MeasurementUnits {

    private static final double MINUTES_PER_HOUR = 60.0;

    private MeasurementUnits() {
    }

    public static double minutesForDistance(double distanceKilometers, double speedKilometersPerHour) {
        if (distanceKilometers < 0) {
            throw new InvalidInputException("distance must not be negative");
        }

        if (speedKilometersPerHour <= 0) {
            throw new InvalidInputException("speed must be greater than zero");
        }

        return distanceKilometers / speedKilometersPerHour * MINUTES_PER_HOUR;
    }

    public static double distanceForMinutes(double minutes, double speedKilometersPerHour) {
        if (minutes < 0) {
            throw new InvalidInputException("minutes must not be negative");
        }

        if (speedKilometersPerHour <= 0) {
            throw new InvalidInputException("speed must be greater than zero");
        }

        return speedKilometersPerHour * minutes / MINUTES_PER_HOUR;
    }
}
