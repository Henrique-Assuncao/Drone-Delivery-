package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRegistrationService registrationService;
    private final ReviewQueryService queryService;

    public ReviewController(ReviewRegistrationService registrationService, ReviewQueryService queryService) {
        this.registrationService = registrationService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@RequestBody CreateReviewRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        ReviewEntity review = registrationService.register(request.stars(), request.title(), request.feedback());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(review));
    }

    @GetMapping
    public List<ReviewResponse> list() {
        return queryService.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ReviewResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findById(id));
    }

    private ReviewResponse toResponse(ReviewEntity review) {
        return new ReviewResponse(
                review.getId(),
                review.getStars(),
                review.getTitle(),
                review.getFeedback(),
                review.getReviewedAt()
        );
    }

    public record CreateReviewRequest(int stars, String title, String feedback) {
    }

    public record ReviewResponse(
            Long id,
            int stars,
            String title,
            String feedback,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant reviewedAt
    ) {
    }
}
