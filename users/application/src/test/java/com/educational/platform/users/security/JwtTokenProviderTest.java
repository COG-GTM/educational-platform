package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    private static final String SECRET = randomSecret();

    @Mock
    private MyUserDetails myUserDetails;

    @Test
    void constructor_missingSecretKey_failsFast() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, ""))
                .withMessageContaining(JwtTokenProvider.SECRET_KEY_PROPERTY);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, null));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "   "));
    }

    @Test
    void constructor_shortSecretKey_failsFast() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "secret-key"))
                .withMessageContaining("at least 32 bytes");
    }

    @Test
    void constructor_missingSecretKey_messageExplainsHowToConfigure() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, ""))
                .withMessageContaining("SECURITY_JWT_TOKEN_SECRET_KEY");
    }

    @Test
    void constructor_secretKeyExactlyMinimumBytes_accepted() {
        // given
        final String key = "x".repeat(JwtTokenProvider.MIN_SECRET_KEY_BYTES);

        // when
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, key);

        // then
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void constructor_secretKeyOneByteBelowMinimum_failsFast() {
        // given
        final String key = "x".repeat(JwtTokenProvider.MIN_SECRET_KEY_BYTES - 1);

        // when / then
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, key))
                .withMessageContaining(JwtTokenProvider.SECRET_KEY_PROPERTY)
                .withMessageContaining("at least " + JwtTokenProvider.MIN_SECRET_KEY_BYTES + " bytes");
    }

    @Test
    void constructor_secretKeyLengthMeasuredInUtf8Bytes_notCharacters() {
        // given: 11 three-byte characters = 33 bytes but only 11 characters
        final String multiByteKey = "\u20AC".repeat(11);
        assertThat(multiByteKey).hasSize(11);
        assertThat(multiByteKey.getBytes(StandardCharsets.UTF_8)).hasSize(33);

        // when
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, multiByteKey);

        // then
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void constructor_whitespaceDoesNotCountTowardsMinimumWhenKeyIsBlank() {
        // given: 40 spaces are >= 32 bytes but still a blank key
        final String blankKey = " ".repeat(40);

        // when / then
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, blankKey))
                .withMessageContaining("must be configured");
    }

    @Test
    void validateToken_sameKeyMaterialInSeparateInstances_tokenAccepted() {
        // given
        final JwtTokenProvider issuer = new JwtTokenProvider(myUserDetails, 3600000, SECRET);
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, SECRET);

        // when
        final String token = issuer.createToken("username", Collections.singletonList(Role.ROLE_TEACHER));

        // then
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    private static String randomSecret() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    @Test
    void createToken_configuredSecretKey_tokenValidatesWithSameKey() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, SECRET);

        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tokenSignedWithOtherKey_rejected() {
        final JwtTokenProvider issuer = new JwtTokenProvider(myUserDetails, 3600000, randomSecret());
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, SECRET);

        final String token = issuer.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token));
    }

}
