package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "Viagens", description = "Consulta e transições operacionais de viagens.")
public class TripController {

    private final TripQueryService queryService;
    private final TripTransitionService transitionService;
    private final TripTelemetryService telemetryService;

    public TripController(
            TripQueryService queryService,
            TripTransitionService transitionService,
            TripTelemetryService telemetryService
    ) {
        this.queryService = queryService;
        this.transitionService = transitionService;
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public List<TripResponse> listAll(@RequestParam(required = false) TripStatus status) {
        List<TripEntity> trips = status == null
                ? queryService.findAll()
                : queryService.findByStatus(status);

        return trips.stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findById(id));
    }

    @GetMapping("/{id}/telemetry")
    public List<TripTelemetryResponse> telemetryHistory(@PathVariable Long id) {
        return telemetryService.findByTripId(id).stream()
                .map(this::toTelemetryResponse)
                .toList();
    }

    @PostMapping("/{id}/start")
    public TripResponse start(@PathVariable Long id) {
        return toResponse(transitionService.start(id));
    }

    @PostMapping("/{id}/complete")
    public TripResponse complete(@PathVariable Long id) {
        return toResponse(transitionService.complete(id));
    }

    @PostMapping("/{id}/route/{routePosition}/deliver")
    public TripResponse deliverRoutePosition(
            @PathVariable Long id,
            @PathVariable int routePosition,
            @RequestBody(required = false) DeliveryConfirmationRequest request
    ) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        return toResponse(transitionService.deliverRoutePosition(
                id,
                routePosition,
                request.confirmationCodeOrThrow()
        ));
    }

    @PostMapping("/{id}/route/{routePosition}/availability")
    public TripResponse confirmRouteAvailability(
            @PathVariable Long id,
            @PathVariable int routePosition,
            @RequestBody(required = false) DeliveryAvailabilityRequest request
    ) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        return toResponse(transitionService.confirmRouteAvailability(
                id,
                routePosition,
                request.availableOrThrow()
        ));
    }

    @PostMapping("/{id}/telemetry")
    public TripResponse reportTelemetry(@PathVariable Long id, @RequestBody(required = false) TripTelemetryRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        return toResponse(transitionService.reportTelemetry(id, request.batteryLevelOrThrow()));
    }

    @PostMapping("/{id}/cancel")
    public TripResponse cancel(@PathVariable Long id) {
        return toResponse(transitionService.cancel(id));
    }

    private TripResponse toResponse(TripEntity trip) {
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
                        tripOrder.getEstimatedDeliveryTime(),
                        tripOrder.getAvailabilityNotifiedAt(),
                        tripOrder.getAvailabilityConfirmedAt(),
                        DeliveryAvailabilityPolicy.responseDeadlineFor(tripOrder),
                        tripOrder.getDeliveryConfirmationRequestedAt(),
                        DeliveryAvailabilityPolicy.deliveryConfirmationDeadlineFor(tripOrder),
                        tripOrder.getDeliveryFailedAt(),
                        tripOrder.getDeliveryFailureReason()
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
                TripDispatchPolicy.minutesUntilIdealDispatch(trip, now),
                TripSimulationService.stateFor(trip)
        );
    }

    private TripTelemetryResponse toTelemetryResponse(TripTelemetryEntity telemetry) {
        return new TripTelemetryResponse(
                telemetry.getId(),
                telemetry.getTrip().getId(),
                telemetry.getBatteryLevel(),
                telemetry.getReportedAt()
        );
    }

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
            double minutesUntilIdealDispatch,
            TripSimulationState simulation
    ) {
    }

    public record TripRouteProgressResponse(
            Long orderId,
            int routePosition,
            boolean delivered,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveredAt,
            @Schema(description = "Tempo estimado até esta entrega em minutos (min).", example = "5.0")
            double estimatedDeliveryTime,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant availabilityNotifiedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant availabilityConfirmedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant availabilityResponseDeadline,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveryConfirmationRequestedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveryConfirmationDeadline,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant deliveryFailedAt,
            String deliveryFailureReason
    ) {
    }

    public record TripTelemetryRequest(
            @Schema(description = "Nível atual de bateria em percentual (%).", example = "70.0")
            Double batteryLevel
    ) {

        double batteryLevelOrThrow() {
            if (batteryLevel == null) {
                throw new InvalidInputException("batteryLevel must not be null");
            }

            return batteryLevel;
        }
    }

    public record DeliveryConfirmationRequest(String confirmationCode) {

        String confirmationCodeOrThrow() {
            if (confirmationCode == null || confirmationCode.isBlank()) {
                throw new InvalidInputException("delivery confirmation code must not be blank");
            }

            return confirmationCode;
        }
    }

    public record DeliveryAvailabilityRequest(Boolean available) {

        boolean availableOrThrow() {
            if (available == null) {
                throw new InvalidInputException("available must not be null");
            }

            return available;
        }
    }

    public record TripTelemetryResponse(
            Long id,
            Long tripId,
            @Schema(description = "Nível de bateria reportado em percentual (%).", example = "70.0")
            double batteryLevel,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant reportedAt
    ) {
    }
}
