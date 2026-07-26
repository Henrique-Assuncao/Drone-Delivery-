package com.example.drone.domain;

import com.example.drone.exception.*;

public record Coordinate(double x, double y) {

    public double distanceTo(Coordinate other) {
        if (other == null) {
            throw new InvalidInputException("other coordinate must not be null");
        }

        double deltaX = other.x - x;
        double deltaY = other.y - y;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}
