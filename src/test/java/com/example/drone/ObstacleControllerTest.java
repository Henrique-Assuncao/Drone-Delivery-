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

class ObstacleControllerTest {

    private MockMvc mockMvc;
    private InMemoryObstacleStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObstacleStorage();
        ObstacleRegistrationService registrationService = new ObstacleRegistrationService(storage);
        ObstacleQueryService queryService = new ObstacleQueryService(storage);
        ObstacleCommandService commandService = new ObstacleCommandService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new ObstacleController(
                        registrationService,
                        queryService,
                        commandService
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateObstacle() throws Exception {
        mockMvc.perform(post("/api/obstacles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "center": {
                                    "x": 5.0,
                                    "y": 0.0
                                  },
                                  "radius": 1.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.center.x").value(5.0))
                .andExpect(jsonPath("$.center.y").value(0.0))
                .andExpect(jsonPath("$.radius").value(1.0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectObstacleWithoutCenter() throws Exception {
        mockMvc.perform(post("/api/obstacles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "radius": 1.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("center must not be null"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectObstacleWithInvalidRadius() throws Exception {
        mockMvc.perform(post("/api/obstacles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "center": {
                                    "x": 5.0,
                                    "y": 0.0
                                  },
                                  "radius": 0.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("radius must be greater than zero"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListObstacles() throws Exception {
        storage.save(new ObstacleEntity(null, 5.0, 0.0, 1.0, true));
        storage.save(new ObstacleEntity(null, 10.0, 2.0, 3.0, false));

        mockMvc.perform(get("/api/obstacles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void shouldDeactivateObstacle() throws Exception {
        storage.save(new ObstacleEntity(null, 5.0, 0.0, 1.0, true));

        mockMvc.perform(delete("/api/obstacles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldReturnNotFoundWhenDeactivatingUnknownObstacle() throws Exception {
        mockMvc.perform(delete("/api/obstacles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("obstacle not found"))
                .andExpect(content().string(not(containsString("trace"))));
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
