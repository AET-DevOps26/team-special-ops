package com.tso.userprogress.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tso.userprogress.entity.User;
import com.tso.userprogress.exception.InvalidCredentialsException;
import com.tso.userprogress.exception.UserAlreadyExistsException;
import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.security.JwtTokenProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserService userService;

  @Mock private JwtTokenProvider jwtTokenProvider;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userService, jwtTokenProvider);
  }

  @Test
  void testSignupSuccess() {
    String email = "newuser@example.com";
    String password = "password123";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setEmail(email);

    when(userService.emailExists(email)).thenReturn(false);
    when(userService.createUser(email, password)).thenReturn(user);
    when(jwtTokenProvider.generateAccessToken(userId, email)).thenReturn("test-token");

    AuthResponse response = authService.signup(email, password);

    assertNotNull(response);
    assertEquals("test-token", response.getAccessToken());
    assertEquals(AuthResponse.TokenTypeEnum.BEARER, response.getTokenType());
    assertNotNull(response.getUser());
    assertEquals(email, response.getUser().getEmail());
    assertEquals(userId, response.getUser().getId());
  }

  @Test
  void testSignupEmailAlreadyExists() {
    String email = "existing@example.com";
    String password = "password123";

    when(userService.emailExists(email)).thenReturn(true);

    assertThrows(UserAlreadyExistsException.class, () -> authService.signup(email, password));

    verify(userService, never()).createUser(anyString(), anyString());
  }

  @Test
  void testLoginSuccess() {
    String email = "testuser@example.com";
    String password = "password123";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setEmail(email);
    user.setPasswordHash("hashed-password");

    when(userService.findByEmail(email)).thenReturn(Optional.of(user));
    when(userService.verifyPassword(password, "hashed-password")).thenReturn(true);
    when(jwtTokenProvider.generateAccessToken(userId, email)).thenReturn("test-token");

    AuthResponse response = authService.login(email, password);

    assertNotNull(response);
    assertEquals("test-token", response.getAccessToken());
    assertEquals(AuthResponse.TokenTypeEnum.BEARER, response.getTokenType());
    assertNotNull(response.getUser());
    assertEquals(email, response.getUser().getEmail());
    assertEquals(userId, response.getUser().getId());
  }

  @Test
  void testLoginUserNotFound() {
    String email = "nonexistent@example.com";
    String password = "password123";

    when(userService.findByEmail(email)).thenReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> authService.login(email, password));
  }

  @Test
  void testLoginWrongPassword() {
    String email = "testuser@example.com";
    String password = "wrongpassword";
    UUID userId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    user.setEmail(email);
    user.setPasswordHash("hashed-password");

    when(userService.findByEmail(email)).thenReturn(Optional.of(user));
    when(userService.verifyPassword(password, "hashed-password")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> authService.login(email, password));

    verify(jwtTokenProvider, never()).generateAccessToken(any(), anyString());
  }
}
