package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.persistence.*;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DemoDataService {

    private final EntityManager entityManager;
    private final TripPlanningService planningService;

    public DemoDataService(EntityManager entityManager, TripPlanningService planningService) {
        this.entityManager = entityManager;
        this.planningService = planningService;
    }

    @Transactional
    public DemoScenario resetAndSeed() {
        clearOperationalData();

        List<DroneEntity> drones = seedDrones();
        List<OrderEntity> orders = seedOrders();
        List<ObstacleEntity> obstacles = seedObstacles();
        ReviewEntity review = seedReview();

        entityManager.flush();

        PersistedTripPlan plan = planningService.planSaved(true);

        return new DemoScenario(drones, orders, obstacles, review, plan);
    }

    private void clearOperationalData() {
        entityManager.createQuery("delete from MonthlyDroneProductivityReportEntity").executeUpdate();
        entityManager.createQuery("delete from MonthlyProductivityReportEntity").executeUpdate();
        entityManager.createQuery("delete from TripTelemetryEntity").executeUpdate();
        entityManager.createQuery("delete from TripOrderEntity").executeUpdate();
        entityManager.createQuery("delete from TripEntity").executeUpdate();
        entityManager.createQuery("delete from ObstacleEntity").executeUpdate();
        entityManager.createQuery("delete from ReviewEntity").executeUpdate();
        entityManager.createQuery("delete from OrderEntity").executeUpdate();
        entityManager.createQuery("delete from DroneEntity").executeUpdate();
        entityManager.clear();
    }

    private List<DroneEntity> seedDrones() {
        List<DroneEntity> drones = List.of(
                new DroneEntity(null, "DEMO-ALFA", 12.0, 120.0, DroneStatus.AVAILABLE, 100.0, 0.45, 18.0, 2.4, 14.0),
                new DroneEntity(null, "DEMO-BRAVO", 8.0, 80.0, DroneStatus.AVAILABLE, 86.0, 0.55, 18.0, 1.8, 11.0),
                new DroneEntity(null, "DEMO-CHARLIE", 6.0, 60.0, DroneStatus.AVAILABLE, 22.0, 0.8, 18.0, 1.6, 15.0)
        );

        drones.forEach(entityManager::persist);
        return drones;
    }

    private List<OrderEntity> seedOrders() {
        List<OrderEntity> orders = List.of(
                new OrderEntity(null, "DEMO-URGENTE", 6.0, 4.0, 4.5, Priority.HIGH, OrderStatus.REQUESTED),
                new OrderEntity(null, "DEMO-PESADO", 12.0, 7.0, 6.0, Priority.HIGH, OrderStatus.REQUESTED),
                new OrderEntity(null, "DEMO-MEDIO", -5.0, 10.0, 3.0, Priority.MEDIUM, OrderStatus.REQUESTED),
                new OrderEntity(null, "DEMO-RETORNO", -10.0, -4.0, 2.5, Priority.MEDIUM, OrderStatus.REQUESTED),
                new OrderEntity(null, "DEMO-LEVE", 7.0, -8.0, 2.0, Priority.LOW, OrderStatus.REQUESTED)
        );

        orders.forEach(entityManager::persist);
        return orders;
    }

    private List<ObstacleEntity> seedObstacles() {
        List<ObstacleEntity> obstacles = List.of(
                new ObstacleEntity(null, 4.0, 2.0, 1.2, true)
        );

        obstacles.forEach(entityManager::persist);
        return obstacles;
    }

    private ReviewEntity seedReview() {
        ReviewEntity review = new ReviewEntity(
                null,
                5,
                "Demo operacional",
                "Cenario criado para validar o ciclo operacional pelo dashboard."
        );

        entityManager.persist(review);
        return review;
    }

    public record DemoScenario(
            List<DroneEntity> drones,
            List<OrderEntity> orders,
            List<ObstacleEntity> obstacles,
            ReviewEntity review,
            PersistedTripPlan plan
    ) {
        public DemoScenario {
            drones = List.copyOf(drones);
            orders = List.copyOf(orders);
            obstacles = List.copyOf(obstacles);
        }
    }
}
