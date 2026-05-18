package com.tso.userprogress.controller;

import com.tso.userprogress.api.UserProgressApi;
import com.tso.userprogress.model.AuthResponse;
import com.tso.userprogress.model.HealthStatus;
import com.tso.userprogress.model.LoginRequest;
import com.tso.userprogress.model.SignupRequest;
import com.tso.userprogress.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
class AuthController implements UserProgressApi {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
    AuthResponse response =
        authService.signup(
            signupRequest.getUsername(), signupRequest.getEmail(), signupRequest.getPassword());
    return ResponseEntity.status(201).body(response);
  }

  @Override
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    AuthResponse response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<HealthStatus> userProgressHealth(){
      HealthStatus body =
      new HealthStatus().status(HealthStatus.StatusEnum.OK).service("user-progress");
      return ResponseEntity.ok(body);
  }
}

