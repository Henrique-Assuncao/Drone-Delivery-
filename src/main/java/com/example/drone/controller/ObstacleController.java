package com.example.drone.controller;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/obstacles")
public class ObstacleController {

    private final ObstacleRegistrationService registrationService;
    private final ObstacleQueryService queryService;
    private final ObstacleCommandService commandService;

    public ObstacleController(
            ObstacleRegistrationService registrationService,
            ObstacleQueryService queryService,
            ObstacleCommandService commandService
    ) {
        this.registrationService = registrationService;
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<ObstacleResponse> create(@RequestBody CreateObstacleRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        ObstacleEntity obstacle = registrationService.register(toCoordinate(request.center()), request.radius());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(obstacle));
    }

    @GetMapping
    public List<ObstacleResponse> list() {
        return queryService.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ObstacleResponse deactivate(@PathVariable Long id) {
        return toResponse(commandService.deactivate(id));
    }

    private Coordinate toCoordinate(LocationRequest center) {
        if (center == null) {
            return null;
        }

        return new Coordinate(center.x(), center.y());
    }

    private ObstacleResponse toResponse(ObstacleEntity obstacle) {
        return new ObstacleResponse(
                obstacle.getId(),
                new LocationResponse(obstacle.getCenterX(), obstacle.getCenterY()),
                obstacle.getRadius(),
                obstacle.isActive()
        );
    }

    public record CreateObstacleRequest(LocationRequest center, double radius) {
    }

    public record LocationRequest(double x, double y) {
    }

    public record ObstacleResponse(Long id, LocationResponse center, double radius, boolean active) {
    }

    public record LocationResponse(double x, double y) {
    }
}
