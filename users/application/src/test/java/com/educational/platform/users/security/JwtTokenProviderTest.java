package com.educational.platform.users.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class JwtTokenProviderTest {

    private static final String STRONG_KEY = "0123456789abcdef0123456789abcdef";

    private final MyUserDetails myUserDetails = mock(MyUserDetails.class);

    @Test
    void constructor_keyShorterThan256Bits_rejected() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "secret-key"))
                .withMessageContaining("at least 256 bits");
    }

    @Test
    void constructor_keyOneByteShort_rejected() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY.substring(1)));
    }

    @Test
    void constructor_keyTooShort_messageReportsRequiredAlgorithmAndActualBits() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "secret-key"))
                .withMessage("security.jwt.token.secret-key must be at least 256 bits for HS256, but was 80 bits");
    }

    @Test
    void constructor_keyLengthMeasuredInUtf8Bytes_notCharacters() {
        // given: 16 two-byte characters = 32 bytes, 10 three-byte characters = 30 bytes
        final String sixteenTwoByteChars = "\u00e9".repeat(16);
        final String tenThreeByteChars = "\u20ac".repeat(10);

        // then
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, sixteenTwoByteChars);
        assertThat(sut.validateToken(sut.createToken("username", List.of(Role.ROLE_STUDENT)))).isTrue();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, tenThreeByteChars))
                .withMessageContaining("but was 240 bits");
    }

    @Test
    void constructor_keyLongerThan256Bits_accepted() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY + STRONG_KEY);

        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void constructor_whitespaceOnlyKey_treatedAsNotConfigured() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "   ");
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, 3600000, "   ");

        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(sut.validateToken(token)).isTrue();
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> other.validateToken(token));
    }

    @Test
    void constructor_noKeyConfigured_logsWarning() {
        // given
        final Logger logger = (Logger) LoggerFactory.getLogger(JwtTokenProvider.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            new JwtTokenProvider(myUserDetails, 3600000, "");

            // then
            assertThat(appender.list)
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage())
                                .contains("security.jwt.token.secret-key is not set")
                                .contains("256-bit")
                                .contains("will not survive a restart");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void constructor_strongKeyConfigured_doesNotLogWarning() {
        // given
        final Logger logger = (Logger) LoggerFactory.getLogger(JwtTokenProvider.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);

            // then
            assertThat(appender.list).isEmpty();
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void createToken_256BitKey_signedWithHs256AndValid() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);

        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(header(token)).contains("\"alg\":\"HS256\"");
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tokenSignedWithOtherKey_rejected() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, 3600000, "fedcba9876543210fedcba9876543210");

        final String token = other.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_tokenForgedWithFormerDefaultKey_rejected() throws Exception {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "");
        final String forged = hs256("{\"alg\":\"HS256\"}",
                "{\"sub\":\"admin\",\"exp\":" + (System.currentTimeMillis() / 1000 + 60) + "}",
                "secret-key".getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(forged));
    }

    @Test
    void validateToken_tokenIssuedByFormerImplementationWithStrongKey_stillAccepted() throws Exception {
        // given: the former Base64 encode/decode round trip signed with the raw UTF-8 key bytes
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);
        final String legacyToken = hs256("{\"alg\":\"HS256\"}",
                "{\"sub\":\"username\",\"exp\":" + (System.currentTimeMillis() / 1000 + 60) + "}",
                STRONG_KEY.getBytes(StandardCharsets.UTF_8));

        // then
        assertThat(sut.validateToken(legacyToken)).isTrue();
        assertThat(sut.getUsername(legacyToken)).isEqualTo("username");
    }

    @Test
    void validateToken_sameConfiguredKeyAcrossInstances_accepted() {
        final JwtTokenProvider issuer = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);
        final JwtTokenProvider verifier = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);

        final String token = issuer.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(verifier.validateToken(token)).isTrue();
        assertThat(verifier.getUsername(token)).isEqualTo("username");
    }

    @Test
    void constructor_noKeyConfigured_usesRandomPerInstanceKey() {
        final JwtTokenProvider first = new JwtTokenProvider(myUserDetails, 3600000, "");
        final JwtTokenProvider second = new JwtTokenProvider(myUserDetails, 3600000, null);

        final String token = first.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(first.validateToken(token)).isTrue();
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> second.validateToken(token));
    }

    private static String hs256(String header, String payload, byte[] key) throws Exception {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String signingInput = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return signingInput + "." + encoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private static String header(String token) {
        return new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), StandardCharsets.UTF_8);
    }
}
