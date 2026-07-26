package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class OrderController {

    private final OrderRegistrationService registrationService;
    private final OrderQueryService queryService;

    public OrderController(OrderRegistrationService registrationService, OrderQueryService queryService) {
        this.registrationService = registrationService;
        this.queryService = queryService;
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
                request.priority()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status) {
        List<OrderEntity> orders = status == null
                ? queryService.findAll()
                : queryService.findByStatus(status);

        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return toResponse(queryService.findById(id));
    }

    private Coordinate toCoordinate(LocationRequest location) {
        if (location == null) {
            return null;
        }

        return new Coordinate(location.x(), location.y());
    }

    private OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getIdentifier(),
                new LocationResponse(order.getLocationX(), order.getLocationY()),
                order.getWeight(),
                order.getPriority(),
                order.getStatus(),
                order.getQueuedAt()
        );
    }

    public record CreateOrderRequest(
            String identifier,
            LocationRequest location,
            double weight,
            Priority priority
    ) {
    }

    public record LocationRequest(double x, double y) {
    }

    public record OrderResponse(
            Long id,
            String identifier,
            LocationResponse location,
            double weight,
            Priority priority,
            OrderStatus status,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant queuedAt
    ) {
    }

    public record LocationResponse(double x, double y) {
    }
}
