package com.example.drone.service;

import java.time.Instant;
import java.util.List;

public record ProductivityReport(
        String month,
        Instant periodStart,
        Instant periodEnd,
        int orderEntries,
        int ordersSent,
        int ordersDelivered,
        int ordersCancelled,
        List<DroneProductivityReport> drones,
        Instant generatedAt
) {
    public ProductivityReport {
        drones = List.copyOf(drones);
    }
}
