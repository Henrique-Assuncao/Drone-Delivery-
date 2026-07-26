package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/recharge-queue")
@Tag(name = "Fila de Recarga", description = "Consulta da fila de drones em recarga.")
public class RechargeQueueController {

    private final DroneRechargeService rechargeService;

    public RechargeQueueController(DroneRechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @GetMapping
    public List<RechargeQueueEntryResponse> list() {
        return rechargeService.findQueue().stream()
                .map(this::toResponse)
                .toList();
    }

    private RechargeQueueEntryResponse toResponse(DroneEntity drone) {
        return new RechargeQueueEntryResponse(
                drone.getId(),
                drone.getIdentifier(),
                drone.getStatus(),
                drone.getBatteryLevel(),
                drone.getRechargeQueuedAt(),
                drone.getRechargeReason()
        );
    }

    public record RechargeQueueEntryResponse(
            Long droneId,
            String droneIdentifier,
            DroneStatus status,
            @Schema(description = "Nível atual de bateria em percentual (%).", example = "35.0")
            double batteryLevel,
            Instant queuedAt,
            String reason
    ) {
    }
}
