package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationalFlowIntegrationTest {

    private static final String SCHEMA = "it_" + UUID.randomUUID().toString().replace("-", "");
    private static final String INTERNAL_API_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configurePostgresSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/drone_delivery?currentSchema=" + SCHEMA);
        registry.add("spring.datasource.username", () -> "drone");
        registry.add("spring.datasource.password", () -> "drone");
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("drone.internal.api-key", () -> INTERNAL_API_KEY);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE monthly_drone_productivity_reports, monthly_productivity_reports, trip_telemetry, trip_orders, trips, obstacles, reviews, orders, client_users, drones RESTART IDENTITY CASCADE");
    }

    @AfterAll
    void dropSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void shouldCompletePersistedOperationalFlow() throws Exception {
        createDrone("DRONE-INTEGRATION-1");
        String deliveryConfirmationCode = createOrder("ORDER-INTEGRATION-1");

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].id").value(1))
                .andExpect(jsonPath("$.trips[0].droneId").value(1))
                .andExpect(jsonPath("$.trips[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.trips[0].orders[0]").value(1))
                .andExpect(jsonPath("$.trips[0].route[0]").value(1))
                .andExpect(jsonPath("$.trips[0].totalWeight").value(4.0))
                .andExpect(jsonPath("$.trips[0].totalDistance").value(10.0))
                .andExpect(jsonPath("$.trips[0].estimatedDuration").value(10.0))
                .andExpect(jsonPath("$.unallocatedOrders.length()").value(0));

        mockMvc.perform(get("/api/orders").param("status", "ALLOCATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ALLOCATED"));

        mockMvc.perform(post("/api/trips/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_ROUTE"));

        mockMvc.perform(get("/api/drones").param("status", "IN_ROUTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_ROUTE"));

        mockMvc.perform(get("/api/orders").param("status", "IN_ROUTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_ROUTE"));

        mockMvc.perform(post("/api/trips/1/simulation/tick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elapsedMinutes": 5.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ROUTE"))
                .andExpect(jsonPath("$.nextOrderId").value(1))
                .andExpect(jsonPath("$.moving").value(false));

        mockMvc.perform(post("/api/trips/1/route/0/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeProgress[0].availabilityConfirmedAt").exists());

        mockMvc.perform(post("/api/trips/1/route/0/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationCode": "%s"
                                }
                                """.formatted(deliveryConfirmationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeProgress[0].delivered").value(true))
                .andExpect(jsonPath("$.status").value("IN_ROUTE"));

        mockMvc.perform(post("/api/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/trips").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(get("/api/drones").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/orders").param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));

        mockMvc.perform(get("/api/reports/productivity/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderEntries").value(1))
                .andExpect(jsonPath("$.ordersSent").value(1))
                .andExpect(jsonPath("$.ordersDelivered").value(1))
                .andExpect(jsonPath("$.ordersCancelled").value(0))
                .andExpect(jsonPath("$.drones[0].droneIdentifier").value("DRONE-INTEGRATION-1"))
                .andExpect(jsonPath("$.drones[0].ordersDelivered").value(1))
                .andExpect(jsonPath("$.drones[0].tripsCompleted").value(1));
    }

    @Test
    void shouldCancelPersistedPlannedTripAndReturnOrderToRequested() throws Exception {
        createDrone("DRONE-INTEGRATION-2");
        createOrder("ORDER-INTEGRATION-2");

        mockMvc.perform(post("/api/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.trips[0].id").value(1))
                .andExpect(jsonPath("$.trips[0].status").value("PLANNED"));

        mockMvc.perform(post("/api/trips/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/trips").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));

        mockMvc.perform(get("/api/drones").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/orders").param("status", "REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));

        mockMvc.perform(get("/api/reports/productivity/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderEntries").value(1))
                .andExpect(jsonPath("$.ordersSent").value(0))
                .andExpect(jsonPath("$.ordersDelivered").value(0))
                .andExpect(jsonPath("$.ordersCancelled").value(1))
                .andExpect(jsonPath("$.drones[0].droneIdentifier").value("DRONE-INTEGRATION-2"))
                .andExpect(jsonPath("$.drones[0].tripsCancelled").value(1));
    }

    @Test
    void shouldResetAndSeedDemoScenario() throws Exception {
        createDrone("DRONE-OLD");
        createOrder("ORDER-OLD");

        mockMvc.perform(post("/internal/demo/reset-and-seed")
                        .header(InternalApiAuthenticationFilter.HEADER_NAME, INTERNAL_API_KEY)
                        .param("confirmation", InternalDemoController.RESET_CONFIRMATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drones").value(4))
                .andExpect(jsonPath("$.orders").value(6))
                .andExpect(jsonPath("$.obstacles").value(1))
                .andExpect(jsonPath("$.reviews").value(2))
                .andExpect(jsonPath("$.clients").value(1))
                .andExpect(jsonPath("$.trips").value(greaterThan(0)))
                .andExpect(jsonPath("$.unallocatedOrders").value(2));

        mockMvc.perform(get("/api/drones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].identifier").value("DEMO-ALFA"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].identifier").value("DEMO-CLIENTE"))
                .andExpect(jsonPath("$[0].status").value("ALLOCATED"));
    }

    private void createDrone(String identifier) throws Exception {
        mockMvc.perform(post("/api/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "maxWeightCapacity": 10.0,
                                  "maxRange": 20.0
                                }
                                """.formatted(identifier)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value(identifier))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    private String createOrder(String identifier) throws Exception {
        String responseBody = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "location": {
                                    "x": 3.0,
                                    "y": 4.0
                                  },
                                  "weight": 4.0,
                                  "priority": "HIGH",
                                  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
                                }
                                """.formatted(identifier)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value(identifier))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.deliveryConfirmationCode").value(identifier))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("deliveryConfirmationCode").asText();
    }
}
