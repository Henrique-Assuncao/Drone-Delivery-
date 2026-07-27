package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/trip-plans")
@Tag(name = "Planejamento", description = "Criação de planos de viagem a partir de drones e pedidos persistidos.")
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
        Instant now = Instant.now();
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
                trip.getAverageDeliveryTime(),
                TripDispatchPolicy.idealDispatchTimeFor(trip).orElse(null),
                TripDispatchPolicy.isDispatchWindowOpen(trip, now),
                TripDispatchPolicy.minutesUntilIdealDispatch(trip, now)
        );
    }

    private UnallocatedOrderResponse toUnallocatedOrderResponse(PersistedUnallocatedOrder unallocatedOrder) {
        return new UnallocatedOrderResponse(
                unallocatedOrder.order().getId(),
                unallocatedOrder.order().getIdentifier(),
                unallocatedOrder.reason()
        );
    }

    @Schema(name = "TripPlanResponse")
    public record TripPlanResponse(List<TripResponse> trips, List<UnallocatedOrderResponse> unallocatedOrders) {
    }

    @Schema(name = "TripPlanTripResponse")
    public record TripResponse(
            Long id,
            Long droneId,
            TripStatus status,
            List<Long> orders,
            List<Long> route,
            List<TripRouteProgressResponse> routeProgress,
            @Schema(description = "Peso total dos pacotes na viagem em quilogramas (kg).", example = "9.0")
            double totalWeight,
            @Schema(description = "Distância total planejada da viagem em quilômetros (km).", example = "20.0")
            double totalDistance,
            @Schema(description = "Duração estimada da viagem em minutos (min).", example = "20.0")
            double estimatedDuration,
            @Schema(description = "Tempo médio estimado de entrega em minutos (min).", example = "8.0")
            double averageDeliveryTime,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            @Schema(description = "Horário ideal de saída para cumprir os horários confirmados de entrega.")
            Instant idealDispatchTime,
            @Schema(description = "Indica se a janela ideal de saída já está aberta.", example = "false")
            boolean dispatchWindowOpen,
            @Schema(description = "Minutos restantes até o horário ideal de saída.", example = "12.5")
            double minutesUntilIdealDispatch
    ) {
    }

    @Schema(name = "TripPlanRouteProgressResponse")
    public record TripRouteProgressResponse(
            Long orderId,
            int routePosition,
            boolean delivered,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveredAt,
            @Schema(description = "Tempo estimado até esta entrega em minutos (min).", example = "5.0")
            double estimatedDeliveryTime
    ) {
    }

    public record UnallocatedOrderResponse(Long orderId, String orderIdentifier, String reason) {
    }
}
