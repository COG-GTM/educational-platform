package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenProperties;
import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
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
    private final String secretKey;
    private final String previousSecretKey;

    public JwtTokenProvider(MyUserDetails myUserDetails, JwtTokenProperties jwtTokenProperties) {
        this.myUserDetails = myUserDetails;
        this.validityInMilliseconds = jwtTokenProperties.getExpireLength();
        this.secretKey = encode(jwtTokenProperties.getSecretKey());
        this.previousSecretKey = jwtTokenProperties.getPreviousSecretKey() == null || jwtTokenProperties.getPreviousSecretKey().isBlank()
                ? null
                : encode(jwtTokenProperties.getPreviousSecretKey());
    }

    private static String encode(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes());
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
        return parseClaims(token).getBody().getSubject();
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
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenValidationException("Expired or invalid JWT token");
        }
    }

    private Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
        } catch (SignatureException e) {
            if (previousSecretKey == null) {
                throw e;
            }
            return Jwts.parser().setSigningKey(previousSecretKey).parseClaimsJws(token);
        }
    }

}
