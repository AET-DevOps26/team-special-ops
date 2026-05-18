package com.tso.userprogress.service;

import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.entity.User;
import com.tso.userprogress.exception.InvalidCredentialsException;
import com.tso.userprogress.exception.UserAlreadyExistsException;
import com.tso.userprogress.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;
  private final long jwtExpiration;

  public AuthService(
      UserService userService,
      JwtTokenProvider jwtTokenProvider,
      @Value("${jwt.expiration}") long jwtExpiration) {
    this.userService = userService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtExpiration = jwtExpiration;
  }

  /**
   * Sign up a new user.
   *
   * @param username the username
   * @param email the email
   * @param password the password (minimum 8 characters)
   * @return AuthResponse containing JWT token
   * @throws UserAlreadyExistsException if username or email already exists
   */
  public AuthResponse signup(String username, String email, String password) {
    // Check if user already exists
    if (userService.usernameExists(username)) {
      throw new UserAlreadyExistsException("Username '" + username + "' is already taken");
    }

    if (userService.emailExists(email)) {
      throw new UserAlreadyExistsException("Email '" + email + "' is already registered");
    }

    // Create user
    User user = userService.createUser(username, email, password);

    // Generate token
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());

    return new AuthResponse(accessToken, AuthResponse.TokenTypeEnum.BEARER)
        .expiresIn(Math.toIntExact(jwtExpiration))
        .userId(user.getId());
  }

  /**
   * Login with username and password.
   *
   * @param username the username
   * @param password the password
   * @return AuthResponse containing JWT token
   * @throws InvalidCredentialsException if credentials are invalid
   */
  public AuthResponse login(String username, String password) {
    // Find user by username
    User user =
        userService
            .findByUsername(username)
            .orElseThrow(
                () -> new InvalidCredentialsException("Invalid username or password"));

    // Verify password
    if (!userService.verifyPassword(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid username or password");
    }

    // Generate token
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());

    return new AuthResponse(accessToken, AuthResponse.TokenTypeEnum.BEARER)
        .expiresIn(Math.toIntExact(jwtExpiration))
        .userId(user.getId());
  }
}

