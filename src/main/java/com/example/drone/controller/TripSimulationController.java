package com.example.drone.controller;

import com.example.drone.exception.*;
import com.example.drone.service.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{id}/simulation")
@Tag(name = "Simulação", description = "Estado e avanço temporal da simulação de viagens.")
public class TripSimulationController {

    private static final double DEFAULT_ELAPSED_MINUTES = 1.0;

    private final TripSimulationService simulationService;

    public TripSimulationController(TripSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public TripSimulationState findState(@PathVariable Long id) {
        return simulationService.findState(id);
    }

    @PostMapping("/tick")
    public TripSimulationState tick(
            @PathVariable Long id,
            @RequestBody(required = false) TripSimulationTickRequest request
    ) {
        double elapsedMinutes = request == null ? DEFAULT_ELAPSED_MINUTES : request.elapsedMinutesOrDefault();

        return simulationService.advance(id, elapsedMinutes);
    }

    public record TripSimulationTickRequest(Double elapsedMinutes) {

        double elapsedMinutesOrDefault() {
            if (elapsedMinutes == null) {
                return DEFAULT_ELAPSED_MINUTES;
            }

            if (elapsedMinutes <= 0) {
                throw new InvalidInputException("elapsedMinutes must be greater than zero");
            }

            return elapsedMinutes;
        }
    }
}
