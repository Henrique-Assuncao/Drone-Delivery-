package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/client/orders")
@Tag(name = "Pedidos do Cliente", description = "Solicitação e consulta de pedidos vinculados à conta autenticada.")
@SecurityRequirement(name = "clientBearerAuth")
public class ClientOrderController {

    private final ClientAuthenticationService authenticationService;
    private final OrderRegistrationService registrationService;
    private final OrderStorage orderStorage;

    public ClientOrderController(
            ClientAuthenticationService authenticationService,
            OrderRegistrationService registrationService,
            OrderStorage orderStorage
    ) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.orderStorage = orderStorage;
    }

    @GetMapping
    public List<ClientOrderResponse> list(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        ClientUserEntity user = authenticationService.authenticate(authorizationHeader);

        return orderStorage.findByClientUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ClientOrderResponse> create(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody(required = false) CreateClientOrderRequest request
    ) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        ClientUserEntity user = authenticationService.authenticate(authorizationHeader);
        OrderEntity order = registrationService.register(
                request.identifier(),
                toCoordinate(request.location()),
                request.weight(),
                Priority.MEDIUM,
                request.confirmedDeliveryTime(),
                user
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    private Coordinate toCoordinate(LocationRequest location) {
        if (location == null) {
            return null;
        }

        return new Coordinate(location.x(), location.y());
    }

    private ClientOrderResponse toResponse(OrderEntity order) {
        return new ClientOrderResponse(
                order.getId(),
                order.getIdentifier(),
                new LocationResponse(order.getLocationX(), order.getLocationY()),
                order.getWeight(),
                order.getPriority(),
                order.getStatus(),
                order.getQueuedAt(),
                order.getConfirmedDeliveryTime(),
                order.getDeliveryConfirmationCode(),
                order.getStatusReason()
        );
    }

    public record CreateClientOrderRequest(
            String identifier,
            LocationRequest location,
            @Schema(description = "Peso do pacote em quilogramas (kg).", example = "4.0")
            double weight,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant confirmedDeliveryTime
    ) {
    }

    public record LocationRequest(
            @Schema(description = "Coordenada X do endereço em quilômetros (km) a partir da base.", example = "3.0")
            double x,
            @Schema(description = "Coordenada Y do endereço em quilômetros (km) a partir da base.", example = "4.0")
            double y
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientOrderResponse(
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
