package com.tso.chat.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class TestJwtFactory {

  private static final String SECRET = "your-secret-key-min-32-chars-change-in-production";
  private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

  private TestJwtFactory() {}

  public static String tokenFor(UUID userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + 900_000);
    return Jwts.builder()
        .setSubject(userId.toString())
        .claim("email", "test@example.com")
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(KEY, SignatureAlgorithm.HS256)
        .compact();
  }
}
