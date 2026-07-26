package com.example.drone.persistence;

import com.example.drone.domain.*;
import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "trip_telemetry")
public class TripTelemetryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Column(name = "battery_level", nullable = false)
    private double batteryLevel;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    protected TripTelemetryEntity() {
    }

    public TripTelemetryEntity(Long id, TripEntity trip, double batteryLevel, Instant reportedAt) {
        if (trip == null) {
            throw new InvalidInputException("trip must not be null");
        }

        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new InvalidInputException("batteryLevel must be between 0 and 100");
        }

        if (reportedAt == null) {
            throw new InvalidInputException("reportedAt must not be null");
        }

        this.id = id;
        this.trip = trip;
        this.batteryLevel = batteryLevel;
        this.reportedAt = reportedAt;
    }

    public Long getId() {
        return id;
    }

    public TripEntity getTrip() {
        return trip;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }
}
