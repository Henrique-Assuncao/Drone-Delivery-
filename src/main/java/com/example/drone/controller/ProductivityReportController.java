package com.example.drone.controller;

import com.example.drone.service.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports/productivity/monthly")
public class ProductivityReportController {

    private final ProductivityReportService reportService;

    public ProductivityReportController(ProductivityReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ProductivityReportResponse currentOrRequestedMonth(@RequestParam(required = false) String month) {
        ProductivityReport report = month == null || month.isBlank()
                ? reportService.refreshCurrentMonth()
                : reportService.refresh(month);

        return toResponse(report);
    }

    @GetMapping("/history")
    public List<ProductivityReportResponse> history() {
        return reportService.findSavedReports().stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductivityReportResponse toResponse(ProductivityReport report) {
        return new ProductivityReportResponse(
                report.month(),
                report.periodStart(),
                report.periodEnd(),
                report.orderEntries(),
                report.ordersSent(),
                report.ordersDelivered(),
                report.ordersCancelled(),
                conversionRate(report.ordersDelivered(), report.orderEntries()),
                report.drones().stream()
                        .map(this::toDroneResponse)
                        .toList(),
                report.generatedAt()
        );
    }

    private DroneProductivityReportResponse toDroneResponse(DroneProductivityReport report) {
        return new DroneProductivityReportResponse(
                report.droneId(),
                report.droneIdentifier(),
                report.ordersDelivered(),
                report.tripsStarted(),
                report.tripsCompleted(),
                report.tripsCancelled(),
                report.tripsReturnedEarly()
        );
    }

    private double conversionRate(int delivered, int entries) {
        if (entries == 0) {
            return 0.0;
        }

        return (double) delivered / entries;
    }

    public record ProductivityReportResponse(
            String month,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant periodStart,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant periodEnd,
            int orderEntries,
            int ordersSent,
            int ordersDelivered,
            int ordersCancelled,
            double conversionRate,
            List<DroneProductivityReportResponse> drones,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant generatedAt
    ) {
    }

    public record DroneProductivityReportResponse(
            Long droneId,
            String droneIdentifier,
            int ordersDelivered,
            int tripsStarted,
            int tripsCompleted,
            int tripsCancelled,
            int tripsReturnedEarly
    ) {
    }
}
