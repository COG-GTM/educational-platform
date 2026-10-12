package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Pins how {@link JwtTokenProvider} binds {@code security.jwt.token.*} properties when created as a Spring bean.
 */
public class JwtTokenProviderPropertiesTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(MyUserDetails.class, () -> mock(MyUserDetails.class))
            .withBean(JwtTokenProvider.class);

    @Test
    void secretKeyPropertyNotSet_contextStartsWithRandomKey() {
        runner.run(context -> {
            // given
            assertThat(context).hasSingleBean(JwtTokenProvider.class);
            final JwtTokenProvider sut = context.getBean(JwtTokenProvider.class);

            // when
            final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

            // then
            assertThat(sut.validateToken(token)).isTrue();
            assertThatExceptionOfType(JwtTokenValidationException.class)
                    .isThrownBy(() -> new JwtTokenProvider(mock(MyUserDetails.class), 3600000L, SECRET).validateToken(token));
        });
    }

    @Test
    void secretKeyPropertyShorterThan32Bytes_contextFailsToStart() {
        runner.withPropertyValues("security.jwt.token.secret-key=secret-key").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("security.jwt.token.secret-key must be at least 32 bytes for HS256");
        });
    }

    @Test
    void secretKeyPropertySet_tokensVerifiableWithConfiguredSecret() {
        runner.withPropertyValues("security.jwt.token.secret-key=" + SECRET).run(context -> {
            // when
            final String token = context.getBean(JwtTokenProvider.class).createToken("username", List.of(Role.ROLE_TEACHER));

            // then
            final Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            assertThat(claims.getSubject()).isEqualTo("username");
            assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(3600000L);
        });
    }

    @Test
    void expireLengthPropertySet_expirationUsesConfiguredValidity() {
        runner.withPropertyValues("security.jwt.token.secret-key=" + SECRET, "security.jwt.token.expire-length=5000")
                .run(context -> {
                    // when
                    final String token = context.getBean(JwtTokenProvider.class).createToken("username", List.of());

                    // then
                    final Claims claims = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(5000L);
                });
    }
}
