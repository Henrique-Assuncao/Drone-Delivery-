package com.example.drone.persistence;

import com.example.drone.exception.*;
import com.example.drone.service.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "monthly_drone_productivity_reports")
public class MonthlyDroneProductivityReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private MonthlyProductivityReportEntity report;

    @Column(name = "drone_id", nullable = false)
    private Long droneId;

    @Column(name = "drone_identifier", nullable = false)
    private String droneIdentifier;

    @Column(name = "orders_delivered", nullable = false)
    private int ordersDelivered;

    @Column(name = "trips_started", nullable = false)
    private int tripsStarted;

    @Column(name = "trips_completed", nullable = false)
    private int tripsCompleted;

    @Column(name = "trips_cancelled", nullable = false)
    private int tripsCancelled;

    @Column(name = "trips_returned_early", nullable = false)
    private int tripsReturnedEarly;

    protected MonthlyDroneProductivityReportEntity() {
    }

    public MonthlyDroneProductivityReportEntity(
            MonthlyProductivityReportEntity report,
            DroneProductivityReport droneReport
    ) {
        if (report == null) {
            throw new InvalidInputException("report must not be null");
        }

        if (droneReport == null) {
            throw new InvalidInputException("droneReport must not be null");
        }

        this.report = report;
        this.droneId = droneReport.droneId();
        this.droneIdentifier = droneReport.droneIdentifier();
        this.ordersDelivered = droneReport.ordersDelivered();
        this.tripsStarted = droneReport.tripsStarted();
        this.tripsCompleted = droneReport.tripsCompleted();
        this.tripsCancelled = droneReport.tripsCancelled();
        this.tripsReturnedEarly = droneReport.tripsReturnedEarly();
    }

    public Long getDroneId() {
        return droneId;
    }

    public String getDroneIdentifier() {
        return droneIdentifier;
    }

    public int getOrdersDelivered() {
        return ordersDelivered;
    }

    public int getTripsStarted() {
        return tripsStarted;
    }

    public int getTripsCompleted() {
        return tripsCompleted;
    }

    public int getTripsCancelled() {
        return tripsCancelled;
    }

    public int getTripsReturnedEarly() {
        return tripsReturnedEarly;
    }
}
