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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDroneControllerTest {

    private static final String INTERNAL_API_KEY = "test-internal-key";

    private MockMvc mockMvc;
    private InMemoryDroneStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryDroneStorage();
        DroneQueryService queryService = new DroneQueryService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new InternalDroneController(queryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new InternalApiAuthenticationFilter(INTERNAL_API_KEY))
                .build();
    }

    @Test
    void shouldFindDroneBatteryById() throws Exception {
        storage.save(new DroneEntity(
                null,
                "DRONE-1",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                75.0,
                1.5,
                25.0,
                2.0,
                12.0
        ));

        mockMvc.perform(get("/internal/drones/1/battery")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("DRONE-1"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batteryLevel").value(75.0))
                .andExpect(jsonPath("$.batteryConsumptionPerDistanceUnit").value(1.5))
                .andExpect(jsonPath("$.minimumReturnBattery").value(25.0))
                .andExpect(jsonPath("$.chargingRate").value(12.0));
    }

    @Test
    void shouldReturnNotFoundWhenDroneDoesNotExist() throws Exception {
        mockMvc.perform(get("/internal/drones/999/battery")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, INTERNAL_API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("drone not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectInternalRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/internal/drones/1/battery"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("internal api key is required"));
    }

    @Test
    void shouldRejectInternalRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/internal/drones/1/battery")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("internal api key is required"));
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
}
