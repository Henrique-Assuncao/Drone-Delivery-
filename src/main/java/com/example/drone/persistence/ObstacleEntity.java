package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "obstacles")
public class ObstacleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_x", nullable = false)
    private double centerX;

    @Column(name = "center_y", nullable = false)
    private double centerY;

    @Column(nullable = false)
    private double radius;

    @Column(nullable = false)
    private boolean active;

    protected ObstacleEntity() {
    }

    public ObstacleEntity(Long id, double centerX, double centerY, double radius, boolean active) {
        new Obstacle(new Coordinate(centerX, centerY), radius, active);

        this.id = id;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isActive() {
        return active;
    }

    public Obstacle toDomain() {
        return new Obstacle(new Coordinate(centerX, centerY), radius, active);
    }

    public void deactivate() {
        active = false;
    }
}
