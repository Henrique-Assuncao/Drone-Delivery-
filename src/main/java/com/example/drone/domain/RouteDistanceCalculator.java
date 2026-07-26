package com.example.drone.domain;

import com.example.drone.exception.*;

import java.util.List;

public class RouteDistanceCalculator {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);

    public double routeDistance(List<Order> orders, List<Obstacle> obstacles) {
        validateOrders(orders);
        validateObstacles(obstacles);

        Coordinate currentLocation = BASE_LOCATION;
        double totalDistance = 0.0;

        for (Order order : orders) {
            totalDistance += segmentDistance(currentLocation, order.location(), obstacles);
            currentLocation = order.location();
        }

        return totalDistance + segmentDistance(currentLocation, BASE_LOCATION, obstacles);
    }

    public double roundTripDistanceFromBase(Order order, List<Obstacle> obstacles) {
        if (order == null) {
            throw new InvalidInputException("order must not be null");
        }

        validateObstacles(obstacles);

        return segmentDistance(BASE_LOCATION, order.location(), obstacles)
                + segmentDistance(order.location(), BASE_LOCATION, obstacles);
    }

    public double segmentDistance(Coordinate start, Coordinate end, List<Obstacle> obstacles) {
        if (start == null) {
            throw new InvalidInputException("start must not be null");
        }

        if (end == null) {
            throw new InvalidInputException("end must not be null");
        }

        validateObstacles(obstacles);

        double directDistance = start.distanceTo(end);
        double adjustedDistance = directDistance;

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.active() || !crossesObstacle(start, end, obstacle)) {
                continue;
            }

            double detourDistance = detourDistance(start, end, obstacle);
            if (Double.isInfinite(detourDistance)) {
                return Double.POSITIVE_INFINITY;
            }

            adjustedDistance += Math.max(0.0, detourDistance - directDistance);
        }

        return adjustedDistance;
    }

    private boolean crossesObstacle(Coordinate start, Coordinate end, Obstacle obstacle) {
        double radius = obstacle.radius();
        Coordinate center = obstacle.center();

        return start.distanceTo(center) <= radius
                || end.distanceTo(center) <= radius
                || distanceFromPointToSegment(center, start, end) <= radius;
    }

    private double detourDistance(Coordinate start, Coordinate end, Obstacle obstacle) {
        Coordinate center = obstacle.center();
        double radius = obstacle.radius();
        double distanceFromStartToCenter = start.distanceTo(center);
        double distanceFromEndToCenter = end.distanceTo(center);

        if (distanceFromStartToCenter <= radius || distanceFromEndToCenter <= radius) {
            return Double.POSITIVE_INFINITY;
        }

        double tangentFromStart = Math.sqrt(distanceFromStartToCenter * distanceFromStartToCenter - radius * radius);
        double tangentFromEnd = Math.sqrt(distanceFromEndToCenter * distanceFromEndToCenter - radius * radius);
        double angleBetweenPoints = angleBetween(center, start, end);
        double startTangentAngle = Math.acos(radius / distanceFromStartToCenter);
        double endTangentAngle = Math.acos(radius / distanceFromEndToCenter);
        double arcAngle = Math.max(0.0, angleBetweenPoints - startTangentAngle - endTangentAngle);

        return tangentFromStart + tangentFromEnd + radius * arcAngle;
    }

    private double angleBetween(Coordinate center, Coordinate first, Coordinate second) {
        double firstX = first.x() - center.x();
        double firstY = first.y() - center.y();
        double secondX = second.x() - center.x();
        double secondY = second.y() - center.y();
        double firstLength = Math.sqrt(firstX * firstX + firstY * firstY);
        double secondLength = Math.sqrt(secondX * secondX + secondY * secondY);
        double cosine = (firstX * secondX + firstY * secondY) / (firstLength * secondLength);

        return Math.acos(Math.max(-1.0, Math.min(1.0, cosine)));
    }

    private double distanceFromPointToSegment(Coordinate point, Coordinate start, Coordinate end) {
        double segmentX = end.x() - start.x();
        double segmentY = end.y() - start.y();
        double segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;

        if (segmentLengthSquared == 0.0) {
            return point.distanceTo(start);
        }

        double projection = ((point.x() - start.x()) * segmentX + (point.y() - start.y()) * segmentY)
                / segmentLengthSquared;
        double clampedProjection = Math.max(0.0, Math.min(1.0, projection));
        Coordinate closestPoint = new Coordinate(
                start.x() + clampedProjection * segmentX,
                start.y() + clampedProjection * segmentY
        );

        return point.distanceTo(closestPoint);
    }

    private void validateOrders(List<Order> orders) {
        if (orders == null) {
            throw new InvalidInputException("orders must not be null");
        }

        for (Order order : orders) {
            if (order == null) {
                throw new InvalidInputException("orders must not contain null");
            }
        }
    }

    private void validateObstacles(List<Obstacle> obstacles) {
        if (obstacles == null) {
            throw new InvalidInputException("obstacles must not be null");
        }

        for (Obstacle obstacle : obstacles) {
            if (obstacle == null) {
                throw new InvalidInputException("obstacles must not contain null");
            }
        }
    }
}
