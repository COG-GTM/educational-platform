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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents per-client throttling of the anonymous sign-up endpoint (fixed window per remote address).
 */
@Component
public class SignUpRateLimitFilter extends OncePerRequestFilter {

    static final String SIGN_UP_PATH = "/users/sign-up";
    static final String TOO_MANY_REQUESTS_MESSAGE = "Too many sign-up attempts, please try again later";

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public SignUpRateLimitFilter(@Value("${com.educational.platform.security.sign-up.rate-limit.max-requests:10}") int maxRequests,
                                 @Value("${com.educational.platform.security.sign-up.rate-limit.window:PT1M}") Duration window,
                                 ObjectMapper objectMapper) {
        this(maxRequests, window, Clock.systemUTC(), objectMapper);
    }

    SignUpRateLimitFilter(int maxRequests, Duration window, Clock clock, ObjectMapper objectMapper) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !SIGN_UP_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final long now = clock.millis();
        evictExpired(now);

        final Window current = windows.compute(request.getRemoteAddr(), (address, existing) ->
                existing == null || existing.isExpired(now) ? new Window(now) : existing);

        if (current.count.incrementAndGet() > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (current.startedAt + window.toMillis() - now) / 1000)));
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(TOO_MANY_REQUESTS_MESSAGE)));
            return;
        }

        filterChain.doFilter(request, response);
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
