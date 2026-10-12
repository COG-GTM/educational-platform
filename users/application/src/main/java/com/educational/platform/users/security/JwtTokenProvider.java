package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents Jwt token provider.
 */
@Component
public class JwtTokenProvider {

    private static final int MIN_KEY_LENGTH_BYTES = 32;

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    private final MyUserDetails myUserDetails;
    private final long validityInMilliseconds;
    private final SecretKey secretKey;

    public JwtTokenProvider(MyUserDetails myUserDetails,
                            @Value("${security.jwt.token.expire-length:3600000}") long validityInMilliseconds,
                            @Value("${security.jwt.token.secret-key:}") String secretKey) {
        this.myUserDetails = myUserDetails;
        this.validityInMilliseconds = validityInMilliseconds;
        this.secretKey = signingKey(secretKey);
    }

    private static SecretKey signingKey(String configuredKey) {
        final byte[] keyBytes;
        if (configuredKey == null || configuredKey.isBlank()) {
            LOGGER.warn("security.jwt.token.secret-key is not set; using a random {}-bit key. "
                    + "Issued tokens will not survive a restart or be accepted by other instances.", MIN_KEY_LENGTH_BYTES * 8);
            keyBytes = new byte[MIN_KEY_LENGTH_BYTES];
            new SecureRandom().nextBytes(keyBytes);
        } else {
            keyBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MIN_KEY_LENGTH_BYTES) {
                throw new IllegalStateException(String.format(
                        "security.jwt.token.secret-key must be at least %d bits for %s, but was %d bits",
                        MIN_KEY_LENGTH_BYTES * 8, SIGNATURE_ALGORITHM.getValue(), keyBytes.length * 8));
            }
        }
        return new SecretKeySpec(keyBytes, SIGNATURE_ALGORITHM.getJcaName());
    }


    public String createToken(String username, List<Role> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("auth", roles.stream()
                .map(s -> new SimpleGrantedAuthority(s.getAuthority()))
                .collect(Collectors.toList())
        );

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = myUserDetails.loadUserByUsername(getUsername(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenValidationException("Expired or invalid JWT token");
        }
    }

}
