package com.example.drone;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripSimulationServiceTest {

    @Test
    void shouldStartPlannedTripAndWaitForDeliveryConfirmationAtReachedOrder() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(oneOrderTrip(TripStatus.PLANNED));
        TripSimulationService service = new TripSimulationService(storage);

        TripSimulationState state = service.advance(trip.getId(), 1.0);

        TripEntity updatedTrip = storage.findById(trip.getId()).orElseThrow();
        TripOrderEntity routeOrder = updatedTrip.getTripOrders().get(0);

        assertEquals(TripStatus.IN_ROUTE, updatedTrip.getStatus());
        assertEquals(DroneStatus.IN_ROUTE, updatedTrip.getDrone().getStatus());
        assertEquals(OrderStatus.IN_ROUTE, routeOrder.getOrder().getStatus());
        assertFalse(routeOrder.isDelivered());
        assertNotNull(routeOrder.getAvailabilityNotifiedAt());
        assertEquals(10.0, state.travelledDistance());
        assertEquals(0.5, state.progress());
        assertFalse(state.moving());
        assertEquals(90.0, updatedTrip.getDrone().getBatteryLevel());
        assertNotNull(state.updatedAt());
    }

    @Test
    void shouldCompleteTripWhenDroneFinishesRoute() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(oneOrderTrip(TripStatus.PLANNED));
        TripSimulationService service = new TripSimulationService(storage);

        service.advance(trip.getId(), 1.0);
        TripEntity waitingTrip = storage.findById(trip.getId()).orElseThrow();
        TripOrderEntity routeOrder = waitingTrip.getTripOrders().get(0);
        routeOrder.markDelivered();
        routeOrder.getOrder().changeStatus(OrderStatus.DELIVERED);

        TripSimulationState state = service.advance(trip.getId(), 1.0);

        TripEntity updatedTrip = storage.findById(trip.getId()).orElseThrow();

        assertEquals(TripStatus.COMPLETED, updatedTrip.getStatus());
        assertEquals(DroneStatus.AVAILABLE, updatedTrip.getDrone().getStatus());
        assertEquals(OrderStatus.DELIVERED, updatedTrip.getTripOrders().get(0).getOrder().getStatus());
        assertEquals(20.0, state.travelledDistance());
        assertEquals(1.0, state.progress());
        assertEquals(new Coordinate(0.0, 0.0), state.currentLocation());
        assertEquals(80.0, updatedTrip.getDrone().getBatteryLevel());
    }

    @Test
    void shouldReturnToBaseWithNotDeliveredPackageWhenAvailabilityResponseExpires() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(oneOrderTrip(TripStatus.IN_ROUTE));
        TripOrderEntity routeOrder = trip.getTripOrders().get(0);
        routeOrder.markAvailabilityNotified(Instant.now().minusSeconds(31));
        trip.updateSimulationState(10.0, 0.0, 10.0, Instant.now().minusSeconds(31));
        TripSimulationService service = new TripSimulationService(storage);

        TripSimulationState state = service.advance(trip.getId(), 1.0);

        TripEntity updatedTrip = storage.findById(trip.getId()).orElseThrow();
        OrderEntity order = updatedTrip.getTripOrders().get(0).getOrder();

        assertEquals(TripStatus.RETURNED_EARLY, updatedTrip.getStatus());
        assertEquals(DroneStatus.AVAILABLE, updatedTrip.getDrone().getStatus());
        assertEquals(OrderStatus.NOT_DELIVERED, order.getStatus());
        assertEquals("Cliente não confirmou disponibilidade para receber o pacote.", order.getStatusReason());
        assertEquals(new Coordinate(0.0, 0.0), state.currentLocation());
        assertFalse(state.moving());
    }

    @Test
    void shouldMarkPackageNotDeliveredAndContinueRouteWhenDeliveryConfirmationExpires() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(twoOrderTrip());
        List<TripOrderEntity> routeOrders = trip.getTripOrders();
        TripOrderEntity firstRouteOrder = routeOrders.get(0);
        TripOrderEntity secondRouteOrder = routeOrders.get(1);
        Instant now = Instant.now();
        firstRouteOrder.markAvailabilityNotified(now.minusSeconds(90));
        firstRouteOrder.markAvailabilityConfirmed(now.minusSeconds(90));
        firstRouteOrder.markDeliveryConfirmationRequested(now.minusSeconds(61));
        trip.updateSimulationState(10.0, 0.0, 10.0, now.minusSeconds(61));
        TripSimulationService service = new TripSimulationService(storage);

        TripSimulationState state = service.advance(trip.getId(), 0.5);

        TripEntity updatedTrip = storage.findById(trip.getId()).orElseThrow();
        TripOrderEntity failedRouteOrder = updatedTrip.getTripOrders().get(0);
        OrderEntity failedOrder = failedRouteOrder.getOrder();

        assertEquals(TripStatus.IN_ROUTE, updatedTrip.getStatus());
        assertTrue(failedRouteOrder.isDeliveryFailed());
        assertEquals(OrderStatus.NOT_DELIVERED, failedOrder.getStatus());
        assertEquals(
                "Cliente não informou o código de confirmação no prazo. Drone seguiu a rota e retornará o pacote à base.",
                failedOrder.getStatusReason()
        );
        assertEquals(secondRouteOrder.getOrder().getId(), state.nextOrderId());
        assertEquals(secondRouteOrder.getRoutePosition(), state.nextRoutePosition());
        assertEquals(15.0, state.travelledDistance());
        assertTrue(state.moving());
    }

    @Test
    void shouldRejectInvalidElapsedMinutes() {
        TripSimulationService service = new TripSimulationService(new InMemoryTripStorage());

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> service.advance(1L, 0.0)
        );

        assertEquals("elapsedMinutes must be greater than zero", exception.getMessage());
    }

    private TripEntity oneOrderTrip(TripStatus status) {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                30.0,
                status == TripStatus.PLANNED ? DroneStatus.AVAILABLE : DroneStatus.IN_ROUTE,
                100.0,
                1.0,
                20.0,
                10.0,
                10.0
        );
        OrderEntity order = new OrderEntity(1L, "ORDER-1", 10.0, 0.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
        TripEntity trip = new TripEntity(null, drone, status, 4.0, 20.0);
        trip.addOrder(order, 0, null, 1.0);

        return trip;
    }

    private TripEntity twoOrderTrip() {
        DroneEntity drone = new DroneEntity(
                1L,
                "DRONE-1",
                10.0,
                30.0,
                DroneStatus.IN_ROUTE,
                100.0,
                1.0,
                20.0,
                10.0,
                10.0
        );
        OrderEntity firstOrder = new OrderEntity(1L, "ORDER-1", 10.0, 0.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        OrderEntity secondOrder = new OrderEntity(2L, "ORDER-2", 20.0, 0.0, 4.0, Priority.HIGH, OrderStatus.IN_ROUTE);
        TripEntity trip = new TripEntity(null, drone, TripStatus.IN_ROUTE, 8.0, 40.0);
        trip.addOrder(firstOrder, 0, null, 1.0);
        trip.addOrder(secondOrder, 1, null, 2.0);

        return trip;
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
                        tripOrder.getEstimatedDeliveryTime(),
                        tripOrder.getAvailabilityNotifiedAt(),
                        tripOrder.getAvailabilityConfirmedAt(),
                        tripOrder.getDeliveryConfirmationRequestedAt(),
                        tripOrder.getDeliveryFailedAt(),
                        tripOrder.getDeliveryFailureReason()
                );
            }

            tripsById.put(id, savedTrip);

            return savedTrip;
        }
    }
}
