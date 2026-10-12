package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents Jwt token provider.
 */
@Component
public class JwtTokenProvider {

    static final String SECRET_KEY_PROPERTY = "security.jwt.token.secret-key";
    static final int MIN_SECRET_KEY_BYTES = 32;

    private final MyUserDetails myUserDetails;
    private final long validityInMilliseconds;
    private final String secretKey;

    public JwtTokenProvider(MyUserDetails myUserDetails,
                            @Value("${security.jwt.token.expire-length:3600000}") long validityInMilliseconds,
                            @Value("${" + SECRET_KEY_PROPERTY + ":}") String secretKey) {
        this.myUserDetails = myUserDetails;
        this.validityInMilliseconds = validityInMilliseconds;
        this.secretKey = Base64.getEncoder().encodeToString(requireSecretKey(secretKey));
    }

    private static byte[] requireSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(SECRET_KEY_PROPERTY + " must be configured (e.g. via the SECURITY_JWT_TOKEN_SECRET_KEY environment variable)");
        }
        final byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_KEY_BYTES) {
            throw new IllegalStateException(SECRET_KEY_PROPERTY + " must be at least " + MIN_SECRET_KEY_BYTES + " bytes for HS256");
        }
        return keyBytes;
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
                .signWith(SignatureAlgorithm.HS256, secretKey)
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
