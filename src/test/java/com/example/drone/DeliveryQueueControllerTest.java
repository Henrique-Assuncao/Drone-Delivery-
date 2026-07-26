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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryQueueControllerTest {

    private MockMvc mockMvc;
    private InMemoryOrderStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryOrderStorage();
        OrderQueryService queryService = new OrderQueryService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new DeliveryQueueController(queryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldListRequestedAndPendingReassignmentOrdersInDeliveryQueueOrder() throws Exception {
        storage.save(new OrderEntity(
                null,
                "ORDER-SECOND",
                5.0,
                6.0,
                2.0,
                Priority.MEDIUM,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:01:00Z")
        ));
        storage.save(new OrderEntity(
                null,
                "ORDER-ALLOCATED",
                7.0,
                8.0,
                1.0,
                Priority.LOW,
                OrderStatus.ALLOCATED,
                Instant.parse("2026-07-25T09:59:00Z")
        ));
        storage.save(new OrderEntity(
                null,
                "ORDER-REASSIGNMENT",
                9.0,
                10.0,
                3.0,
                Priority.MEDIUM,
                OrderStatus.PENDING_REASSIGNMENT,
                Instant.parse("2026-07-25T10:00:30Z")
        ));
        storage.save(new OrderEntity(
                null,
                "ORDER-FIRST",
                3.0,
                4.0,
                4.0,
                Priority.HIGH,
                OrderStatus.REQUESTED,
                Instant.parse("2026-07-25T10:00:00Z")
        ));

        mockMvc.perform(get("/api/delivery-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].orderId").value(4))
                .andExpect(jsonPath("$[0].orderIdentifier").value("ORDER-FIRST"))
                .andExpect(jsonPath("$[0].location.x").value(3.0))
                .andExpect(jsonPath("$[0].location.y").value(4.0))
                .andExpect(jsonPath("$[0].weight").value(4.0))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].queuedAt").value("2026-07-25T10:00:00Z"))
                .andExpect(jsonPath("$[1].orderId").value(3))
                .andExpect(jsonPath("$[1].orderIdentifier").value("ORDER-REASSIGNMENT"))
                .andExpect(jsonPath("$[1].status").value("PENDING_REASSIGNMENT"))
                .andExpect(jsonPath("$[1].queuedAt").value("2026-07-25T10:00:30Z"))
                .andExpect(jsonPath("$[2].orderId").value(1))
                .andExpect(jsonPath("$[2].orderIdentifier").value("ORDER-SECOND"))
                .andExpect(jsonPath("$[2].queuedAt").value("2026-07-25T10:01:00Z"));
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
        public List<OrderEntity> findDeliveryQueue() {
            return ordersById.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.REQUESTED
                            || order.getStatus() == OrderStatus.PENDING_REASSIGNMENT)
                    .sorted(Comparator.comparing(OrderEntity::getQueuedAt)
                            .thenComparing(OrderEntity::getId))
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
                    order.getQueuedAt()
            );

            ordersById.put(savedOrder.getId(), savedOrder);

            return savedOrder;
        }
    }
}
