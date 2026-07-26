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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RechargeQueueControllerTest {

    private MockMvc mockMvc;
    private InMemoryDroneStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryDroneStorage();
        DroneRechargeService rechargeService = new DroneRechargeService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new RechargeQueueController(rechargeService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldListRechargeQueue() throws Exception {
        DroneEntity chargingDrone = storage.save(new DroneEntity(
                null,
                "DRONE-CHARGING",
                10.0,
                20.0,
                DroneStatus.AVAILABLE,
                75.0,
                1.0,
                20.0,
                1.0,
                10.0
        ));
        chargingDrone.enqueueForRecharge("manual recharge requested");
        storage.save(new DroneEntity(null, "DRONE-AVAILABLE", 10.0, 20.0, DroneStatus.AVAILABLE));

        mockMvc.perform(get("/api/recharge-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].droneId").value(1))
                .andExpect(jsonPath("$[0].droneIdentifier").value("DRONE-CHARGING"))
                .andExpect(jsonPath("$[0].status").value("CHARGING"))
                .andExpect(jsonPath("$[0].batteryLevel").value(75.0))
                .andExpect(jsonPath("$[0].queuedAt").exists())
                .andExpect(jsonPath("$[0].reason").value("manual recharge requested"));
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
                    .sorted(Comparator.comparing(DroneEntity::getRechargeQueuedAt)
                            .thenComparing(DroneEntity::getId))
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
