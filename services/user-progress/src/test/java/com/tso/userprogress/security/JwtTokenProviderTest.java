package com.tso.userprogress.security;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

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
    String username = "testuser";

    String token = jwtTokenProvider.generateAccessToken(userId, username);

    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
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
    String token = jwtTokenProvider.generateAccessToken(userId, "testuser");

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
    String token = jwtTokenProvider.generateAccessToken(userId, "testuser");

    UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

    assertEquals(userId, extractedUserId);
  }

  @Test
  void testGetUsernameFromToken() {
    UUID userId = UUID.randomUUID();
    String username = "testuser123";
    String token = jwtTokenProvider.generateAccessToken(userId, username);

    String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    assertEquals(username, extractedUsername);
  }
}

