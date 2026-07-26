package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-queue")
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
            double weight,
            Priority priority,
            OrderStatus status,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant queuedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant confirmedDeliveryTime
    ) {
    }

    public record LocationResponse(double x, double y) {
    }
}
