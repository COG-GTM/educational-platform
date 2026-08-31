package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Represents Jwt token provider.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final MyUserDetails myUserDetails;
    private final long validityInMilliseconds;
    private final SecretKey secretKey;

    public JwtTokenProvider(MyUserDetails myUserDetails,
                            @Value("${security.jwt.token.expire-length:3600000}") long validityInMilliseconds,
                            @Value("${security.jwt.token.secret-key:}") String secretKey) {
        this.myUserDetails = myUserDetails;
        this.validityInMilliseconds = validityInMilliseconds;
        this.secretKey = resolveSecretKey(secretKey);
    }

    private static SecretKey resolveSecretKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("security.jwt.token.secret-key is not configured; a random HS256 key is generated and issued tokens won't survive a restart");
            return Jwts.SIG.HS256.key().build();
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.token.secret-key must be at least 32 bytes long for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String username, List<Role> roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .claim("auth", roles.stream().map(Role::getAuthority).toList())
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = myUserDetails.loadUserByUsername(getUsername(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
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
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenValidationException("Expired or invalid JWT token");
        }
    }

}
