package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.persistence.*;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DemoDataService {

    public static final String DEMO_CLIENT_EMAIL = "cliente.demo@drone.local";
    public static final String DEMO_CLIENT_PASSWORD = "senha123";

    private final EntityManager entityManager;
    private final TripPlanningService planningService;
    private final PasswordHashingService passwordHashingService;

    public DemoDataService(
            EntityManager entityManager,
            TripPlanningService planningService,
            PasswordHashingService passwordHashingService
    ) {
        this.entityManager = entityManager;
        this.planningService = planningService;
        this.passwordHashingService = passwordHashingService;
    }

    @Transactional
    public DemoScenario resetAndSeed() {
        clearOperationalData();
        return seedScenario();
    }

    @Transactional
    public boolean seedInitialScenarioIfEmpty() {
        if (hasAnyApplicationData()) {
            return false;
        }

        seedScenario();
        return true;
    }

    private DemoScenario seedScenario() {
        Instant now = Instant.now();

        List<DroneEntity> drones = seedDrones();
        List<ClientUserEntity> clientUsers = seedClientUsers(now);
        List<OrderEntity> orders = seedOrders(clientUsers.get(0), now);
        List<ObstacleEntity> obstacles = seedObstacles();
        List<ReviewEntity> reviews = seedReviews();

        entityManager.flush();

        PersistedTripPlan plan = planningService.planSaved(true);

        return new DemoScenario(drones, orders, obstacles, reviews, clientUsers, plan);
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
        entityManager.createQuery("delete from ClientUserEntity").executeUpdate();
        entityManager.createQuery("delete from DroneEntity").executeUpdate();
        entityManager.clear();
    }

    private boolean hasAnyApplicationData() {
        return countEntities("MonthlyDroneProductivityReportEntity") > 0
                || countEntities("MonthlyProductivityReportEntity") > 0
                || countEntities("TripTelemetryEntity") > 0
                || countEntities("TripOrderEntity") > 0
                || countEntities("TripEntity") > 0
                || countEntities("ObstacleEntity") > 0
                || countEntities("ReviewEntity") > 0
                || countEntities("OrderEntity") > 0
                || countEntities("ClientUserEntity") > 0
                || countEntities("DroneEntity") > 0;
    }

    private long countEntities(String entityName) {
        return entityManager.createQuery("select count(entity) from " + entityName + " entity", Long.class)
                .getSingleResult();
    }

    private List<DroneEntity> seedDrones() {
        DroneEntity chargingDrone = new DroneEntity(
                null,
                "DEMO-CHARLIE",
                6.0,
                60.0,
                DroneStatus.AVAILABLE,
                22.0,
                0.8,
                18.0,
                24.0,
                15.0
        );
        chargingDrone.enqueueForRecharge(DroneRechargeService.INSUFFICIENT_BATTERY_REASON);

        List<DroneEntity> drones = List.of(
                new DroneEntity(null, "DEMO-ALFA", 12.0, 120.0, DroneStatus.AVAILABLE, 100.0, 0.45, 18.0, 24.0, 14.0),
                new DroneEntity(null, "DEMO-BRAVO", 8.0, 80.0, DroneStatus.AVAILABLE, 86.0, 0.55, 18.0, 30.0, 11.0),
                chargingDrone,
                new DroneEntity(null, "DEMO-RESERVA", 10.0, 90.0, DroneStatus.UNAVAILABLE, 100.0, 0.5, 18.0, 36.0, 12.0)
        );

        drones.forEach(entityManager::persist);
        return drones;
    }

    private List<ClientUserEntity> seedClientUsers(Instant now) {
        ClientUserEntity demoClient = new ClientUserEntity(
                null,
                "Cliente Demo",
                DEMO_CLIENT_EMAIL,
                passwordHashingService.hash(DEMO_CLIENT_PASSWORD),
                now
        );

        entityManager.persist(demoClient);
        return List.of(demoClient);
    }

    private List<OrderEntity> seedOrders(ClientUserEntity demoClient, Instant now) {
        List<OrderEntity> orders = List.of(
                order("DEMO-CLIENTE", 6.0, 4.0, 4.0, Priority.HIGH, now, 45, demoClient),
                order("DEMO-URGENTE", 12.0, 7.0, 4.5, Priority.HIGH, now, 55, null),
                order("DEMO-PESADO", -5.0, 10.0, 6.0, Priority.HIGH, now, 65, null),
                order("DEMO-MEDIO", -10.0, -4.0, 3.0, Priority.MEDIUM, now, 80, null),
                order("DEMO-LEVE", 7.0, -8.0, 2.0, Priority.LOW, now, 95, null),
                order("DEMO-CLIENTE-EXCESSO", 20.0, 20.0, 20.0, Priority.HIGH, now, 110, demoClient)
        );

        orders.forEach(entityManager::persist);
        return orders;
    }

    private OrderEntity order(
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority,
            Instant now,
            long deliveryMinutesFromNow,
            ClientUserEntity clientUser
    ) {
        return new OrderEntity(
                null,
                identifier,
                locationX,
                locationY,
                weight,
                priority,
                OrderStatus.REQUESTED,
                now,
                identifier,
                now.plusSeconds(deliveryMinutesFromNow * 60),
                clientUser
        );
    }

    private List<ObstacleEntity> seedObstacles() {
        List<ObstacleEntity> obstacles = List.of(
                new ObstacleEntity(null, 4.0, 2.0, 1.2, true)
        );

        obstacles.forEach(entityManager::persist);
        return obstacles;
    }

    private List<ReviewEntity> seedReviews() {
        List<ReviewEntity> reviews = List.of(
                new ReviewEntity(
                        null,
                        5,
                        "Entrega segura",
                        "Confirmacao por codigo validada no recebimento."
                ),
                new ReviewEntity(
                        null,
                        4,
                        "Rastreamento claro",
                        "Mapa e aviso de aproximacao facilitaram o acompanhamento."
                )
        );

        reviews.forEach(entityManager::persist);
        return reviews;
    }

    public record DemoScenario(
            List<DroneEntity> drones,
            List<OrderEntity> orders,
            List<ObstacleEntity> obstacles,
            List<ReviewEntity> reviews,
            List<ClientUserEntity> clientUsers,
            PersistedTripPlan plan
    ) {
        public DemoScenario {
            drones = List.copyOf(drones);
            orders = List.copyOf(orders);
            obstacles = List.copyOf(obstacles);
            reviews = List.copyOf(reviews);
            clientUsers = List.copyOf(clientUsers);
        }
    }
}
