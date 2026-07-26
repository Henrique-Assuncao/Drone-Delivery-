package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/drones")
@Tag(name = "Interno - Drones", description = "Consultas internas de dados sensíveis de drones.")
@SecurityRequirement(name = "internalApiKey")
public class InternalDroneController {

    private final DroneQueryService queryService;

    public InternalDroneController(DroneQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}/battery")
    public DroneBatteryResponse findBatteryByDroneId(@PathVariable Long id) {
        DroneEntity drone = queryService.findById(id);

        return new DroneBatteryResponse(
                drone.getId(),
                drone.getIdentifier(),
                drone.getStatus(),
                drone.getBatteryLevel(),
                drone.getBatteryConsumptionPerDistanceUnit(),
                drone.getMinimumReturnBattery(),
                drone.getChargingRate()
        );
    }

    public record DroneBatteryResponse(
            Long id,
            String identifier,
            DroneStatus status,
            @Schema(description = "Nível atual de bateria em percentual (%).", example = "100.0")
            double batteryLevel,
            @Schema(description = "Consumo de bateria em percentual por quilômetro (%/km).", example = "1.0")
            double batteryConsumptionPerDistanceUnit,
            @Schema(description = "Reserva mínima de bateria para retorno em percentual (%).", example = "20.0")
            double minimumReturnBattery,
            @Schema(description = "Taxa de recarga em percentual por minuto (%/min).", example = "10.0")
            double chargingRate
    ) {
    }
}
