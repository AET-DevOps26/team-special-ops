package com.tso.userprogress.service;

import com.tso.userprogress.entity.User;
import com.tso.userprogress.exception.InvalidCredentialsException;
import com.tso.userprogress.exception.UserAlreadyExistsException;
import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.model.UserSummary;
import com.tso.userprogress.security.JwtTokenProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserService userService;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(UserService userService, JwtTokenProvider jwtTokenProvider) {
    this.userService = userService;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * Sign up a new user.
   *
   * @param email the email
   * @param password the password (minimum 8 characters)
   * @return AuthResponse containing JWT token and user info
   * @throws UserAlreadyExistsException if email already exists
   */
  public AuthResponse signup(String email, String password) {
    // Check if user already exists
    if (userService.emailExists(email)) {
      throw new UserAlreadyExistsException("Email '" + email + "' is already registered");
    }

    // Create user
    User user = userService.createUser(email, password);

    // Generate token
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), email);

    UserSummary userSummary = new UserSummary(user.getId(), user.getEmail());
    return new AuthResponse(accessToken, AuthResponse.TokenTypeEnum.BEARER, userSummary);
  }

  /**
   * Login with email and password.
   *
   * @param email the email
   * @param password the password
   * @return AuthResponse containing JWT token and user info
   * @throws InvalidCredentialsException if credentials are invalid
   */
  public AuthResponse login(String email, String password) {
    // Find user by email
    User user =
        userService
            .findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    // Verify password
    if (!userService.verifyPassword(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    // Generate token
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), email);

    UserSummary userSummary = new UserSummary(user.getId(), user.getEmail());
    return new AuthResponse(accessToken, AuthResponse.TokenTypeEnum.BEARER, userSummary);
  }

  /**
   * Get current authenticated user.
   *
   * @param userId the user ID from JWT token
   * @return UserSummary with user information
   */
  public UserSummary getCurrentUser(String userId) {
    User user =
        userService
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    return new UserSummary(user.getId(), user.getEmail());
  }
}
