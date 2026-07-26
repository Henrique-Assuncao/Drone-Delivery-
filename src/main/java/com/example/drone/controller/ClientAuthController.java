package com.example.drone.controller;

import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class ClientAuthController {

    private final ClientAuthenticationService authenticationService;

    public ClientAuthController(ClientAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody(required = false) RegisterRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        ClientAuthentication authentication = authenticationService.register(
                request.name(),
                request.email(),
                request.password()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(authentication));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            throw new InvalidInputException("request body must not be null");
        }

        return toAuthResponse(authenticationService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    public ClientUserResponse me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return toUserResponse(authenticationService.authenticate(authorizationHeader));
    }

    private AuthResponse toAuthResponse(ClientAuthentication authentication) {
        return new AuthResponse(toUserResponse(authentication.user()), authentication.token());
    }

    private ClientUserResponse toUserResponse(ClientUserEntity user) {
        return new ClientUserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }

    public record RegisterRequest(String name, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record AuthResponse(ClientUserResponse user, String token) {
    }

    public record ClientUserResponse(
            Long id,
            String name,
            String email,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Instant createdAt
    ) {
    }
}
