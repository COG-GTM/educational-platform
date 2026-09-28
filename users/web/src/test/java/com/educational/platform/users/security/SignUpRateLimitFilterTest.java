package com.educational.platform.users.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SignUpRateLimitFilterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final AtomicInteger passed = new AtomicInteger();
    private final FilterChain chain = (request, response) -> passed.incrementAndGet();
    private final MutableClock clock = new MutableClock();
    private final SignUpRateLimitFilter sut = new SignUpRateLimitFilter(2, Duration.ofMinutes(1), clock, new ObjectMapper());

    private static MockHttpServletRequest post(String uri, String remoteAddress) {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    @Test
    void doFilter_limitExceededForClient_tooManyRequests() throws Exception {
        final MockHttpServletResponse third = new MockHttpServletResponse();

        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), third, chain);

        assertThat(passed.get()).isEqualTo(2);
        assertThat(third.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(third.getHeader("Retry-After")).isNotBlank();
        assertThat(third.getContentAsString()).contains(SignUpRateLimitFilter.TOO_MANY_REQUESTS_MESSAGE);
    }

    @Test
    void doFilter_otherClient_notAffected() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);

        final MockHttpServletResponse other = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.2"), other, chain);

        assertThat(passed.get()).isEqualTo(3);
        assertThat(other.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilter_windowElapsed_limitReset() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        clock.advance(Duration.ofMinutes(1));

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(passed.get()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilter_otherEndpoint_notThrottled() throws Exception {
        for (int i = 0; i < 5; i++) {
            sut.doFilter(post("/users/sign-in", "10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        assertThat(passed.get()).isEqualTo(5);
    }
}
