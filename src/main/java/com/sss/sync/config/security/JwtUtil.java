package com.sss.sync.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

  private final JwtProperties props;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(Long userId, String username, String role) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.getAccessTokenExpireMinutes(), ChronoUnit.MINUTES);

    return Jwts.builder()
      .issuer(props.getIssuer())
      .subject(String.valueOf(userId))
      .claims(Map.of(
        "username", username,
        "role", role
      ))
      .issuedAt(Date.from(now))
      .expiration(Date.from(exp))
      .signWith(key())
      .compact();
  }

  public io.jsonwebtoken.Claims parseClaims(String token) {
    return Jwts.parser()
      .verifyWith(key())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }
}