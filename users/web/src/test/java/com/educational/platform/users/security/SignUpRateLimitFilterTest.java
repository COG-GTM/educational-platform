package com.educational.platform.users.security;

import com.educational.platform.web.handler.ErrorResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
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

    @Test
    void doFilter_getSignUp_notThrottled() throws Exception {
        for (int i = 0; i < 5; i++) {
            final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/sign-up");
            request.setRequestURI("/users/sign-up");
            request.setRemoteAddr("10.0.0.1");
            sut.doFilter(request, new MockHttpServletResponse(), chain);
        }

        assertThat(passed.get()).isEqualTo(5);
    }

    @Test
    void doFilter_limitExceeded_subsequentRequestsInWindowRejected() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);

        final MockHttpServletResponse third = new MockHttpServletResponse();
        final MockHttpServletResponse fourth = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), third, chain);
        clock.advance(Duration.ofSeconds(30));
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), fourth, chain);

        assertThat(passed.get()).isEqualTo(2);
        assertThat(third.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(fourth.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_limitExceeded_retryAfterIsRemainingWindowInSeconds() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        clock.advance(Duration.ofSeconds(20));

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(response.getHeader("Retry-After")).isEqualTo("40");
    }

    @Test
    void doFilter_limitExceededJustBeforeWindowEnd_retryAfterAtLeastOneSecond() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        clock.advance(Duration.ofMinutes(1).minusMillis(1));

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(passed.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    }

    @Test
    void doFilter_limitExceeded_jsonErrorResponseBody() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        final ErrorResponse body = new ObjectMapper().readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.errors()).containsExactly(SignUpRateLimitFilter.TOO_MANY_REQUESTS_MESSAGE);
    }

    @Test
    void doFilter_windowElapsedAfterRejection_freshWindowStarted() throws Exception {
        for (int i = 0; i < 3; i++) {
            sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        }
        clock.advance(Duration.ofMinutes(1));

        final MockHttpServletResponse first = new MockHttpServletResponse();
        final MockHttpServletResponse second = new MockHttpServletResponse();
        final MockHttpServletResponse third = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), first, chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), second, chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), third, chain);

        assertThat(passed.get()).isEqualTo(4);
        assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(third.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_concurrentRequestsFromSameClient_exactlyMaxRequestsAdmitted() throws Exception {
        final int attempts = 32;
        final CountDownLatch start = new CountDownLatch(1);
        final List<MockHttpServletResponse> responses = new CopyOnWriteArrayList<>();
        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            final Thread thread = new Thread(() -> {
                try {
                    start.await();
                    final MockHttpServletResponse response = new MockHttpServletResponse();
                    sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);
                    responses.add(response);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(passed.get()).isEqualTo(2);
        assertThat(responses).hasSize(attempts);
        assertThat(responses).filteredOn(r -> r.getStatus() == HttpStatus.TOO_MANY_REQUESTS.value()).hasSize(attempts - 2);
    }

    @Test
    void doFilter_percentEncodedSignUpPath_throttled() throws Exception {
        sut.doFilter(post("/users/sign%2Dup", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign%2Dup", "10.0.0.1"), new MockHttpServletResponse(), chain);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(passed.get()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_limitExceededWithFractionalSecondRemaining_retryAfterRoundedUp() throws Exception {
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        clock.advance(Duration.ofSeconds(58).plusMillis(500));

        final MockHttpServletResponse response = new MockHttpServletResponse();
        sut.doFilter(post("/users/sign-up", "10.0.0.1"), response, chain);

        assertThat(response.getHeader("Retry-After")).isEqualTo("2");
    }

    @Test
    void doFilter_trackedClientsExhausted_newClientRejectedUntilWindowsExpire() throws Exception {
        final SignUpRateLimitFilter bounded = new SignUpRateLimitFilter(2, Duration.ofMinutes(1), 2, clock, new ObjectMapper());
        bounded.doFilter(post("/users/sign-up", "10.0.0.1"), new MockHttpServletResponse(), chain);
        bounded.doFilter(post("/users/sign-up", "10.0.0.2"), new MockHttpServletResponse(), chain);

        final MockHttpServletResponse rejected = new MockHttpServletResponse();
        bounded.doFilter(post("/users/sign-up", "10.0.0.3"), rejected, chain);
        final MockHttpServletResponse knownClient = new MockHttpServletResponse();
        bounded.doFilter(post("/users/sign-up", "10.0.0.1"), knownClient, chain);

        assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(knownClient.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(passed.get()).isEqualTo(3);

        clock.advance(Duration.ofMinutes(1));
        final MockHttpServletResponse admitted = new MockHttpServletResponse();
        bounded.doFilter(post("/users/sign-up", "10.0.0.3"), admitted, chain);

        assertThat(admitted.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(passed.get()).isEqualTo(4);
    }
}
