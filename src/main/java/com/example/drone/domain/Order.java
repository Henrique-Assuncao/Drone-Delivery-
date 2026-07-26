package com.example.drone.domain;

import com.example.drone.exception.*;

public record Order(String identifier, Coordinate location, double weight, Priority priority) {

    public Order {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidInputException("identifier must not be blank");
        }

        if (location == null) {
            throw new InvalidInputException("location must not be null");
        }

        if (weight <= 0) {
            throw new InvalidInputException("weight must be greater than zero");
        }

        if (priority == null) {
            throw new InvalidInputException("priority must not be null");
        }
    }
}
