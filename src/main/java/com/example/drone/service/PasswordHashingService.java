package com.example.drone.service;

import com.example.drone.exception.*;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordHashingService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String PREFIX = "pbkdf2";

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidInputException("password must not be blank");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        return String.join(
                "$",
                PREFIX,
                Integer.toString(ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    public boolean matches(String password, String encodedPassword) {
        if (password == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }

        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * Byte.SIZE);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("password hashing failed", exception);
        }
    }
}
