package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewRegistrationService {

    private final ReviewStorage storage;

    public ReviewRegistrationService(ReviewStorage storage) {
        this.storage = storage;
    }

    @Transactional
    public ReviewEntity register(int stars, String title, String feedback) {
        Review review = new Review(stars, title, feedback);
        ReviewEntity entity = new ReviewEntity(
                null,
                review.stars(),
                review.title(),
                review.feedback()
        );

        return storage.save(entity);
    }
}
