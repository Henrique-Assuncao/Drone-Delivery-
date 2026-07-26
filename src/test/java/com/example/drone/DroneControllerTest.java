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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DroneControllerTest {

    private MockMvc mockMvc;
    private InMemoryDroneStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryDroneStorage();
        mockMvc = buildMockMvc(new EmptyTripStorage());
    }

    private MockMvc buildMockMvc(TripStorage tripStorage) {
        DroneRegistrationService registrationService = new DroneRegistrationService(storage);
        DroneQueryService queryService = new DroneQueryService(storage);
        DroneAvailabilityService availabilityService = new DroneAvailabilityService(storage);
        DroneRechargeService rechargeService = new DroneRechargeService(storage);
        DroneRemovalService removalService = new DroneRemovalService(storage, tripStorage);

        return MockMvcBuilders.standaloneSetup(
                        new DroneController(registrationService, queryService, availabilityService, rechargeService, removalService)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateDrone() throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "DRONE-1",
                                  "maxWeightCapacity": 10.0,
                                  "maxRange": 20.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.maxWeightCapacity").value(10.0))
                .andExpect(jsonPath("$.maxRange").value(20.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batteryLevel").value(100.0))
                .andExpect(jsonPath("$.batteryConsumptionPerDistanceUnit").value(1.0))
                .andExpect(jsonPath("$.minimumReturnBattery").value(20.0))
                .andExpect(jsonPath("$.speed").value(60.0))
                .andExpect(jsonPath("$.chargingRate").value(10.0));
    }

    @Test
    void shouldCreateDroneWithBatteryFields() throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "DRONE-1",
                                  "maxWeightCapacity": 10.0,
                                  "maxRange": 20.0,
                                  "batteryLevel": 75.0,
                                  "batteryConsumptionPerDistanceUnit": 1.5,
                                  "minimumReturnBattery": 25.0,
                                  "speed": 2.0,
                                  "chargingRate": 12.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.maxWeightCapacity").value(10.0))
                .andExpect(jsonPath("$.maxRange").value(20.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batteryLevel").value(75.0))
                .andExpect(jsonPath("$.batteryConsumptionPerDistanceUnit").value(1.5))
                .andExpect(jsonPath("$.minimumReturnBattery").value(25.0))
                .andExpect(jsonPath("$.speed").value(2.0))
                .andExpect(jsonPath("$.chargingRate").value(12.0));
    }

    @Test
    void shouldRejectInvalidDroneInput() throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "DRONE-1",
                                  "maxWeightCapacity": 0.0,
                                  "maxRange": 20.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("maxWeightCapacity must be greater than zero"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectInvalidDroneBatteryInput() throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "DRONE-1",
                                  "maxWeightCapacity": 10.0,
                                  "maxRange": 20.0,
                                  "batteryLevel": 100.1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("batteryLevel must be between 0 and 100"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnConflictForDuplicatedDroneIdentifier() throws Exception {
        String request = """
                {
                  "identifier": "DRONE-1",
                  "maxWeightCapacity": 10.0,
                  "maxRange": 20.0
                }
                """;

        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("drone identifier already exists"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListAllDrones() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);
        createDrone("DRONE-2", 15.0, 30.0);

        mockMvc.perform(get("/api/drones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].identifier").value("DRONE-1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].identifier").value("DRONE-2"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    void shouldFindDroneById() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(get("/api/drones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.maxWeightCapacity").value(10.0))
                .andExpect(jsonPath("$.maxRange").value(20.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batteryLevel").value(100.0))
                .andExpect(jsonPath("$.batteryConsumptionPerDistanceUnit").value(1.0))
                .andExpect(jsonPath("$.minimumReturnBattery").value(20.0))
                .andExpect(jsonPath("$.speed").value(60.0))
                .andExpect(jsonPath("$.chargingRate").value(10.0));
    }

    @Test
    void shouldReturnNotFoundWhenDroneDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/drones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("drone not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListOnlyAvailableDrones() throws Exception {
        createDrone("DRONE-AVAILABLE", 10.0, 20.0);
        storage.save(new DroneEntity(null, "DRONE-IN-ROUTE", 10.0, 20.0, DroneStatus.IN_ROUTE));
        storage.save(new DroneEntity(null, "DRONE-UNAVAILABLE", 10.0, 20.0, DroneStatus.UNAVAILABLE));

        mockMvc.perform(get("/api/drones/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].identifier").value("DRONE-AVAILABLE"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void shouldListDronesFilteredByStatus() throws Exception {
        createDrone("DRONE-AVAILABLE", 10.0, 20.0);
        storage.save(new DroneEntity(null, "DRONE-IN-ROUTE", 10.0, 20.0, DroneStatus.IN_ROUTE));
        storage.save(new DroneEntity(null, "DRONE-UNAVAILABLE", 10.0, 20.0, DroneStatus.UNAVAILABLE));

        mockMvc.perform(get("/api/drones").param("status", "UNAVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].identifier").value("DRONE-UNAVAILABLE"))
                .andExpect(jsonPath("$[0].status").value("UNAVAILABLE"));
    }

    @Test
    void shouldRejectInvalidDroneStatusFilter() throws Exception {
        mockMvc.perform(get("/api/drones").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be one of AVAILABLE, IN_ROUTE, UNAVAILABLE, CHARGING"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldMarkAvailableDroneAsUnavailable() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(post("/api/drones/1/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));

        org.junit.jupiter.api.Assertions.assertEquals(
                DroneStatus.UNAVAILABLE,
                storage.findById(1L).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldMarkUnavailableDroneAsAvailable() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.UNAVAILABLE));

        mockMvc.perform(post("/api/drones/1/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        org.junit.jupiter.api.Assertions.assertEquals(
                DroneStatus.AVAILABLE,
                storage.findById(1L).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldEnqueueAvailableDroneForRecharge() throws Exception {
        storage.save(new DroneEntity(
                null,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                75.0,
                1.0,
                20.0,
                1.0,
                10.0
        ));

        mockMvc.perform(post("/api/drones/1/recharge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.status").value("CHARGING"))
                .andExpect(jsonPath("$.batteryLevel").value(75.0))
                .andExpect(jsonPath("$.rechargeQueuedAt").exists())
                .andExpect(jsonPath("$.rechargeReason").value("manual recharge requested"));

        org.junit.jupiter.api.Assertions.assertEquals(
                DroneStatus.CHARGING,
                storage.findById(1L).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldCompleteDroneRecharge() throws Exception {
        DroneEntity drone = storage.save(new DroneEntity(
                null,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                75.0,
                1.0,
                20.0,
                1.0,
                10.0
        ));
        drone.enqueueForRecharge("manual recharge requested");

        mockMvc.perform(post("/api/drones/1/recharge/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batteryLevel").value(100.0));

        DroneEntity rechargedDrone = storage.findById(1L).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(DroneStatus.AVAILABLE, rechargedDrone.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(100.0, rechargedDrone.getBatteryLevel());
        org.junit.jupiter.api.Assertions.assertNull(rechargedDrone.getRechargeQueuedAt());
        org.junit.jupiter.api.Assertions.assertNull(rechargedDrone.getRechargeReason());
    }

    @Test
    void shouldRejectEnqueuingDroneWithFullBatteryForRecharge() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(post("/api/drones/1/recharge"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone battery must be below 100 to enter recharge queue"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectCompletingRechargeWhenDroneIsNotCharging() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(post("/api/drones/1/recharge/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be CHARGING to complete recharge"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnNotFoundWhenMarkingUnknownDroneAsUnavailable() throws Exception {
        mockMvc.perform(post("/api/drones/999/unavailable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("drone not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnNotFoundWhenMarkingUnknownDroneAsAvailable() throws Exception {
        mockMvc.perform(post("/api/drones/999/available"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("drone not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMarkingUnavailableDroneAsUnavailable() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.UNAVAILABLE));

        mockMvc.perform(post("/api/drones/1/unavailable"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be AVAILABLE to mark unavailable"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMarkingAvailableDroneAsAvailable() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(post("/api/drones/1/available"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be UNAVAILABLE to mark available"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMarkingDroneInRouteAsUnavailable() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE));

        mockMvc.perform(post("/api/drones/1/unavailable"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be AVAILABLE to mark unavailable"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMarkingDroneInRouteAsAvailable() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE));

        mockMvc.perform(post("/api/drones/1/available"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must be UNAVAILABLE to mark available"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldDeleteDrone() throws Exception {
        createDrone("DRONE-1", 10.0, 20.0);

        mockMvc.perform(delete("/api/drones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(content().string(not(containsString("trace"))));

        mockMvc.perform(get("/api/drones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectDeletingDroneInRoute() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.IN_ROUTE));

        mockMvc.perform(delete("/api/drones/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone must not be IN_ROUTE to delete"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectDeletingDroneWithTrips() throws Exception {
        storage.save(new DroneEntity(null, "DRONE-1", 10.0, 20.0, DroneStatus.AVAILABLE));
        MockMvc mockMvcWithTripHistory = buildMockMvc(new TripLinkedStorage());

        mockMvcWithTripHistory.perform(delete("/api/drones/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("drone with trips cannot be deleted"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    private void createDrone(String identifier, double maxWeightCapacity, double maxRange) throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "maxWeightCapacity": %s,
                                  "maxRange": %s
                                }
                                """.formatted(
                                identifier,
                                Double.toString(maxWeightCapacity),
                                Double.toString(maxRange)
                        )))
                .andExpect(status().isCreated());
    }

    private static class InMemoryDroneStorage implements DroneStorage {

        private final Map<String, DroneEntity> dronesByIdentifier = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public boolean existsByIdentifier(String identifier) {
            return dronesByIdentifier.containsKey(identifier);
        }

        @Override
        public List<DroneEntity> findAll() {
            return new ArrayList<>(dronesByIdentifier.values());
        }

        @Override
        public Optional<DroneEntity> findById(Long id) {
            return dronesByIdentifier.values().stream()
                    .filter(drone -> drone.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<DroneEntity> findByStatus(DroneStatus status) {
            return dronesByIdentifier.values().stream()
                    .filter(drone -> drone.getStatus() == status)
                    .toList();
        }

        @Override
        public List<DroneEntity> findRechargeQueue() {
            return dronesByIdentifier.values().stream()
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

            dronesByIdentifier.put(savedDrone.getIdentifier(), savedDrone);

            return savedDrone;
        }

        @Override
        public void delete(DroneEntity drone) {
            dronesByIdentifier.remove(drone.getIdentifier());
        }
    }

    private static class EmptyTripStorage implements TripStorage {

        @Override
        public List<TripEntity> findAll() {
            return List.of();
        }

        @Override
        public List<TripEntity> findByStatus(TripStatus status) {
            return List.of();
        }

        @Override
        public Optional<TripEntity> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public TripEntity save(TripEntity trip) {
            return trip;
        }
    }

    private static class TripLinkedStorage extends EmptyTripStorage {

        @Override
        public boolean existsByDroneId(Long droneId) {
            return true;
        }
    }
}
