package com.example.drone.persistence;

import com.example.drone.exception.*;
import com.example.drone.service.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monthly_productivity_reports")
public class MonthlyProductivityReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_key", nullable = false, unique = true)
    private String monthKey;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "order_entries", nullable = false)
    private int orderEntries;

    @Column(name = "orders_sent", nullable = false)
    private int ordersSent;

    @Column(name = "orders_delivered", nullable = false)
    private int ordersDelivered;

    @Column(name = "orders_cancelled", nullable = false)
    private int ordersCancelled;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordersDelivered DESC, tripsCancelled DESC, droneIdentifier ASC")
    private List<MonthlyDroneProductivityReportEntity> droneProductivity = new ArrayList<>();

    protected MonthlyProductivityReportEntity() {
    }

    public MonthlyProductivityReportEntity(String monthKey) {
        if (monthKey == null || monthKey.isBlank()) {
            throw new InvalidInputException("monthKey must not be blank");
        }

        this.monthKey = monthKey;
    }

    public Long getId() {
        return id;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public int getOrderEntries() {
        return orderEntries;
    }

    public int getOrdersSent() {
        return ordersSent;
    }

    public int getOrdersDelivered() {
        return ordersDelivered;
    }

    public int getOrdersCancelled() {
        return ordersCancelled;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public List<MonthlyDroneProductivityReportEntity> getDroneProductivity() {
        return List.copyOf(droneProductivity);
    }

    public void update(ProductivityReport report) {
        if (report == null) {
            throw new InvalidInputException("report must not be null");
        }

        this.periodStart = report.periodStart();
        this.periodEnd = report.periodEnd();
        this.orderEntries = report.orderEntries();
        this.ordersSent = report.ordersSent();
        this.ordersDelivered = report.ordersDelivered();
        this.ordersCancelled = report.ordersCancelled();
        this.generatedAt = report.generatedAt();

        droneProductivity.clear();
        for (DroneProductivityReport droneReport : report.drones()) {
            droneProductivity.add(new MonthlyDroneProductivityReportEntity(this, droneReport));
        }
    }
}
