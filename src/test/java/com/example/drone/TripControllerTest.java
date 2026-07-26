package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripControllerTest {

    private MockMvc mockMvc;
    private InMemoryTripStorage storage;
    private InMemoryTripTelemetryStorage telemetryStorage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryTripStorage();
        telemetryStorage = new InMemoryTripTelemetryStorage();
        TripQueryService queryService = new TripQueryService(storage);
        TripTelemetryService telemetryService = new TripTelemetryService(storage, telemetryStorage);
        TripTransitionService transitionService = new TripTransitionService(storage, telemetryService);

        mockMvc = MockMvcBuilders.standaloneSetup(new TripController(queryService, transitionService, telemetryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldListEmptyTrips() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldListSavedTrips() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                100.0,
                1.0,
                20.0,
                120.0,
                10.0
        );
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 5.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 9.0, 20.0);
        trip.addOrder(secondOrder, 0);
        trip.addOrder(firstOrder, 1);
        storage.save(trip);

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].droneId").value(1))
                .andExpect(jsonPath("$[0].status").value("PLANNED"))
                .andExpect(jsonPath("$[0].orders[0]").value(2))
                .andExpect(jsonPath("$[0].orders[1]").value(1))
                .andExpect(jsonPath("$[0].route[0]").value(2))
                .andExpect(jsonPath("$[0].route[1]").value(1))
                .andExpect(jsonPath("$[0].routeProgress[0].orderId").value(2))
                .andExpect(jsonPath("$[0].routeProgress[0].routePosition").value(0))
                .andExpect(jsonPath("$[0].routeProgress[0].delivered").value(false))
                .andExpect(jsonPath("$[0].routeProgress[1].orderId").value(1))
                .andExpect(jsonPath("$[0].routeProgress[1].routePosition").value(1))
                .andExpect(jsonPath("$[0].routeProgress[1].delivered").value(false))
                .andExpect(jsonPath("$[0].totalWeight").value(9.0))
                .andExpect(jsonPath("$[0].totalDistance").value(20.0))
                .andExpect(jsonPath("$[0].estimatedDuration").value(10.0));
    }

    @Test
    void shouldListTripsFilteredByStatus() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        storage.save(new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0));
        storage.save(new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0));
        storage.save(new TripEntity(null, drone, TripStatus.CANCELLED, 4.0, 10.0));

        mockMvc.perform(get("/api/trips").param("status", "IN_ROUTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].droneId").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_ROUTE"));
    }

    @Test
    void shouldRejectInvalidTripStatusFilter() throws Exception {
        mockMvc.perform(get("/api/trips").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be one of PLANNED, IN_ROUTE, RETURNED_EARLY, COMPLETED, CANCELLED"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldFindTripById() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(get("/api/trips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.routeProgress[0].orderId").value(1))
                .andExpect(jsonPath("$.routeProgress[0].routePosition").value(0))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(false))
                .andExpect(jsonPath("$.totalWeight").value(4.0))
                .andExpect(jsonPath("$.totalDistance").value(10.0))
                .andExpect(jsonPath("$.estimatedDuration").value(10.0));
    }

    @Test
    void shouldReturnNotFoundWhenTripDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/trips/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldStartPlannedTrip() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        TripEntity savedTrip = storage.save(trip);

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("IN_ROUTE"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.totalWeight").value(4.0))
                .andExpect(jsonPath("$.totalDistance").value(10.0));

        TripEntity startedTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.IN_ROUTE, startedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.IN_ROUTE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
    }

    @Test
    void shouldReturnNotFoundWhenStartingUnknownTrip() throws Exception {
        mockMvc.perform(post("/api/trips/999/start"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectStartingTripThatIsNotPlanned() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be PLANNED to start"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectStartingTripWhenDroneIsNotAvailable() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        TripEntity savedTrip = storage.save(trip);

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be AVAILABLE to start trip"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectStartingTripWhenDroneBatteryIsInsufficient() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                29.9,
                1.0,
                20.0,
                1.0,
                10.0
        );
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        TripEntity savedTrip = storage.save(trip);

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone battery is insufficient for complete trip and safe return"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.PLANNED, savedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.ALLOCATED, order.getStatus());
    }

    @Test
    void shouldCompleteTripInRoute() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.DELIVERED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0, Instant.parse("2026-07-25T20:00:00Z"));
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.routeProgress[0].orderId").value(1))
                .andExpect(jsonPath("$.routeProgress[0].routePosition").value(0))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true))
                .andExpect(jsonPath("$.routeProgress[0].deliveredAt").exists())
                .andExpect(jsonPath("$.totalWeight").value(4.0))
                .andExpect(jsonPath("$.totalDistance").value(10.0));

        TripEntity completedTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.COMPLETED, completedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.DELIVERED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(completedTrip.getTripOrders().get(0).isDelivered());
    }

    @Test
    void shouldRejectCompletingTripWithUnconfirmedDelivery() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("all route positions must be resolved before completing trip"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.IN_ROUTE, trip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
    }

    @Test
    void shouldReportDeliveredRoutePosition() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 20.0);
        trip.addOrder(firstOrder, 0);
        trip.addOrder(secondOrder, 1);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_ROUTE"))
                .andExpect(jsonPath("$.routeProgress[0].orderId").value(1))
                .andExpect(jsonPath("$.routeProgress[0].routePosition").value(0))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true))
                .andExpect(jsonPath("$.routeProgress[0].deliveredAt").exists())
                .andExpect(jsonPath("$.routeProgress[1].orderId").value(2))
                .andExpect(jsonPath("$.routeProgress[1].routePosition").value(1))
                .andExpect(jsonPath("$.routeProgress[1].delivered").value(false));

        TripEntity inRouteTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.IN_ROUTE, inRouteTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.IN_ROUTE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.DELIVERED, firstOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, secondOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(inRouteTrip.getTripOrders().get(0).getDeliveredAt());
        org.junit.jupiter.api.Assertions.assertFalse(inRouteTrip.getTripOrders().get(1).isDelivered());
    }

    @Test
    void shouldConfirmRouteAvailability() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        trip.getTripOrders().get(0).markAvailabilityNotified(Instant.now());
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ROUTE"))
                .andExpect(jsonPath("$.routeProgress[0].availabilityNotifiedAt").exists())
                .andExpect(jsonPath("$.routeProgress[0].availabilityConfirmedAt").exists())
                .andExpect(jsonPath("$.routeProgress[0].availabilityResponseDeadline").exists());
    }

    @Test
    void shouldReturnToBaseWithNotDeliveredPackageWhenClientDeclinesAvailability() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        trip.getTripOrders().get(0).markAvailabilityNotified(Instant.now());
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_EARLY"))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(false));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.NOT_DELIVERED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Cliente informou que não está disponível para receber o pacote.",
                order.getStatusReason()
        );
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, drone.getStatus());
    }

    @Test
    void shouldRejectDeliveryWithoutAvailabilityConfirmation() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("delivery availability must be confirmed before delivery"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectDeliveringRoutePositionOutOfOrder() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 20.0);
        trip.addOrder(firstOrder, 0);
        trip.addOrder(secondOrder, 1);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/1/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("previous route positions must be delivered first"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, firstOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, secondOrder.getStatus());
    }

    @Test
    void shouldRejectDeliveryWithInvalidConfirmationCode() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("WRONG1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("delivery confirmation code is invalid"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
    }

    @Test
    void shouldRejectDeliveryWhenConfirmationWindowExpired() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        TripOrderEntity routeOrder = trip.getTripOrders().get(0);
        routeOrder.markAvailabilityConfirmed(Instant.now().minusSeconds(90));
        routeOrder.markDeliveryConfirmationRequested(Instant.now().minusSeconds(61));
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("delivery confirmation window expired"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
    }

    @Test
    void shouldRejectDeliveryWithoutConfirmationRequestBody() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request body must not be null"))
                .andExpect(content().string(not(containsString("trace"))));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
    }

    @Test
    void shouldRejectDeliveryForTripThatIsNotInRoute() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be IN_ROUTE to report delivery"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectDeliveryWithNegativeRoutePosition() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/-1/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("routePosition must not be negative"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnNotFoundWhenDeliveringUnknownRoutePosition() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/1/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip route position not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnEarlyWhenBatteryCannotCompleteRemainingRouteSafely() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.IN_ROUTE,
                37.0,
                1.0,
                20.0,
                1.0,
                10.0
        );
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 20.0);
        trip.addOrder(firstOrder, 0);
        trip.addOrder(secondOrder, 1);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true));

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED_EARLY"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.orders[1]").value(2))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.route[1]").value(2))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true))
                .andExpect(jsonPath("$.routeProgress[1].delivered").value(false));

        TripEntity returnedTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.RETURNED_EARLY, returnedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.CHARGING, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(27.0, drone.getBatteryLevel(), 1.0E-9);
        org.junit.jupiter.api.Assertions.assertEquals(
                "drone returned early to preserve minimum return battery",
                drone.getRechargeReason()
        );
        org.junit.jupiter.api.Assertions.assertNotNull(drone.getRechargeQueuedAt());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.DELIVERED, firstOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PENDING_REASSIGNMENT, secondOrder.getStatus());
    }

    @Test
    void shouldReturnEarlyWithoutInferringUnreportedDeliveries() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.IN_ROUTE,
                37.0,
                1.0,
                20.0,
                1.0,
                10.0
        );
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 20.0);
        trip.addOrder(firstOrder, 0);
        trip.addOrder(secondOrder, 1);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_EARLY"))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(false))
                .andExpect(jsonPath("$.routeProgress[1].delivered").value(false));

        TripEntity returnedTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.RETURNED_EARLY, returnedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.CHARGING, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(37.0, drone.getBatteryLevel(), 1.0E-9);
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PENDING_REASSIGNMENT, firstOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PENDING_REASSIGNMENT, secondOrder.getStatus());
    }

    @Test
    void shouldUpdateTelemetryForInRouteTripWithoutReturningEarly() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.IN_ROUTE,
                100.0,
                1.0,
                20.0,
                1.0,
                10.0
        );
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batteryLevel": 35.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("IN_ROUTE"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.route[0]").value(1));

        TripEntity inRouteTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.IN_ROUTE, inRouteTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.IN_ROUTE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(35.0, drone.getBatteryLevel(), 1.0E-9);
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(1, telemetryStorage.findByTripId(1L).size());
        org.junit.jupiter.api.Assertions.assertEquals(
                35.0,
                telemetryStorage.findByTripId(1L).get(0).getBatteryLevel(),
                1.0E-9
        );
    }

    @Test
    void shouldReturnEarlyWhenTelemetryBatteryCannotCompleteRouteSafely() throws Exception {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.IN_ROUTE,
                100.0,
                1.0,
                20.0,
                1.0,
                10.0
        );
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 6.0, 8.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 20.0);
        trip.addOrder(firstOrder, 0);
        trip.addOrder(secondOrder, 1);
        confirmAvailability(trip, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryConfirmationRequest("ORDER-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true));

        mockMvc.perform(post("/api/trips/1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batteryLevel": 37.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED_EARLY"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.orders[1]").value(2))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.route[1]").value(2))
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true))
                .andExpect(jsonPath("$.routeProgress[1].delivered").value(false));

        TripEntity returnedTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.RETURNED_EARLY, returnedTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.CHARGING, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(27.0, drone.getBatteryLevel(), 1.0E-9);
        org.junit.jupiter.api.Assertions.assertEquals(
                "drone returned early to preserve minimum return battery",
                drone.getRechargeReason()
        );
        org.junit.jupiter.api.Assertions.assertEquals(37.0, telemetryStorage.findByTripId(1L).get(0).getBatteryLevel(), 1.0E-9);
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.DELIVERED, firstOrder.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PENDING_REASSIGNMENT, secondOrder.getStatus());
    }

    @Test
    void shouldListTripTelemetryHistory() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        TripEntity savedTrip = storage.save(trip);
        telemetryStorage.save(new TripTelemetryEntity(
                null,
                savedTrip,
                70.0,
                Instant.parse("2026-07-25T20:00:00Z")
        ));
        telemetryStorage.save(new TripTelemetryEntity(
                null,
                savedTrip,
                65.0,
                Instant.parse("2026-07-25T20:01:00Z")
        ));

        mockMvc.perform(get("/api/trips/1/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tripId").value(1))
                .andExpect(jsonPath("$[0].batteryLevel").value(70.0))
                .andExpect(jsonPath("$[0].reportedAt").value("2026-07-25T20:00:00Z"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].tripId").value(1))
                .andExpect(jsonPath("$[1].batteryLevel").value(65.0))
                .andExpect(jsonPath("$[1].reportedAt").value("2026-07-25T20:01:00Z"));
    }

    @Test
    void shouldReturnNotFoundWhenListingTelemetryForUnknownTrip() throws Exception {
        mockMvc.perform(get("/api/trips/999/telemetry"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectTelemetryForTripThatIsNotInRoute() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batteryLevel": 35.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be IN_ROUTE to report telemetry"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectTelemetryWithInvalidBatteryLevel() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batteryLevel": 100.1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("batteryLevel must be between 0 and 100"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnNotFoundWhenCompletingUnknownTrip() throws Exception {
        mockMvc.perform(post("/api/trips/999/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectCompletingTripThatIsNotInRoute() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be IN_ROUTE to complete"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldCancelPlannedTrip() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.PLANNED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.droneId").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.route[0]").value(1))
                .andExpect(jsonPath("$.totalWeight").value(4.0))
                .andExpect(jsonPath("$.totalDistance").value(10.0));

        TripEntity cancelledTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.CANCELLED, cancelledTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.REQUESTED, order.getStatus());
    }

    @Test
    void shouldCancelTripInRoute() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        TripEntity cancelledTrip = storage.findAll().get(0);

        org.junit.jupiter.api.Assertions.assertEquals(TripStatus.CANCELLED, cancelledTrip.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, drone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.REQUESTED, order.getStatus());
    }

    @Test
    void shouldReturnNotFoundWhenCancellingUnknownTrip() throws Exception {
        mockMvc.perform(post("/api/trips/999/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("trip not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectCancellingCompletedTrip() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.DELIVERED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.COMPLETED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be PLANNED or IN_ROUTE to cancel"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledTrip() throws Exception {
        DroneEntity drone = new DroneEntity(1L, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE);
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.REQUESTED);
        TripEntity trip = new TripEntity(null, drone, TripStatus.CANCELLED, 4.0, 10.0);
        trip.addOrder(order, 0);
        storage.save(trip);

        mockMvc.perform(post("/api/trips/1/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trip must be PLANNED or IN_ROUTE to cancel"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    private String deliveryConfirmationRequest(String confirmationCode) {
        return """
                {
                  "confirmationCode": "%s"
                }
                """.formatted(confirmationCode);
    }

    private void confirmAvailability(TripEntity trip, int routePosition) {
        TripOrderEntity tripOrder = trip.getTripOrders().stream()
                .filter(candidate -> candidate.getRoutePosition() == routePosition)
                .findFirst()
                .orElseThrow();
        Instant now = Instant.now();
        tripOrder.markAvailabilityNotified(now);
        tripOrder.markAvailabilityConfirmed(now);
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
                        tripOrder.getEstimatedDeliveryTime(),
                        tripOrder.getAvailabilityNotifiedAt(),
                        tripOrder.getAvailabilityConfirmedAt(),
                        tripOrder.getDeliveryConfirmationRequestedAt(),
                        tripOrder.getDeliveryFailedAt(),
                        tripOrder.getDeliveryFailureReason()
                );
            }

            tripsById.put(savedTrip.getId(), savedTrip);

            return savedTrip;
        }
    }

    private static class InMemoryTripTelemetryStorage implements TripTelemetryStorage {

        private final Map<Long, TripTelemetryEntity> telemetryById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public List<TripTelemetryEntity> findByTripId(Long tripId) {
            return telemetryById.values().stream()
                    .filter(telemetry -> telemetry.getTrip().getId().equals(tripId))
                    .sorted((firstTelemetry, secondTelemetry) -> {
                        int reportedAtComparison = firstTelemetry.getReportedAt()
                                .compareTo(secondTelemetry.getReportedAt());
                        if (reportedAtComparison != 0) {
                            return reportedAtComparison;
                        }

                        return firstTelemetry.getId().compareTo(secondTelemetry.getId());
                    })
                    .toList();
        }

        @Override
        public TripTelemetryEntity save(TripTelemetryEntity telemetry) {
            TripTelemetryEntity savedTelemetry = new TripTelemetryEntity(
                    nextId++,
                    telemetry.getTrip(),
                    telemetry.getBatteryLevel(),
                    telemetry.getReportedAt()
            );

            telemetryById.put(savedTelemetry.getId(), savedTelemetry);

            return savedTelemetry;
        }
    }
}
