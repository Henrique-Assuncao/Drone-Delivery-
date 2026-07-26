package com.example.drone.domain;

import com.example.drone.exception.*;

public record Review(int stars, String title, String feedback) {

    public Review {
        if (stars < 1 || stars > 5) {
            throw new InvalidInputException("stars must be between 1 and 5");
        }

        if (title == null || title.isBlank()) {
            throw new InvalidInputException("title must not be blank");
        }

        if (feedback == null || feedback.isBlank()) {
            throw new InvalidInputException("feedback must not be blank");
        }
    }
}
