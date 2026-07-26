package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
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

class ClientOrderControllerTest {

    private MockMvc mockMvc;
    private InMemoryOrderStorage orderStorage;
    private ClientAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        InMemoryClientUserStorage userStorage = new InMemoryClientUserStorage();
        orderStorage = new InMemoryOrderStorage();
        authenticationService = new ClientAuthenticationService(
                userStorage,
                new PasswordHashingService(),
                "test-client-auth-secret"
        );
        OrderRegistrationService registrationService = new OrderRegistrationService(orderStorage);

        mockMvc = MockMvcBuilders.standaloneSetup(new ClientOrderController(
                        authenticationService,
                        registrationService,
                        orderStorage
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateClientOrderForAuthenticatedUser() throws Exception {
        String token = authenticationService.register("Cliente Demo", "cliente@exemplo.com", "senha123").token();

        mockMvc.perform(post("/api/client/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "CLIENTE-1",
                                  "location": {
                                    "x": 3.0,
                                    "y": 4.0
                                  },
                                  "weight": 4.0,
                                  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identifier").value("CLIENTE-1"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.deliveryConfirmationCode").value("CLIENTE-1"));

        OrderEntity savedOrder = orderStorage.findById(1L).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(savedOrder.getClientUser());
        org.junit.jupiter.api.Assertions.assertEquals("cliente@exemplo.com", savedOrder.getClientUser().getEmail());
    }

    @Test
    void shouldListOnlyAuthenticatedUserOrders() throws Exception {
        ClientAuthentication firstClient = authenticationService.register("Cliente Um", "um@exemplo.com", "senha123");
        ClientAuthentication secondClient = authenticationService.register("Cliente Dois", "dois@exemplo.com", "senha123");
        orderStorage.save(new OrderEntity(null, "UM-1", 1.0, 2.0, 2.0, Priority.MEDIUM, OrderStatus.REQUESTED, Instant.now(), "UM-1", Instant.parse("2026-07-26T18:30:00Z"), firstClient.user()));
        orderStorage.save(new OrderEntity(null, "DOIS-1", 3.0, 4.0, 2.0, Priority.MEDIUM, OrderStatus.REQUESTED, Instant.now(), "DOIS-1", Instant.parse("2026-07-26T18:30:00Z"), secondClient.user()));
        orderStorage.save(new OrderEntity(null, "ADMIN-1", 5.0, 6.0, 2.0, Priority.HIGH, OrderStatus.REQUESTED));

        mockMvc.perform(get("/api/client/orders")
                        .header("Authorization", "Bearer " + firstClient.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].identifier").value("UM-1"))
                .andExpect(content().string(not(containsString("DOIS-1"))))
                .andExpect(content().string(not(containsString("ADMIN-1"))));
    }

    @Test
    void shouldRejectClientOrdersWithoutAuthorizationToken() throws Exception {
        mockMvc.perform(get("/api/client/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("authorization token is required"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    private static class InMemoryClientUserStorage implements ClientUserStorage {

        private final Map<Long, ClientUserEntity> usersById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public boolean existsByEmail(String email) {
            return usersById.values().stream()
                    .anyMatch(user -> user.getEmail().equals(email));
        }

        @Override
        public Optional<ClientUserEntity> findById(Long id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public Optional<ClientUserEntity> findByEmail(String email) {
            return usersById.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public ClientUserEntity save(ClientUserEntity user) {
            Long id = user.getId() == null ? nextId++ : user.getId();
            ClientUserEntity savedUser = new ClientUserEntity(
                    id,
                    user.getName(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getCreatedAt() == null ? Instant.now() : user.getCreatedAt()
            );

            usersById.put(id, savedUser);
            return savedUser;
        }
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
            Long id = order.getId() == null ? nextId++ : order.getId();
            OrderEntity savedOrder = new OrderEntity(
                    id,
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

            ordersById.put(id, savedOrder);

            return savedOrder;
        }
    }
}
