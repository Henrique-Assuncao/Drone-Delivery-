package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/orders")
@Tag(name = "Pedidos Admin", description = "Cadastro, consulta, cancelamento e replanejamento de pedidos operacionais.")
public class OrderController {

    private final OrderRegistrationService registrationService;
    private final OrderQueryService queryService;
    private final OrderOperationService operationService;

    public OrderController(
            OrderRegistrationService registrationService,
            OrderQueryService queryService,
            OrderOperationService operationService
    ) {
        this.registrationService = registrationService;
        this.queryService = queryService;
        this.operationService = operationService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        OrderEntity order = registrationService.register(
                request.identifier(),
                toCoordinate(request.location()),
                request.weight(),
                request.priority(),
                request.confirmedDeliveryTime()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order, true));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status) {
        List<OrderEntity> orders = status == null
                ? queryService.findAll()
                : queryService.findByStatus(status);

        return orders.stream()
                .map(order -> toResponse(order, false))
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findById(id), false);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @RequestBody(required = false) CancelOrderRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        return toResponse(operationService.cancelUnallocated(id, request.reasonOrThrow()), false);
    }

    @PostMapping("/{id}/requeue")
    public OrderResponse requeue(@PathVariable Long id) {
        return toResponse(operationService.requeueUnallocated(id), false);
    }

    private Coordinate toCoordinate(LocationRequest location) {
        if (location == null) {
            return null;
        }

        return new Coordinate(location.x(), location.y());
    }

    private OrderResponse toResponse(OrderEntity order, boolean includeDeliveryConfirmationCode) {
        return new OrderResponse(
                order.getId(),
                order.getIdentifier(),
                new LocationResponse(order.getLocationX(), order.getLocationY()),
                order.getWeight(),
                order.getPriority(),
                order.getStatus(),
                order.getQueuedAt(),
                order.getConfirmedDeliveryTime(),
                includeDeliveryConfirmationCode ? order.getDeliveryConfirmationCode() : null,
                order.getStatusReason()
        );
    }

    public record CreateOrderRequest(
            String identifier,
            LocationRequest location,
            @Schema(description = "Peso do pacote em quilogramas (kg).", example = "4.0")
            double weight,
            Priority priority,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant confirmedDeliveryTime
    ) {
    }

    public record CancelOrderRequest(String reason) {

        String reasonOrThrow() {
            if (reason == null || reason.isBlank()) {
                throw new InvalidInputException("cancel reason must not be blank");
            }

            return reason;
        }
    }

    public record LocationRequest(
            @Schema(description = "Coordenada X do endereço em quilômetros (km) a partir da base.", example = "3.0")
            double x,
            @Schema(description = "Coordenada Y do endereço em quilômetros (km) a partir da base.", example = "4.0")
            double y
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderResponse(
            Long id,
            String identifier,
            LocationResponse location,
            @Schema(description = "Peso do pacote em quilogramas (kg).", example = "4.0")
            double weight,
            Priority priority,
            OrderStatus status,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant queuedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant confirmedDeliveryTime,
            String deliveryConfirmationCode,
            String statusReason
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
