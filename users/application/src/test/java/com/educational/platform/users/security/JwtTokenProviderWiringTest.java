package com.educational.platform.users.security;

import com.educational.platform.users.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Verifies the Spring wiring of {@link JwtTokenProvider}: the {@code @Value} binding has no default anymore,
 * so a context without {@code security.jwt.token.secret-key} must refuse to start.
 */
public class JwtTokenProviderWiringTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void contextRefresh_secretKeyPropertyAbsent_failsWithConfigurationHint() {
        // given
        final AnnotationConfigApplicationContext sut = contextWith(new MockEnvironment());

        // when / then
        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(sut::refresh)
                .withRootCauseInstanceOf(IllegalStateException.class)
                .withStackTraceContaining(JwtTokenProvider.SECRET_KEY_PROPERTY + " must be configured");
    }

    @Test
    void contextRefresh_secretKeyPropertyTooShort_fails() {
        // given
        final AnnotationConfigApplicationContext sut = contextWith(new MockEnvironment()
                .withProperty(JwtTokenProvider.SECRET_KEY_PROPERTY, "secret-key"));

        // when / then
        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(sut::refresh)
                .withRootCauseInstanceOf(IllegalStateException.class)
                .withStackTraceContaining("at least " + JwtTokenProvider.MIN_SECRET_KEY_BYTES + " bytes");
    }

    @Test
    void contextRefresh_secretKeyPropertyConfigured_providerIssuesValidTokens() {
        // given
        final String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        context = contextWith(new MockEnvironment().withProperty(JwtTokenProvider.SECRET_KEY_PROPERTY, secret));

        // when
        context.refresh();
        final JwtTokenProvider provider = context.getBean(JwtTokenProvider.class);
        final String token = provider.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(new JwtTokenProvider(mock(MyUserDetails.class), 3600000, secret).getUsername(token))
                .isEqualTo("username");
    }

    @Test
    void contextRefresh_gradleProvidedTestKeyInSystemProperties_providerStarts() {
        // given: build.gradle.kts passes a random key to every Test task as a system property
        final String gradleKey = System.getProperty(JwtTokenProvider.SECRET_KEY_PROPERTY);
        Assumptions.assumeTrue(gradleKey != null, "test key is only injected when running under Gradle");
        context = new AnnotationConfigApplicationContext();
        context.registerBean(MyUserDetails.class, () -> mock(MyUserDetails.class));
        context.registerBean(JwtTokenProvider.class);

        // when
        context.refresh();

        // then
        assertThat(gradleKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .hasSizeGreaterThanOrEqualTo(JwtTokenProvider.MIN_SECRET_KEY_BYTES);
        assertThat(context.getBean(JwtTokenProvider.class)).isNotNull();
    }

    private AnnotationConfigApplicationContext contextWith(MockEnvironment environment) {
        context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.registerBean(MyUserDetails.class, () -> mock(MyUserDetails.class));
        context.registerBean(JwtTokenProvider.class);
        return context;
    }

}
