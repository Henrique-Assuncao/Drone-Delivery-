package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-queue")
@Tag(name = "Fila de Entrega", description = "Consulta da fila operacional de pedidos aguardando planejamento.")
public class DeliveryQueueController {

    private final OrderQueryService queryService;

    public DeliveryQueueController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<DeliveryQueueEntryResponse> list() {
        return queryService.findDeliveryQueue().stream()
                .map(this::toResponse)
                .toList();
    }

    private DeliveryQueueEntryResponse toResponse(OrderEntity order) {
        return new DeliveryQueueEntryResponse(
                order.getId(),
                order.getIdentifier(),
                new LocationResponse(order.getLocationX(), order.getLocationY()),
                order.getWeight(),
                order.getPriority(),
                order.getStatus(),
                order.getQueuedAt(),
                order.getConfirmedDeliveryTime()
        );
    }

    public record DeliveryQueueEntryResponse(
            Long orderId,
            String orderIdentifier,
            LocationResponse location,
            @Schema(description = "Peso do pacote em quilogramas (kg).", example = "4.0")
            double weight,
            Priority priority,
            OrderStatus status,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant queuedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant confirmedDeliveryTime
    ) {
    }

    public record LocationResponse(
            @Schema(description = "Coordenada X do endereço em quilômetros (km) a partir da base.", example = "3.0")
            double x,
            @Schema(description = "Coordenada Y do endereço em quilômetros (km) a partir da base.", example = "4.0")
            double y
    ) {
    }
}
