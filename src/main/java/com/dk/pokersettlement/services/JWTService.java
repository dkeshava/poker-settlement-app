package com.dk.pokersettlement.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JWTService {

    private static final String DEFAULT_DEV_SECRET =
            "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdTZWNyZXRLZXlGb3JKV1QxMjM0NTY3ODkw";

    @Autowired
    private Environment environment;

    @Value("${jwt.secret:}")
    private String configuredSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private String secret;

    @PostConstruct
    public void init() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            String trimmed = configuredSecret.trim();

            if (isProd && trimmed.length() < 32) {
                throw new IllegalStateException(
                        "SECURITY ERROR: JWT_SECRET must be at least 32 characters in production!");
            }

            try {
                Decoders.BASE64.decode(trimmed);
                this.secret = trimmed;
            } catch (Exception e) {
                this.secret = Base64.getEncoder().encodeToString(trimmed.getBytes(StandardCharsets.UTF_8));
            }
            log.info("Initialized JWT secret key from external configuration.");
        } else {
            if (isProd) {
                throw new IllegalStateException(
                        "SECURITY ERROR: JWT_SECRET environment variable is REQUIRED when running with profile 'prod'!");
            }
            this.secret = DEFAULT_DEV_SECRET;
            log.warn("No JWT secret provided in configuration! Using default dev key for local development.");
        }
    }

    private final Map<String, Object> claims = new HashMap<>();

    public String generateToken(String username) {
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .and()
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        // extract the username from jwt token
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}