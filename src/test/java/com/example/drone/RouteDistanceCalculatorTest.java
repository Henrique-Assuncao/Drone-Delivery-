package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDistanceCalculatorTest {

    private static final double DECIMAL_TOLERANCE = 1.0E-9;

    private final RouteDistanceCalculator calculator = new RouteDistanceCalculator();

    @Test
    void shouldKeepEuclideanDistanceWhenSegmentDoesNotCrossObstacle() {
        double distance = calculator.segmentDistance(
                new Coordinate(0.0, 0.0),
                new Coordinate(0.0, 10.0),
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        assertEquals(10.0, distance, DECIMAL_TOLERANCE);
    }

    @Test
    void shouldIncreaseDistanceWhenSegmentCrossesActiveObstacle() {
        double distance = calculator.segmentDistance(
                new Coordinate(0.0, 0.0),
                new Coordinate(10.0, 0.0),
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        assertTrue(distance > 10.0);
        assertEquals(10.200674813517882, distance, DECIMAL_TOLERANCE);
    }

    @Test
    void shouldIgnoreInactiveObstacle() {
        double distance = calculator.segmentDistance(
                new Coordinate(0.0, 0.0),
                new Coordinate(10.0, 0.0),
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0, false))
        );

        assertEquals(10.0, distance, DECIMAL_TOLERANCE);
    }

    @Test
    void shouldTreatRoutePointInsideObstacleAsUnreachable() {
        double distance = calculator.segmentDistance(
                new Coordinate(0.0, 0.0),
                new Coordinate(5.0, 0.0),
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        assertTrue(Double.isInfinite(distance));
    }
}
