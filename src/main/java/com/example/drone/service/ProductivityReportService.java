package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductivityReportService {

    private final OrderStorage orderStorage;
    private final DroneStorage droneStorage;
    private final TripStorage tripStorage;
    private final JpaMonthlyProductivityReportStorage reportStorage;
    private final ZoneId zoneId;

    public ProductivityReportService(
            OrderStorage orderStorage,
            DroneStorage droneStorage,
            TripStorage tripStorage,
            JpaMonthlyProductivityReportStorage reportStorage
    ) {
        this.orderStorage = orderStorage;
        this.droneStorage = droneStorage;
        this.tripStorage = tripStorage;
        this.reportStorage = reportStorage;
        this.zoneId = ZoneId.systemDefault();
    }

    @Transactional
    public ProductivityReport refreshCurrentMonth() {
        return refresh(YearMonth.now(zoneId).toString());
    }

    @Transactional
    public ProductivityReport refresh(String month) {
        YearMonth yearMonth = parseMonth(month);
        Instant periodStart = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant();
        Instant periodEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
        List<OrderEntity> orders = orderStorage.findAll();
        List<DroneEntity> drones = droneStorage.findAll();
        List<TripEntity> trips = tripStorage.findAll();

        ProductivityReport calculatedReport = new ProductivityReport(
                yearMonth.toString(),
                periodStart,
                periodEnd,
                countOrderEntries(orders, periodStart, periodEnd),
                countOrdersSent(trips, periodStart, periodEnd),
                countOrdersDelivered(trips, periodStart, periodEnd),
                countOrdersCancelled(trips, periodStart, periodEnd),
                buildDroneReports(drones, trips, periodStart, periodEnd),
                Instant.now()
        );

        MonthlyProductivityReportEntity entity = reportStorage.findByMonthKey(yearMonth.toString())
                .orElseGet(() -> new MonthlyProductivityReportEntity(yearMonth.toString()));
        entity.update(calculatedReport);

        return toReport(reportStorage.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ProductivityReport> findSavedReports() {
        return reportStorage.findAll().stream()
                .map(this::toReport)
                .toList();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new InvalidInputException("month must not be blank");
        }

        try {
            return YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new InvalidInputException("month must use YYYY-MM format");
        }
    }

    private int countOrderEntries(List<OrderEntity> orders, Instant periodStart, Instant periodEnd) {
        return (int) orders.stream()
                .filter(order -> isWithin(order.getQueuedAt(), periodStart, periodEnd))
                .count();
    }

    private int countOrdersSent(List<TripEntity> trips, Instant periodStart, Instant periodEnd) {
        return trips.stream()
                .filter(trip -> isWithin(trip.getStartedAt(), periodStart, periodEnd))
                .mapToInt(trip -> trip.getTripOrders().size())
                .sum();
    }

    private int countOrdersDelivered(List<TripEntity> trips, Instant periodStart, Instant periodEnd) {
        return trips.stream()
                .flatMap(trip -> trip.getTripOrders().stream())
                .filter(tripOrder -> isWithin(tripOrder.getDeliveredAt(), periodStart, periodEnd))
                .mapToInt(tripOrder -> 1)
                .sum();
    }

    private int countOrdersCancelled(List<TripEntity> trips, Instant periodStart, Instant periodEnd) {
        return trips.stream()
                .filter(trip -> trip.getStatus() == TripStatus.CANCELLED)
                .filter(trip -> isWithin(trip.getCancelledAt(), periodStart, periodEnd))
                .mapToInt(trip -> (int) trip.getTripOrders().stream()
                        .filter(tripOrder -> !tripOrder.isDelivered())
                        .count())
                .sum();
    }

    private List<DroneProductivityReport> buildDroneReports(
            List<DroneEntity> drones,
            List<TripEntity> trips,
            Instant periodStart,
            Instant periodEnd
    ) {
        List<DroneProductivityReport> reports = new ArrayList<>();

        for (DroneEntity drone : drones) {
            List<TripEntity> droneTrips = trips.stream()
                    .filter(trip -> trip.getDrone().getId().equals(drone.getId()))
                    .toList();
            int ordersDelivered = droneTrips.stream()
                    .flatMap(trip -> trip.getTripOrders().stream())
                    .filter(tripOrder -> isWithin(tripOrder.getDeliveredAt(), periodStart, periodEnd))
                    .mapToInt(tripOrder -> 1)
                    .sum();
            int tripsStarted = (int) droneTrips.stream()
                    .filter(trip -> isWithin(trip.getStartedAt(), periodStart, periodEnd))
                    .count();
            int tripsCompleted = (int) droneTrips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                    .filter(trip -> isWithin(trip.getEndedAt(), periodStart, periodEnd))
                    .count();
            int tripsCancelled = (int) droneTrips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.CANCELLED)
                    .filter(trip -> isWithin(trip.getCancelledAt(), periodStart, periodEnd))
                    .count();
            int tripsReturnedEarly = (int) droneTrips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.RETURNED_EARLY)
                    .filter(trip -> isWithin(trip.getEndedAt(), periodStart, periodEnd))
                    .count();

            reports.add(new DroneProductivityReport(
                    drone.getId(),
                    drone.getIdentifier(),
                    ordersDelivered,
                    tripsStarted,
                    tripsCompleted,
                    tripsCancelled,
                    tripsReturnedEarly
            ));
        }

        return reports.stream()
                .sorted(Comparator
                        .comparingInt(DroneProductivityReport::ordersDelivered).reversed()
                        .thenComparing(Comparator.comparingInt(DroneProductivityReport::tripsCancelled).reversed())
                        .thenComparing(DroneProductivityReport::droneIdentifier))
                .toList();
    }

    private boolean isWithin(Instant value, Instant periodStart, Instant periodEnd) {
        return value != null && !value.isBefore(periodStart) && value.isBefore(periodEnd);
    }

    private ProductivityReport toReport(MonthlyProductivityReportEntity entity) {
        return new ProductivityReport(
                entity.getMonthKey(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getOrderEntries(),
                entity.getOrdersSent(),
                entity.getOrdersDelivered(),
                entity.getOrdersCancelled(),
                entity.getDroneProductivity().stream()
                        .map(drone -> new DroneProductivityReport(
                                drone.getDroneId(),
                                drone.getDroneIdentifier(),
                                drone.getOrdersDelivered(),
                                drone.getTripsStarted(),
                                drone.getTripsCompleted(),
                                drone.getTripsCancelled(),
                                drone.getTripsReturnedEarly()
                        ))
                        .toList(),
                entity.getGeneratedAt()
        );
    }
}
