package com.example.drone.service;

import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class ClientAuthenticationService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L;

    private final ClientUserStorage userStorage;
    private final PasswordHashingService passwordHashingService;
    private final String tokenSecret;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientAuthenticationService(
            ClientUserStorage userStorage,
            PasswordHashingService passwordHashingService,
            @Value("${drone.client-auth.secret}") String tokenSecret
    ) {
        this.userStorage = userStorage;
        this.passwordHashingService = passwordHashingService;
        this.tokenSecret = tokenSecret == null ? "" : tokenSecret.trim();
    }

    @Transactional
    public ClientAuthentication register(String name, String email, String password) {
        String normalizedName = requireName(name);
        String normalizedEmail = normalizeAndValidateEmail(email);
        validatePassword(password);

        if (userStorage.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("user email already exists");
        }

        ClientUserEntity user = userStorage.save(new ClientUserEntity(
                null,
                normalizedName,
                normalizedEmail,
                passwordHashingService.hash(password),
                Instant.now()
        ));

        return new ClientAuthentication(user, issueToken(user));
    }

    @Transactional(readOnly = true)
    public ClientAuthentication login(String email, String password) {
        String normalizedEmail = normalizeAndValidateEmail(email);
        ClientUserEntity user = userStorage.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("email or password is invalid"));

        if (!passwordHashingService.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("email or password is invalid");
        }

        return new ClientAuthentication(user, issueToken(user));
    }

    @Transactional(readOnly = true)
    public ClientUserEntity authenticate(String authorizationHeader) {
        String token = bearerTokenOrThrow(authorizationHeader);
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 2) {
            throw new UnauthorizedException("authorization token is invalid");
        }

        String payload = decodeTokenPart(tokenParts[0]);
        byte[] providedSignature = decodeTokenPartBytes(tokenParts[1]);
        byte[] expectedSignature = sign(payload);
        if (!MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new UnauthorizedException("authorization token is invalid");
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 3) {
            throw new UnauthorizedException("authorization token is invalid");
        }

        long userId;
        long expiresAtEpochSecond;
        try {
            userId = Long.parseLong(payloadParts[0]);
            expiresAtEpochSecond = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("authorization token is invalid");
        }

        if (Instant.now().getEpochSecond() >= expiresAtEpochSecond) {
            throw new UnauthorizedException("authorization token expired");
        }

        return userStorage.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("authorization token is invalid"));
    }

    private String issueToken(ClientUserEntity user) {
        String payload = user.getId()
                + ":"
                + Instant.now().plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond()
                + ":"
                + randomTokenNonce();

        return encodeTokenPart(payload.getBytes(StandardCharsets.UTF_8)) + "." + encodeTokenPart(sign(payload));
    }

    private String bearerTokenOrThrow(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("authorization token is required");
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("authorization token is invalid");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("authorization token is required");
        }

        return token;
    }

    private byte[] sign(String payload) {
        if (tokenSecret.isBlank()) {
            throw new IllegalStateException("client auth secret is not configured");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("token signing failed", exception);
        }
    }

    private String randomTokenNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return encodeTokenPart(bytes);
    }

    private String decodeTokenPart(String encodedValue) {
        return new String(decodeTokenPartBytes(encodedValue), StandardCharsets.UTF_8);
    }

    private byte[] decodeTokenPartBytes(String encodedValue) {
        try {
            return Base64.getUrlDecoder().decode(encodedValue);
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("authorization token is invalid");
        }
    }

    private String encodeTokenPart(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("name must not be blank");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() > 120) {
            throw new InvalidInputException("name must be at most 120 characters");
        }

        return normalizedName;
    }

    private String normalizeAndValidateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("email must not be blank");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 180 || !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidInputException("email is invalid");
        }

        return normalizedEmail;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidInputException("password must not be blank");
        }

        if (password.length() < 8) {
            throw new InvalidInputException("password must have at least 8 characters");
        }
    }
}
