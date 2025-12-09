package com.smartspend.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    // Get signing key (safe fallbacks)
    private Key getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Fallback: string as raw secret
            return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }

    // Create token with userId inside (claim: id)
    public String generateJwtToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)                 // user email/username
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Extract username (email)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract user ID safely
    public Long extractUserId(String token) {
        try {
            Object id = extractClaim(token, claims -> claims.get("id"));
            return id != null ? Long.parseLong(id.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimResolver.apply(claims);
    }

    // Validate JWT safely (no exception propagation)
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("⚠ JWT expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("⚠ Unsupported JWT: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("⚠ Malformed JWT: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("⚠ Invalid JWT signature: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ Empty JWT claims: " + e.getMessage());
        }
        return false;
    }
}
