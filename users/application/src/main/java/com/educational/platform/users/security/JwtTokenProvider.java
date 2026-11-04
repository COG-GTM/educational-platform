package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents Jwt token provider.
 */
@Component
public class JwtTokenProvider {

    private final MyUserDetails myUserDetails;
    private final long validityInMilliseconds;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    /**
     * @param secretKey HMAC secret (at least 256 bits). When blank, a random key is generated on startup,
     *                  so issued tokens do not survive a restart.
     */
    public JwtTokenProvider(MyUserDetails myUserDetails,
                            @Value("${security.jwt.token.expire-length:3600000}") long validityInMilliseconds,
                            @Value("${security.jwt.token.secret-key:}") String secretKey) {
        this.myUserDetails = myUserDetails;
        this.validityInMilliseconds = validityInMilliseconds;
        this.secretKey = secretKey == null || secretKey.isBlank()
                ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .sig().add(Jwts.SIG.HS256).and()
                .build();
    }

    public String createToken(String username, List<Role> roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .claim("auth", roles.stream()
                        .map(s -> new SimpleGrantedAuthority(s.getAuthority()))
                        .collect(Collectors.toList()))
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
        return jwtParser.parseSignedClaims(token).getPayload().getSubject();
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
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenValidationException("Expired or invalid JWT token");
        }
    }

}
