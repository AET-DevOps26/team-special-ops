package com.tso.userprogress.service;

import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.entity.User;
import com.tso.userprogress.exception.InvalidCredentialsException;
import com.tso.userprogress.exception.UserAlreadyExistsException;
import com.tso.userprogress.security.JwtTokenProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserService userService;

  @Mock private JwtTokenProvider jwtTokenProvider;

  private AuthService authService;
  private static final int JWT_EXPIRATION = 900;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userService, jwtTokenProvider, JWT_EXPIRATION);
  }

  @Test
  void testSignupSuccess() {
    String username = "newuser";
    String email = "newuser@example.com";
    String password = "password123";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    user.setEmail(email);

    when(userService.usernameExists(username)).thenReturn(false);
    when(userService.emailExists(email)).thenReturn(false);
    when(userService.createUser(username, email, password)).thenReturn(user);
    when(jwtTokenProvider.generateAccessToken(userId, username)).thenReturn("test-token");

    AuthResponse response = authService.signup(username, email, password);

    assertNotNull(response);
    assertEquals("test-token", response.getAccessToken());
    assertEquals(AuthResponse.TokenTypeEnum.BEARER, response.getTokenType());
    assertEquals(JWT_EXPIRATION, response.getExpiresIn());
    assertEquals(userId, response.getUserId());
  }

  @Test
  void testSignupUsernameAlreadyExists() {
    String username = "existinguser";
    String email = "newuser@example.com";
    String password = "password123";

    when(userService.usernameExists(username)).thenReturn(true);

    assertThrows(
        UserAlreadyExistsException.class, () -> authService.signup(username, email, password));

    verify(userService, never()).createUser(anyString(), anyString(), anyString());
  }

  @Test
  void testSignupEmailAlreadyExists() {
    String username = "newuser";
    String email = "existing@example.com";
    String password = "password123";

    when(userService.usernameExists(username)).thenReturn(false);
    when(userService.emailExists(email)).thenReturn(true);

    assertThrows(
        UserAlreadyExistsException.class, () -> authService.signup(username, email, password));

    verify(userService, never()).createUser(anyString(), anyString(), anyString());
  }

  @Test
  void testLoginSuccess() {
    String username = "testuser";
    String password = "password123";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    user.setPasswordHash("hashed-password");

    when(userService.findByUsername(username)).thenReturn(Optional.of(user));
    when(userService.verifyPassword(password, "hashed-password")).thenReturn(true);
    when(jwtTokenProvider.generateAccessToken(userId, username)).thenReturn("test-token");

    AuthResponse response = authService.login(username, password);

    assertNotNull(response);
    assertEquals("test-token", response.getAccessToken());
    assertEquals(AuthResponse.TokenTypeEnum.BEARER, response.getTokenType());
    assertEquals(JWT_EXPIRATION, response.getExpiresIn());
    assertEquals(userId, response.getUserId());
  }

  @Test
  void testLoginUserNotFound() {
    String username = "nonexistent";
    String password = "password123";

    when(userService.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> authService.login(username, password));
  }

  @Test
  void testLoginWrongPassword() {
    String username = "testuser";
    String password = "wrongpassword";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    user.setPasswordHash("hashed-password");

    when(userService.findByUsername(username)).thenReturn(Optional.of(user));
    when(userService.verifyPassword(password, "hashed-password")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> authService.login(username, password));

    verify(jwtTokenProvider, never()).generateAccessToken(any(), anyString());
  }
}

