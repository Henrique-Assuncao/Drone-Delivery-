package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripPlanControllerTest {

    private MockMvc mockMvc;
    private InMemoryDroneStorage droneStorage;
    private InMemoryOrderStorage orderStorage;
    private InMemoryTripStorage tripStorage;
    private InMemoryObstacleStorage obstacleStorage;

    @BeforeEach
    void setUp() {
        droneStorage = new InMemoryDroneStorage();
        orderStorage = new InMemoryOrderStorage();
        tripStorage = new InMemoryTripStorage();
        obstacleStorage = new InMemoryObstacleStorage();

        TripPlanningService planningService = new TripPlanningService(
                droneStorage,
                orderStorage,
                tripStorage,
                obstacleStorage
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new TripPlanController(planningService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreatePlannedTripsFromSavedAvailableDronesAndRequestedOrders() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(
                null,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                100.0,
                1.0,
                20.0,
                2.0,
                10.0
        ));
        OrderEntity order = orderStorage.save(new OrderEntity(null, "ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].id").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(order.getId()))
                .andExpect(jsonPath("$.trips[0].route[0]").value(order.getId()))
                .andExpect(jsonPath("$.trips[0].totalWeight").value(5.0))
                .andExpect(jsonPath("$.trips[0].totalDistance").value(10.0))
                .andExpect(jsonPath("$.trips[0].estimatedDuration").value(5.0))
                .andExpect(jsonPath("$.trips[0].averageDeliveryTime").value(2.5))
                .andExpect(jsonPath("$.trips[0].routeProgress[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.trips[0].routeProgress[0].routePosition").value(0))
                .andExpect(jsonPath("$.trips[0].routeProgress[0].delivered").value(false))
                .andExpect(jsonPath("$.trips[0].routeProgress[0].estimatedDeliveryTime").value(2.5))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        assertEquals(OrderStatus.ALLOCATED, orderStorage.findAll().get(0).getStatus());
        assertEquals(1, tripStorage.findAll().size());
    }

    @Test
    void shouldReturnAutomaticDeliveryOrderAndAverageDeliveryTime() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(
                null,
                "DRONE-1",
                30.0,
                100.0,
                DroneStatus.AVAILABLE,
                100.0,
                1.0,
                20.0,
                2.0,
                10.0
        ));
        OrderEntity lowPriorityOrder = orderStorage.save(new OrderEntity(null, "ORDER-LOW", 1.0, 0.0, 9.0, Priority.LOW, OrderStatus.REQUESTED));
        OrderEntity mediumPriorityOrder = orderStorage.save(new OrderEntity(null, "ORDER-MEDIUM", 2.0, 0.0, 9.0, Priority.MEDIUM, OrderStatus.REQUESTED));
        OrderEntity highLightOrder = orderStorage.save(new OrderEntity(null, "ORDER-HIGH-LIGHT", 10.0, 0.0, 1.0, Priority.HIGH, OrderStatus.REQUESTED));
        OrderEntity highHeavyFarOrder = orderStorage.save(new OrderEntity(null, "ORDER-HIGH-HEAVY-FAR", 8.0, 0.0, 5.0, Priority.HIGH, OrderStatus.REQUESTED));
        OrderEntity highHeavyNearOrder = orderStorage.save(new OrderEntity(null, "ORDER-HIGH-HEAVY-NEAR", 3.0, 0.0, 5.0, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].route[0]").value(highHeavyNearOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[1]").value(highHeavyFarOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[2]").value(highLightOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[3]").value(mediumPriorityOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[4]").value(lowPriorityOrder.getId()))
                .andExpect(jsonPath("$.trips[0].estimatedDuration").value(10.0))
                .andExpect(jsonPath("$.trips[0].averageDeliveryTime").value(5.8))
                .andExpect(jsonPath("$.trips[0].routeProgress[0].estimatedDeliveryTime").value(1.5))
                .andExpect(jsonPath("$.trips[0].routeProgress[1].estimatedDeliveryTime").value(4.0))
                .andExpect(jsonPath("$.trips[0].routeProgress[2].estimatedDeliveryTime").value(5.0))
                .andExpect(jsonPath("$.trips[0].routeProgress[3].estimatedDeliveryTime").value(9.0))
                .andExpect(jsonPath("$.trips[0].routeProgress[4].estimatedDeliveryTime").value(9.5))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));
    }

    @Test
    void shouldRespectDeliveryQueueOrderWhenRouteOptimizationIsDisabled() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 60.0, DroneStatus.AVAILABLE));
        OrderEntity thirdOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-THIRD",
                9.0,
                0.0,
                1.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:02:00Z")
        ));
        OrderEntity firstOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-FIRST",
                10.0,
                0.0,
                1.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:00Z")
        ));
        OrderEntity secondOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-SECOND",
                0.0,
                10.0,
                1.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:01:00Z")
        ));

        mockMvc.perform(post("/api/trip-plans").param("optimizeRoute", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].route[0]").value(firstOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[1]").value(secondOrder.getId()))
                .andExpect(jsonPath("$.trips[0].route[2]").value(thirdOrder.getId()));
    }

    @Test
    void shouldRejectInvalidRouteOptimizationParameter() throws Exception {
        mockMvc.perform(post("/api/trip-plans").param("optimizeRoute", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("optimizeRoute is invalid"));
    }

    @Test
    void shouldUseActiveObstaclesWhenPlanningTrips() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 21.0, DroneStatus.AVAILABLE));
        OrderEntity order = orderStorage.save(new OrderEntity(
                null,
                "ORDER-1",
                10.0,
                0.0,
                5.0,
                Priority.HIGH,
                OrderStatus.REQUESTED
        ));
        obstacleStorage.save(new ObstacleEntity(null, 5.0, 0.0, 1.0, true));
        double adjustedDistance = new RouteDistanceCalculator().roundTripDistanceFromBase(
                new Order("ORDER-1", new Coordinate(10.0, 0.0), 5.0, Priority.HIGH),
                List.of(new Obstacle(new Coordinate(5.0, 0.0), 1.0))
        );

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].route[0]").value(order.getId()))
                .andExpect(jsonPath("$.trips[0].totalDistance").value(closeTo(adjustedDistance, 1.0E-9)))
                .andExpect(jsonPath("$.trips[0].estimatedDuration").value(closeTo(adjustedDistance, 1.0E-9)));
    }

    @Test
    void shouldIgnoreNonAvailableDronesWhenPlanning() throws Exception {
        droneStorage.save(new DroneEntity(null, "DRONE-IN-ROUTE", 10.0, 20.0, DroneStatus.IN_ROUTE));
        OrderEntity order = orderStorage.save(new OrderEntity(null, "ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(1))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderIdentifier").value("ORDER-1"))
                .andExpect(jsonPath("$.unallocatedOrders[0].reason")
                        .value("Pedido não pode ser atendido por nenhum drone no planejamento atual."));

        assertEquals(OrderStatus.UNALLOCATED, orderStorage.findAll().get(0).getStatus());
        assertEquals(
                "Pedido não pode ser atendido por nenhum drone no planejamento atual.",
                orderStorage.findAll().get(0).getStatusReason()
        );
    }

    @Test
    void shouldIgnoreNonRequestedOrdersWhenPlanning() throws Exception {
        droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        orderStorage.save(new OrderEntity(null, "ORDER-ALLOCATED", 3.0, 4.0, 5.0, Priority.HIGH, OrderStatus.ALLOCATED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        assertEquals(OrderStatus.ALLOCATED, orderStorage.findAll().get(0).getStatus());
    }

    @Test
    void shouldPlanPendingReassignmentOrders() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity order = orderStorage.save(new OrderEntity(
                null,
                "ORDER-REASSIGNMENT",
                3.0,
                4.0,
                5.0,
                Priority.HIGH,
                OrderStatus.PENDING_REASSIGNMENT
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(order.getId()))
                .andExpect(jsonPath("$.trips[0].route[0]").value(order.getId()))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        assertEquals(OrderStatus.ALLOCATED, orderStorage.findAll().get(0).getStatus());
    }

    @Test
    void shouldMarkImpossibleRequestedOrderAsUnallocated() throws Exception {
        droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity order = orderStorage.save(new OrderEntity(null, "ORDER-1", 3.0, 4.0, 10.1, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(1))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderIdentifier").value("ORDER-1"))
                .andExpect(jsonPath("$.unallocatedOrders[0].reason")
                        .value("Pedido excede a capacidade máxima de peso dos drones disponíveis."));

        assertEquals(OrderStatus.UNALLOCATED, orderStorage.findAll().get(0).getStatus());
        assertEquals(
                "Pedido excede a capacidade máxima de peso dos drones disponíveis.",
                orderStorage.findAll().get(0).getStatusReason()
        );
    }

    @Test
    void shouldMarkRequestedOrderAsUnallocatedWhenDroneBatteryIsInsufficient() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(
                null,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                29.9,
                1.0,
                20.0,
                1.0,
                10.0
        ));
        OrderEntity order = orderStorage.save(new OrderEntity(null, "ORDER-1", 3.0, 4.0, 5.0, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(1))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderIdentifier").value("ORDER-1"))
                .andExpect(jsonPath("$.unallocatedOrders[0].reason")
                        .value("Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança."));

        assertEquals(OrderStatus.UNALLOCATED, orderStorage.findAll().get(0).getStatus());
        assertEquals(
                "Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança.",
                orderStorage.findAll().get(0).getStatusReason()
        );
        assertEquals(DroneStatus.CHARGING, drone.getStatus());
        assertEquals("drone battery is insufficient for requested orders", drone.getRechargeReason());
        org.junit.jupiter.api.Assertions.assertNotNull(drone.getRechargeQueuedAt());
    }

    private static class InMemoryDroneStorage implements DroneStorage {

        private final Map<Long, DroneEntity> dronesById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public boolean existsByIdentifier(String identifier) {
            return dronesById.values().stream()
                    .anyMatch(drone -> drone.getIdentifier().equals(identifier));
        }

        @Override
        public List<DroneEntity> findAll() {
            return new ArrayList<>(dronesById.values());
        }

        @Override
        public Optional<DroneEntity> findById(Long id) {
            return Optional.ofNullable(dronesById.get(id));
        }

        @Override
        public List<DroneEntity> findByStatus(DroneStatus status) {
            return dronesById.values().stream()
                    .filter(drone -> drone.getStatus() == status)
                    .toList();
        }

        @Override
        public List<DroneEntity> findRechargeQueue() {
            return dronesById.values().stream()
                    .filter(drone -> drone.getStatus() == DroneStatus.CHARGING)
                    .toList();
        }

        @Override
        public DroneEntity save(DroneEntity drone) {
            DroneEntity savedDrone = new DroneEntity(
                    nextId++,
                    drone.getIdentifier(),
                    drone.getMaxWeightCapacity(),
                    drone.getMaxRange(),
                    drone.getStatus(),
                    drone.getBatteryLevel(),
                    drone.getBatteryConsumptionPerDistanceUnit(),
                    drone.getMinimumReturnBattery(),
                    drone.getSpeed(),
                    drone.getChargingRate()
            );

            dronesById.put(savedDrone.getId(), savedDrone);

            return savedDrone;
        }
    }

    private static class InMemoryOrderStorage implements OrderStorage {

        private final Map<Long, OrderEntity> ordersById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public boolean existsByIdentifier(String identifier) {
            return ordersById.values().stream()
                    .anyMatch(order -> order.getIdentifier().equals(identifier));
        }

        @Override
        public List<OrderEntity> findAll() {
            return new ArrayList<>(ordersById.values());
        }

        @Override
        public Optional<OrderEntity> findById(Long id) {
            return Optional.ofNullable(ordersById.get(id));
        }

        @Override
        public List<OrderEntity> findByStatus(OrderStatus status) {
            return ordersById.values().stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();
        }

        @Override
        public List<OrderEntity> findByClientUserId(Long clientUserId) {
            return ordersById.values().stream()
                    .filter(order -> order.getClientUser() != null && order.getClientUser().getId().equals(clientUserId))
                    .toList();
        }

        @Override
        public List<OrderEntity> findDeliveryQueue() {
            return ordersById.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.REQUESTED
                            || order.getStatus() == OrderStatus.PENDING_REASSIGNMENT)
                    .sorted(Comparator.comparing(OrderEntity::getQueuedAt)
                            .thenComparing(OrderEntity::getId))
                    .toList();
        }

        @Override
        public OrderEntity save(OrderEntity order) {
            OrderEntity savedOrder = new OrderEntity(
                    nextId++,
                    order.getIdentifier(),
                    order.getLocationX(),
                    order.getLocationY(),
                    order.getWeight(),
                    order.getPriority(),
                    order.getStatus(),
                    order.getQueuedAt(),
                    order.getDeliveryConfirmationCode(),
                    order.getConfirmedDeliveryTime()
            );
            savedOrder.changeStatus(savedOrder.getStatus(), order.getStatusReason());

            ordersById.put(savedOrder.getId(), savedOrder);

            return savedOrder;
        }
    }

    private static class InMemoryTripStorage implements TripStorage {

        private final Map<Long, TripEntity> tripsById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public List<TripEntity> findAll() {
            return new ArrayList<>(tripsById.values());
        }

        @Override
        public List<TripEntity> findByStatus(TripStatus status) {
            return tripsById.values().stream()
                    .filter(trip -> trip.getStatus() == status)
                    .toList();
        }

        @Override
        public Optional<TripEntity> findById(Long id) {
            return Optional.ofNullable(tripsById.get(id));
        }

        @Override
        public TripEntity save(TripEntity trip) {
            TripEntity savedTrip = new TripEntity(
                    nextId++,
                    trip.getDrone(),
                    trip.getStatus(),
                    trip.getTotalWeight(),
                    trip.getTotalDistance()
            );

            for (TripOrderEntity tripOrder : trip.getTripOrders()) {
                savedTrip.addOrder(
                        tripOrder.getOrder(),
                        tripOrder.getRoutePosition(),
                        tripOrder.getDeliveredAt(),
                        tripOrder.getEstimatedDeliveryTime()
                );
            }

            tripsById.put(savedTrip.getId(), savedTrip);

            return savedTrip;
        }
    }

    private static class InMemoryObstacleStorage implements ObstacleStorage {

        private final Map<Long, ObstacleEntity> obstaclesById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public List<ObstacleEntity> findAll() {
            return new ArrayList<>(obstaclesById.values());
        }

        @Override
        public List<ObstacleEntity> findActive() {
            return obstaclesById.values().stream()
                    .filter(ObstacleEntity::isActive)
                    .toList();
        }

        @Override
        public Optional<ObstacleEntity> findById(Long id) {
            return Optional.ofNullable(obstaclesById.get(id));
        }

        @Override
        public ObstacleEntity save(ObstacleEntity obstacle) {
            ObstacleEntity savedObstacle = new ObstacleEntity(
                    nextId++,
                    obstacle.getCenterX(),
                    obstacle.getCenterY(),
                    obstacle.getRadius(),
                    obstacle.isActive()
            );

            obstaclesById.put(savedObstacle.getId(), savedObstacle);

            return savedObstacle;
        }
    }
}
