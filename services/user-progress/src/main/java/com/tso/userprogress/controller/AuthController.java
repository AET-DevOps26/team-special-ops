package com.tso.userprogress.controller;

import com.tso.userprogress.api.UserProgressApi;
import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.model.HealthStatus;
import com.tso.userprogress.model.LoginRequest;
import com.tso.userprogress.model.SignupRequest;
import com.tso.userprogress.model.UserSummary;
import com.tso.userprogress.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements UserProgressApi {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public ResponseEntity<AuthResponse> userProgressAuthSignup(SignupRequest signupRequest) {
    AuthResponse response =
        authService.signup(signupRequest.getEmail(), signupRequest.getPassword());
    return ResponseEntity.status(201).body(response);
  }

  public ResponseEntity<AuthResponse> userProgressAuthLogin(LoginRequest loginRequest) {
    AuthResponse response = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
    return ResponseEntity.ok(response);
  }

  public ResponseEntity<UserSummary> userProgressAuthMe() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(401).build();
    }

    // Extract user ID from JWT token (authentication principal should be the user ID)
    String userId = (String) authentication.getPrincipal();
    UserSummary user = authService.getCurrentUser(userId);
    return ResponseEntity.ok(user);
  }

  public ResponseEntity<HealthStatus> userProgressHealth() {
    HealthStatus body = new HealthStatus(HealthStatus.StatusEnum.OK, "user-progress");
    return ResponseEntity.ok(body);
  }
}
