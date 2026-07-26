package com.example.drone.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final String apiKey;

    public InternalApiAuthenticationFilter(@Value("${drone.internal.api-key}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isInternalRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (apiKey.isBlank()) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "internal api key is not configured");
            return;
        }

        String providedApiKey = request.getHeader(HEADER_NAME);
        if (!apiKeyMatches(providedApiKey)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "internal api key is required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath.isBlank() ? requestUri : requestUri.substring(contextPath.length());

        return path.equals("/internal") || path.startsWith("/internal/");
    }

    private boolean apiKeyMatches(String providedApiKey) {
        if (providedApiKey == null) {
            return false;
        }

        return MessageDigest.isEqual(
                providedApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
