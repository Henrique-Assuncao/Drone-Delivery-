package com.example.drone.service;

public record DroneProductivityReport(
        Long droneId,
        String droneIdentifier,
        int ordersDelivered,
        int tripsStarted,
        int tripsCompleted,
        int tripsCancelled,
        int tripsReturnedEarly
) {
}
