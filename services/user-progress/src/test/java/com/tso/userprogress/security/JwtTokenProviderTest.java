package com.tso.userprogress.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;
  private static final String SECRET = "your-secret-key-min-32-chars-change-in-production";
  private static final long EXPIRATION = 900;
  private static final long REFRESH_EXPIRATION = 604800;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION, REFRESH_EXPIRATION);
  }

  @Test
  void testGenerateAccessToken() {
    UUID userId = UUID.randomUUID();
    String email = "test@example.com";

    String token = jwtTokenProvider.generateAccessToken(userId, email);

    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
  }

  @Test
  void testGenerateRefreshToken() {
    UUID userId = UUID.randomUUID();

    String token = jwtTokenProvider.generateRefreshToken(userId);

    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
  }

  @Test
  void testValidateToken_Valid() {
    UUID userId = UUID.randomUUID();
    String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com");

    assertTrue(jwtTokenProvider.validateToken(token));
  }

  @Test
  void testValidateToken_Invalid() {
    String invalidToken = "invalid.token.here";

    assertFalse(jwtTokenProvider.validateToken(invalidToken));
  }

  @Test
  void testValidateToken_Malformed() {
    String malformedToken = "not-a-jwt";

    assertFalse(jwtTokenProvider.validateToken(malformedToken));
  }

  @Test
  void testGetUserIdFromToken() {
    UUID userId = UUID.randomUUID();
    String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com");

    UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

    assertEquals(userId, extractedUserId);
  }

  @Test
  void testGetEmailFromToken() {
    UUID userId = UUID.randomUUID();
    String email = "user@example.com";
    String token = jwtTokenProvider.generateAccessToken(userId, email);

    String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

    assertEquals(email, extractedEmail);
  }
}
