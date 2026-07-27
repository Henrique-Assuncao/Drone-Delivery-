package com.example.drone.service;

import com.example.drone.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

public final class TripDispatchPolicy {

    private static final long NANOS_PER_MINUTE = 60_000_000_000L;

    private TripDispatchPolicy() {
    }

    public static Optional<Instant> idealDispatchTimeFor(TripEntity trip) {
        return trip.getTripOrders().stream()
                .filter(tripOrder -> !tripOrder.isResolved())
                .map(tripOrder -> tripOrder.getOrder()
                        .getConfirmedDeliveryTime()
                        .minus(durationForMinutes(tripOrder.getEstimatedDeliveryTime())))
                .min(Comparator.naturalOrder());
    }

    public static boolean isDispatchWindowOpen(TripEntity trip, Instant now) {
        return idealDispatchTimeFor(trip)
                .map(idealDispatchTime -> !now.isBefore(idealDispatchTime))
                .orElse(true);
    }

    public static double minutesUntilIdealDispatch(TripEntity trip, Instant now) {
        return idealDispatchTimeFor(trip)
                .map(idealDispatchTime -> Math.max(0.0, Duration.between(now, idealDispatchTime).toMillis() / 60_000.0))
                .orElse(0.0);
    }

    private static Duration durationForMinutes(double minutes) {
        return Duration.ofNanos(Math.round(minutes * NANOS_PER_MINUTE));
    }
}
