package com.educational.platform.users.security;

import com.educational.platform.web.handler.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents per-client throttling of the anonymous sign-up endpoint (fixed window per remote address).
 * The number of tracked clients is bounded; once the bound is reached, requests from untracked clients are
 * rejected until expired windows have been evicted.
 */
@Component
public class SignUpRateLimitFilter extends OncePerRequestFilter {

    static final String SIGN_UP_PATH = "/users/sign-up";
    static final String TOO_MANY_REQUESTS_MESSAGE = "Too many sign-up attempts, please try again later";
    static final int DEFAULT_MAX_TRACKED_CLIENTS = 100_000;

    private final RequestMatcher signUpMatcher = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, SIGN_UP_PATH);
    private final int maxRequests;
    private final Duration window;
    private final int maxTrackedClients;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastEvictionAt;

    @Autowired
    public SignUpRateLimitFilter(@Value("${com.educational.platform.security.sign-up.rate-limit.max-requests:10}") int maxRequests,
                                 @Value("${com.educational.platform.security.sign-up.rate-limit.window:PT1M}") Duration window,
                                 @Value("${com.educational.platform.security.sign-up.rate-limit.max-tracked-clients:" + DEFAULT_MAX_TRACKED_CLIENTS + "}") int maxTrackedClients,
                                 ObjectMapper objectMapper) {
        this(maxRequests, window, maxTrackedClients, Clock.systemUTC(), objectMapper);
    }

    SignUpRateLimitFilter(int maxRequests, Duration window, Clock clock, ObjectMapper objectMapper) {
        this(maxRequests, window, DEFAULT_MAX_TRACKED_CLIENTS, clock, objectMapper);
    }

    SignUpRateLimitFilter(int maxRequests, Duration window, int maxTrackedClients, Clock clock, ObjectMapper objectMapper) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.maxTrackedClients = maxTrackedClients;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.lastEvictionAt = new AtomicLong(clock.millis());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !signUpMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final long now = clock.millis();
        evictExpiredIfDue(now);

        final String client = request.getRemoteAddr();
        if (!windows.containsKey(client) && windows.size() >= maxTrackedClients) {
            evictExpired(now);
            if (windows.size() >= maxTrackedClients) {
                reject(response, window.toMillis());
                return;
            }
        }

        final Window current = windows.compute(client, (address, existing) ->
                existing == null || existing.isExpired(now) ? new Window(now) : existing);

        if (current.count.incrementAndGet() > maxRequests) {
            reject(response, current.startedAt + window.toMillis() - now);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfterMillis) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, (retryAfterMillis + 999) / 1000)));
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(TOO_MANY_REQUESTS_MESSAGE)));
    }

    private void evictExpiredIfDue(long now) {
        final long last = lastEvictionAt.get();
        if (now - last >= window.toMillis() && lastEvictionAt.compareAndSet(last, now)) {
            evictExpired(now);
        }
    }

    private void evictExpired(long now) {
        windows.values().removeIf(w -> w.isExpired(now));
    }

    private final class Window {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(long now) {
            return now - startedAt >= window.toMillis();
        }
    }
}
