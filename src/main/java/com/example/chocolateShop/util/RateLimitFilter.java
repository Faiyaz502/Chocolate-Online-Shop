package com.example.chocolateShop.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter applied only to /api/v1/auth/**
 * (register, login, refresh, logout) to blunt credential-stuffing /
 * brute-force attempts. Keyed by client IP.
 *
 * This is a single-instance, in-memory limiter.
 *
 * Caffeine provides:
 * - Automatic expiration of inactive IP entries
 * - Maximum cache size to prevent unbounded memory growth
 *
 * For multiple application instances behind a load balancer,
 * use a shared store such as Redis instead.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private static final long CACHE_EXPIRY_MINUTES = 5;
    private static final long MAX_CACHE_SIZE = 100_000;

    private final Cache<String, Window> windows;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.windows = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(Duration.ofMinutes(CACHE_EXPIRY_MINUTES))
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = ClientIpUtil.resolve(request);

        Window window = windows.get(
                clientKey,
                key -> new Window()
        );

        if (!window.tryConsume()) {
            respondTooManyRequests(response, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void respondTooManyRequests(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {

        response.setStatus(429);

        response.setHeader(
                "Retry-After",
                String.valueOf(WINDOW_MILLIS / 1000)
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", Instant.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put(
                "message",
                "Rate limit exceeded. Please try again in a minute."
        );
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }

    private static final class Window {

        private final AtomicInteger count = new AtomicInteger(0);

        private volatile long windowStart =
                System.currentTimeMillis();

        synchronized boolean tryConsume() {

            long now = System.currentTimeMillis();

            if (now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                count.set(0);
            }

            return count.incrementAndGet()
                    <= MAX_REQUESTS_PER_WINDOW;
        }
    }
}