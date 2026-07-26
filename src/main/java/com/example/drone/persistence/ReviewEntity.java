package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String feedback;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ReviewEntity() {
    }

    public ReviewEntity(Long id, int stars, String title, String feedback) {
        this(id, stars, title, feedback, Instant.now());
    }

    public ReviewEntity(Long id, int stars, String title, String feedback, Instant reviewedAt) {
        new Review(stars, title, feedback);

        if (reviewedAt == null) {
            throw new InvalidInputException("reviewedAt must not be null");
        }

        this.id = id;
        this.stars = stars;
        this.title = title;
        this.feedback = feedback;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public int getStars() {
        return stars;
    }

    public String getTitle() {
        return title;
    }

    public String getFeedback() {
        return feedback;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
