package com.example.drone.persistence;

import com.example.drone.exception.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "client_users")
public class ClientUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClientUserEntity() {
    }

    public ClientUserEntity(Long id, String name, String email, String passwordHash, Instant createdAt) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("name must not be blank");
        }

        if (email == null || email.isBlank()) {
            throw new InvalidInputException("email must not be blank");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidInputException("passwordHash must not be blank");
        }

        if (createdAt == null) {
            throw new InvalidInputException("createdAt must not be null");
        }

        this.id = id;
        this.name = name.trim();
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
