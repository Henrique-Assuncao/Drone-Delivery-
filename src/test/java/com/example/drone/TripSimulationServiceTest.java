package com.example.drone;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripSimulationServiceTest {

    @Test
    void shouldStartPlannedTripAndDeliverReachedOrder() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(oneOrderTrip(TripStatus.PLANNED));
        TripSimulationService service = new TripSimulationService(storage);

        TripSimulationState state = service.advance(trip.getId(), 1.0);

        TripEntity updatedTrip = storage.findById(trip.getId()).orElseThrow();
        TripOrderEntity routeOrder = updatedTrip.getTripOrders().get(0);

        assertEquals(TripStatus.IN_ROUTE, updatedTrip.getStatus());
        assertEquals(DroneStatus.IN_ROUTE, updatedTrip.getDrone().getStatus());
        assertEquals(OrderStatus.DELIVERED, routeOrder.getOrder().getStatus());
        assertTrue(routeOrder.isDelivered());
        assertEquals(10.0, state.travelledDistance());
        assertEquals(0.5, state.progress());
        assertEquals(90.0, updatedTrip.getDrone().getBatteryLevel());
        assertNotNull(state.updatedAt());
    }

    @Test
    void shouldCompleteTripWhenDroneFinishesRoute() {
        InMemoryTripStorage storage = new InMemoryTripStorage();
        TripEntity trip = storage.save(oneOrderTrip(TripStatus.PLANNED));
        TripSimulationService service = new TripSimulationService(storage);

        service.advance(trip.getId(), 1.0);
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
}
