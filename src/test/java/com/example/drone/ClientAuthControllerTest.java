package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientAuthControllerTest {

    private MockMvc mockMvc;
    private ClientAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        InMemoryClientUserStorage storage = new InMemoryClientUserStorage();
        authenticationService = new ClientAuthenticationService(
                storage,
                new PasswordHashingService(),
                "test-client-auth-secret"
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new ClientAuthController(authenticationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldRegisterClientUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cliente Demo",
                                  "email": "CLIENTE@EXEMPLO.COM",
                                  "password": "senha123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.name").value("Cliente Demo"))
                .andExpect(jsonPath("$.user.email").value("cliente@exemplo.com"))
                .andExpect(jsonPath("$.user.createdAt").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void shouldLoginRegisteredClientUserAndReturnCurrentUser() throws Exception {
        String token = registerAndExtractToken("cliente@exemplo.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "cliente@exemplo.com",
                                  "password": "senha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("cliente@exemplo.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("cliente@exemplo.com"));
    }

    @Test
    void shouldRejectDuplicatedEmail() throws Exception {
        registerAndExtractToken("cliente@exemplo.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Outro Cliente",
                                  "email": "cliente@exemplo.com",
                                  "password": "senha123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("user email already exists"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        registerAndExtractToken("cliente@exemplo.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "cliente@exemplo.com",
                                  "password": "senha-errada"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("email or password is invalid"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectMissingAuthorizationToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("authorization token is required"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    private String registerAndExtractToken(String email) {
        return authenticationService.register("Cliente Demo", email, "senha123").token();
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
}
