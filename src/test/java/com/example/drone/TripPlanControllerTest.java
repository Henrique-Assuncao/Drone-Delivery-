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
                120.0,
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
    void shouldAllocateOverflowToAnotherAvailableDroneImmediately() throws Exception {
        DroneEntity firstDrone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        DroneEntity secondDrone = droneStorage.save(new DroneEntity(null, "DRONE-2", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity firstOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-1",
                1.0,
                1.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:00Z"),
                "ORDER-1",
                Instant.parse("2026-07-26T18:00:00Z")
        ));
        OrderEntity secondOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-2",
                2.0,
                2.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:01:00Z"),
                "ORDER-2",
                Instant.parse("2026-07-26T18:01:00Z")
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(2))
                .andExpect(jsonPath("$.trips[0].droneId").value(firstDrone.getId()))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(firstOrder.getId()))
                .andExpect(jsonPath("$.trips[1].droneId").value(secondDrone.getId()))
                .andExpect(jsonPath("$.trips[1].orders[0]").value(secondOrder.getId()))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        assertEquals(OrderStatus.ALLOCATED, orderStorage.findById(firstOrder.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.ALLOCATED, orderStorage.findById(secondOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldMarkOverflowAsUnallocatedWhenNoImmediateDroneIsAvailable() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity firstOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-1",
                1.0,
                1.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:00Z"),
                "ORDER-1",
                Instant.parse("2026-07-26T18:00:00Z")
        ));
        OrderEntity secondOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-2",
                2.0,
                2.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:01:00Z"),
                "ORDER-2",
                Instant.parse("2026-07-26T18:01:00Z")
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(firstOrder.getId()))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(1))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderId").value(secondOrder.getId()))
                .andExpect(jsonPath("$.unallocatedOrders[0].reason")
                        .value("Pedido exige outro drone imediato, mas não há drone disponível nesta rodada de planejamento."));

        assertEquals(OrderStatus.ALLOCATED, orderStorage.findById(firstOrder.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.UNALLOCATED, orderStorage.findById(secondOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldAddOrderToExistingPlannedTripBeforeIdealDispatchTimeWhenCapacityAllows() throws Exception {
        DroneEntity drone = droneStorage.save(new DroneEntity(null, "DRONE-1", 10.0, 100.0, DroneStatus.AVAILABLE));
        Instant futureDeliveryTime = Instant.now().plusSeconds(3_600);
        OrderEntity plannedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-PLANNED",
                3.0,
                4.0,
                4.0,
                Priority.HIGH,
                OrderStatus.ALLOCATED,
                Instant.parse("2026-07-25T10:00:00Z"),
                "ORDER-PLANNED",
                futureDeliveryTime
        ));
        TripEntity plannedTrip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        plannedTrip.addOrder(plannedOrder, 0, null, 10.0);
        TripEntity savedPlannedTrip = tripStorage.save(plannedTrip);
        OrderEntity requestedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-REQUESTED",
                6.0,
                8.0,
                5.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:01:00Z"),
                "ORDER-REQUESTED",
                futureDeliveryTime.plusSeconds(600)
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].id").value(savedPlannedTrip.getId()))
                .andExpect(jsonPath("$.trips[0].droneId").value(drone.getId()))
                .andExpect(jsonPath("$.trips[0].orders.length()").value(2))
                .andExpect(jsonPath("$.trips[0].totalWeight").value(9.0))
                .andExpect(jsonPath("$.trips[0].dispatchWindowOpen").value(false))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        TripEntity updatedTrip = tripStorage.findById(savedPlannedTrip.getId()).orElseThrow();
        assertEquals(1, tripStorage.findAll().size());
        assertEquals(2, updatedTrip.getTripOrders().size());
        assertEquals(9.0, updatedTrip.getTotalWeight());
        assertEquals(OrderStatus.ALLOCATED, orderStorage.findById(requestedOrder.getId()).orElseThrow().getStatus());
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
                120.0,
                10.0
        ));
        Instant confirmedDeliveryTime = Instant.parse("2026-07-26T18:00:00Z");
        OrderEntity lowPriorityOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-LOW",
                1.0,
                0.0,
                9.0,
                Priority.LOW,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:00Z"),
                "ORDER-LOW",
                confirmedDeliveryTime
        ));
        OrderEntity mediumPriorityOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-MEDIUM",
                2.0,
                0.0,
                9.0,
                Priority.MEDIUM,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:01Z"),
                "ORDER-MEDIUM",
                confirmedDeliveryTime
        ));
        OrderEntity highLightOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-HIGH-LIGHT",
                10.0,
                0.0,
                1.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:02Z"),
                "ORDER-HIGH-LIGHT",
                confirmedDeliveryTime
        ));
        OrderEntity highHeavyFarOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-HIGH-HEAVY-FAR",
                8.0,
                0.0,
                5.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:03Z"),
                "ORDER-HIGH-HEAVY-FAR",
                confirmedDeliveryTime
        ));
        OrderEntity highHeavyNearOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-HIGH-HEAVY-NEAR",
                3.0,
                0.0,
                5.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:04Z"),
                "ORDER-HIGH-HEAVY-NEAR",
                confirmedDeliveryTime
        ));

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
    void shouldUseAnotherDroneWhenAvailableDroneAlreadyHasPlannedTrip() throws Exception {
        DroneEntity reservedDrone = droneStorage.save(new DroneEntity(null, "DRONE-RESERVED", 10.0, 20.0, DroneStatus.AVAILABLE));
        DroneEntity freeDrone = droneStorage.save(new DroneEntity(null, "DRONE-FREE", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity reservedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-RESERVED",
                1.0,
                1.0,
                8.0,
                Priority.HIGH,
                OrderStatus.ALLOCATED
        ));
        TripEntity reservedTrip = new TripEntity(null, reservedDrone, TripStatus.PLANNED, 8.0, 4.0);
        reservedTrip.addOrder(reservedOrder, 0, null, 1.0);
        tripStorage.save(reservedTrip);
        OrderEntity requestedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-REQUESTED",
                2.0,
                2.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(freeDrone.getId()))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(requestedOrder.getId()))
                .andExpect(jsonPath("$.trips[0].totalWeight").value(8.0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        assertEquals(2, tripStorage.findAll().size());
        assertEquals(OrderStatus.ALLOCATED, orderStorage.findById(requestedOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldMarkOrderAsUnallocatedWhenOnlyAvailableDroneAlreadyHasPlannedTrip() throws Exception {
        DroneEntity reservedDrone = droneStorage.save(new DroneEntity(null, "DRONE-RESERVED", 10.0, 20.0, DroneStatus.AVAILABLE));
        OrderEntity reservedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-RESERVED",
                1.0,
                1.0,
                8.0,
                Priority.HIGH,
                OrderStatus.ALLOCATED
        ));
        TripEntity reservedTrip = new TripEntity(null, reservedDrone, TripStatus.PLANNED, 8.0, 4.0);
        reservedTrip.addOrder(reservedOrder, 0, null, 1.0);
        tripStorage.save(reservedTrip);
        OrderEntity requestedOrder = orderStorage.save(new OrderEntity(
                null,
                "ORDER-REQUESTED",
                2.0,
                2.0,
                8.0,
                Priority.HIGH,
                OrderStatus.REQUESTED
        ));

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(1))
                .andExpect(jsonPath("$.unallocatedOrders[0].orderId").value(requestedOrder.getId()))
                .andExpect(jsonPath("$.unallocatedOrders[0].reason")
                        .value("Pedido não pode ser atendido por nenhum drone no planejamento atual."));

        assertEquals(OrderStatus.UNALLOCATED, orderStorage.findById(requestedOrder.getId()).orElseThrow().getStatus());
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
                    .sorted(Comparator.comparing(OrderEntity::getConfirmedDeliveryTime)
                            .thenComparing(Comparator.comparingInt((OrderEntity order) -> priorityRank(order.getPriority())).reversed())
                            .thenComparing(OrderEntity::getQueuedAt)
                            .thenComparing(OrderEntity::getId))
                    .toList();
        }

        private int priorityRank(Priority priority) {
            return switch (priority) {
                case HIGH -> 3;
                case MEDIUM -> 2;
                case LOW -> 1;
            };
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
            Long id = trip.getId() == null ? nextId++ : trip.getId();
            TripEntity savedTrip = new TripEntity(
                    id,
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

            tripsById.put(id, savedTrip);

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
