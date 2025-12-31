package com.sss.sync.service.conflict;

import com.sss.sync.config.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConflictLinkTokenService {

  private final JwtProperties jwtProperties;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generate(long conflictId, String adminUsername) {
    Instant now = Instant.now();
    Instant exp = now.plus(24, ChronoUnit.HOURS);

    return Jwts.builder()
      .issuer(jwtProperties.getIssuer())
      .subject("conflict-view")
      .claims(Map.of(
        "conflictId", conflictId,
        "admin", adminUsername
      ))
      .issuedAt(Date.from(now))
      .expiration(Date.from(exp))
      .signWith(key())
      .compact();
  }

  public io.jsonwebtoken.Claims parse(String token) {
    return Jwts.parser()
      .verifyWith(key())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }
}