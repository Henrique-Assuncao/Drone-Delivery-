package com.example.drone.domain;

import com.example.drone.exception.*;

public record Obstacle(Coordinate center, double radius, boolean active) {

    public Obstacle {
        if (center == null) {
            throw new InvalidInputException("center must not be null");
        }

        if (radius <= 0) {
            throw new InvalidInputException("radius must be greater than zero");
        }
    }

    public Obstacle(Coordinate center, double radius) {
        this(center, radius, true);
    }
}
