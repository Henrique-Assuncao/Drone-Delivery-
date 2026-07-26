package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/trip-plans")
public class TripPlanController {

    private final TripPlanningService planningService;

    public TripPlanController(TripPlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping
    public TripPlanResponse plan(@RequestParam(defaultValue = "true") boolean optimizeRoute) {
        return toResponse(planningService.planSaved(optimizeRoute));
    }

    private TripPlanResponse toResponse(PersistedTripPlan plan) {
        return new TripPlanResponse(
                plan.trips().stream().map(this::toTripResponse).toList(),
                plan.unallocatedOrders().stream().map(this::toUnallocatedOrderResponse).toList()
        );
    }

    private TripResponse toTripResponse(TripEntity trip) {
        List<Long> route = trip.getTripOrders().stream()
                .map(tripOrder -> tripOrder.getOrder().getId())
                .toList();

        List<TripRouteProgressResponse> routeProgress = trip.getTripOrders().stream()
                .map(tripOrder -> new TripRouteProgressResponse(
                        tripOrder.getOrder().getId(),
                        tripOrder.getRoutePosition(),
                        tripOrder.isDelivered(),
                        tripOrder.getDeliveredAt(),
                        tripOrder.getEstimatedDeliveryTime()
                ))
                .toList();

        return new TripResponse(
                trip.getId(),
                trip.getDrone().getId(),
                trip.getStatus(),
                route,
                route,
                routeProgress,
                trip.getTotalWeight(),
                trip.getTotalDistance(),
                trip.getEstimatedDuration(),
                trip.getAverageDeliveryTime()
        );
    }

    private UnallocatedOrderResponse toUnallocatedOrderResponse(PersistedUnallocatedOrder unallocatedOrder) {
        return new UnallocatedOrderResponse(
                unallocatedOrder.order().getId(),
                unallocatedOrder.order().getIdentifier(),
                unallocatedOrder.reason()
        );
    }

    public record TripPlanResponse(List<TripResponse> trips, List<UnallocatedOrderResponse> unallocatedOrders) {
    }

    public record TripResponse(
            Long id,
            Long droneId,
            TripStatus status,
            List<Long> orders,
            List<Long> route,
            List<TripRouteProgressResponse> routeProgress,
            double totalWeight,
            double totalDistance,
            double estimatedDuration,
            double averageDeliveryTime
    ) {
    }

    public record TripRouteProgressResponse(
            Long orderId,
            int routePosition,
            boolean delivered,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveredAt,
            double estimatedDeliveryTime
    ) {
    }

    public record UnallocatedOrderResponse(Long orderId, String orderIdentifier, String reason) {
    }
}
