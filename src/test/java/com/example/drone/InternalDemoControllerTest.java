package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDemoControllerTest {

    private static final String INTERNAL_API_KEY = "test-internal-key";

    private MockMvc mockMvc;
    private FakeDemoDataService service;

    @BeforeEach
    void setUp() {
        service = new FakeDemoDataService();
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalDemoController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new InternalApiAuthenticationFilter(INTERNAL_API_KEY))
                .build();
    }

    @Test
    void shouldResetAndSeedDemoScenarioWhenConfirmationIsValid() throws Exception {
        mockMvc.perform(post("/internal/demo/reset-and-seed")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, INTERNAL_API_KEY)
                        .param("confirmation", InternalDemoController.RESET_CONFIRMATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drones").value(1))
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.obstacles").value(1))
                .andExpect(jsonPath("$.reviews").value(1))
                .andExpect(jsonPath("$.clients").value(1))
                .andExpect(jsonPath("$.trips").value(1))
                .andExpect(jsonPath("$.unallocatedOrders").value(0));
    }

    @Test
    void shouldRejectDemoResetWhenConfirmationIsMissing() throws Exception {
        mockMvc.perform(post("/internal/demo/reset-and-seed")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, INTERNAL_API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("confirmation must be RESET_DEMO_DATA"));
    }

    @Test
    void shouldRejectDemoResetWithoutApiKey() throws Exception {
        mockMvc.perform(post("/internal/demo/reset-and-seed")
                        .param("confirmation", InternalDemoController.RESET_CONFIRMATION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("internal api key is required"));
    }

    private static class FakeDemoDataService extends DemoDataService {

        FakeDemoDataService() {
            super(null, null, null);
        }

        @Override
        public DemoScenario resetAndSeed() {
            DroneEntity drone = new DroneEntity(1L, "DEMO-ALFA", 12.0, 120.0, DroneStatus.AVAILABLE);
            OrderEntity order = new OrderEntity(1L, "DEMO-ORDER", 3.0, 4.0, 4.0, Priority.HIGH, OrderStatus.ALLOCATED);
            ObstacleEntity obstacle = new ObstacleEntity(1L, 4.0, 2.0, 1.2, true);
            ReviewEntity review = new ReviewEntity(1L, 5, "Demo", "Feedback demo.");
            ClientUserEntity clientUser = new ClientUserEntity(
                    1L,
                    "Cliente Demo",
                    "cliente.demo@drone.local",
                    "hash",
                    java.time.Instant.now()
            );
            TripEntity trip = new TripEntity(1L, drone, TripStatus.PLANNED, 4.0, 10.0);

            return new DemoScenario(
                    List.of(drone),
                    List.of(order),
                    List.of(obstacle),
                    List.of(review),
                    List.of(clientUser),
                    new PersistedTripPlan(List.of(trip), List.of())
            );
        }
    }
}
