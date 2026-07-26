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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private MockMvc mockMvc;
    private InMemoryOrderStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryOrderStorage();
        OrderRegistrationService registrationService = new OrderRegistrationService(storage);
        OrderQueryService queryService = new OrderQueryService(storage);
        OrderOperationService operationService = new OrderOperationService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(registrationService, queryService, operationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "ORDER-1",
                                  "location": {
                                    "x": 3.0,
                                    "y": 4.0
                                  },
                                  "weight": 4.0,
                                  "priority": "HIGH",
                                  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("ORDER-1"))
                .andExpect(jsonPath("$.location.x").value(3.0))
                .andExpect(jsonPath("$.location.y").value(4.0))
                .andExpect(jsonPath("$.weight").value(4.0))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.queuedAt").exists())
                .andExpect(jsonPath("$.confirmedDeliveryTime").value("2026-07-26T18:30:00Z"))
                .andExpect(jsonPath("$.deliveryConfirmationCode").value("ORDER-1"));
    }

    @Test
    void shouldRejectInvalidOrderWeight() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "ORDER-1",
                                  "location": {
                                    "x": 3.0,
                                    "y": 4.0
                                  },
                                  "weight": 0.0,
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("weight must be greater than zero"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMissingOrderLocation() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "ORDER-1",
                                  "weight": 4.0,
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("location must not be null"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldReturnConflictForDuplicatedOrderIdentifier() throws Exception {
        String request = """
                {
                  "identifier": "ORDER-1",
                  "location": {
                    "x": 3.0,
                    "y": 4.0
                  },
                  "weight": 4.0,
                  "priority": "HIGH",
                  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("order identifier already exists"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListAllOrders() throws Exception {
        createOrder("ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH);
        createOrder("ORDER-2", 5.0, 6.0, 2.0, Priority.LOW);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].identifier").value("ORDER-1"))
                .andExpect(jsonPath("$[0].location.x").value(3.0))
                .andExpect(jsonPath("$[0].location.y").value(4.0))
                .andExpect(jsonPath("$[0].weight").value(4.0))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].confirmedDeliveryTime").exists())
                .andExpect(jsonPath("$[0].deliveryConfirmationCode").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].identifier").value("ORDER-2"))
                .andExpect(jsonPath("$[1].status").value("REQUESTED"));
    }

    @Test
    void shouldFindOrderById() throws Exception {
        createOrder("ORDER-1", 3.0, 4.0, 4.0, Priority.HIGH);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("ORDER-1"))
                .andExpect(jsonPath("$.location.x").value(3.0))
                .andExpect(jsonPath("$.location.y").value(4.0))
                .andExpect(jsonPath("$.weight").value(4.0))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.confirmedDeliveryTime").exists())
                .andExpect(jsonPath("$.deliveryConfirmationCode").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("order not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListOrdersFilteredByStatus() throws Exception {
        createOrder("ORDER-REQUESTED", 3.0, 4.0, 4.0, Priority.HIGH);
        storage.save(new OrderEntity(null, "ORDER-ALLOCATED", 5.0, 6.0, 2.0, Priority.MEDIUM, OrderStatus.ALLOCATED));
        storage.save(new OrderEntity(null, "ORDER-DELIVERED", 7.0, 8.0, 1.0, Priority.LOW, OrderStatus.DELIVERED));

        mockMvc.perform(get("/api/orders").param("status", "REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].identifier").value("ORDER-REQUESTED"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/orders").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("status must be one of REQUESTED, ALLOCATED, IN_ROUTE, PENDING_REASSIGNMENT, DELIVERED, NOT_DELIVERED, CANCELLED, UNALLOCATED"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldCancelUnallocatedOrderWithReason() throws Exception {
        OrderEntity order = storage.save(new OrderEntity(null, "ORDER-UNALLOCATED", 5.0, 6.0, 2.0, Priority.MEDIUM, OrderStatus.REQUESTED));
        order.changeStatus(OrderStatus.UNALLOCATED, "order exceeds max drone range");

        mockMvc.perform(post("/api/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Cliente solicitou cancelamento por inviabilidade operacional."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusReason").value("Cliente solicitou cancelamento por inviabilidade operacional."))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRequeueUnallocatedOrder() throws Exception {
        OrderEntity order = storage.save(new OrderEntity(null, "ORDER-UNALLOCATED", 5.0, 6.0, 2.0, Priority.MEDIUM, OrderStatus.REQUESTED));
        order.changeStatus(OrderStatus.UNALLOCATED, "order exceeds max drone range");

        mockMvc.perform(post("/api/orders/1/requeue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.statusReason").doesNotExist())
                .andExpect(content().string(not(containsString("trace"))));
    }

    private void createOrder(
            String identifier,
            double locationX,
            double locationY,
            double weight,
            Priority priority
    ) throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "location": {
                                    "x": %s,
                                    "y": %s
                                  },
                                  "weight": %s,
                                  "priority": "%s",
                                  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
                                }
                                """.formatted(
                                identifier,
                                Double.toString(locationX),
                                Double.toString(locationY),
                                Double.toString(weight),
                                priority
                        )))
                .andExpect(status().isCreated());
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
                    .toList();
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
                    order.getConfirmedDeliveryTime(),
                    order.getClientUser()
            );
            savedOrder.changeStatus(savedOrder.getStatus(), order.getStatusReason());

            ordersById.put(savedOrder.getId(), savedOrder);

            return savedOrder;
        }
    }
}
