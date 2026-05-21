package com.tso.userprogress.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tso.userprogress.model.LoginRequest;
import com.tso.userprogress.model.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("user_progress")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testSignupSuccess() throws Exception {
    SignupRequest request = new SignupRequest("test@example.com", "password123");

    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.id", notNullValue()))
        .andExpect(jsonPath("$.user.email").value("test@example.com"));
  }

  @Test
  void testSignupDuplicateEmail() throws Exception {
    SignupRequest request1 = new SignupRequest("test1@example.com", "password123");
    SignupRequest request2 = new SignupRequest("test1@example.com", "password123");

    // First signup should succeed
    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    // Second signup with same email should fail
    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
  }

  @Test
  void testLoginSuccess() throws Exception {
    // First create a user
    SignupRequest signupRequest = new SignupRequest("login@example.com", "password123");
    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
        .andExpect(status().isCreated());

    // Then login
    LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");
    mockMvc
        .perform(
            post("/user-progress/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.id", notNullValue()))
        .andExpect(jsonPath("$.user.email").value("login@example.com"));
  }

  @Test
  void testLoginInvalidCredentials() throws Exception {
    // First create a user
    SignupRequest signupRequest = new SignupRequest("invalid@example.com", "password123");
    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
        .andExpect(status().isCreated());

    // Try to login with wrong password
    LoginRequest loginRequest = new LoginRequest("invalid@example.com", "wrongpassword");
    mockMvc
        .perform(
            post("/user-progress/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void testSignupValidationFailure() throws Exception {
    SignupRequest request = new SignupRequest("invalid-email", "short");

    mockMvc
        .perform(
            post("/user-progress/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testUserProgressAuthMe() throws Exception {
    // First create a user
    String email = "authme@example.com";
    SignupRequest signupRequest = new SignupRequest(email, "password123");

    String response =
        mockMvc
            .perform(
                post("/user-progress/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract the token from signup response
    String accessToken = objectMapper.readTree(response).get("accessToken").asText();

    // Call /auth/me with the JWT token
    mockMvc
        .perform(get("/user-progress/auth/me").header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void testUserProgressAuthMeWithoutToken() throws Exception {
    mockMvc
        .perform(get("/user-progress/auth/me"))
        .andDo(print())
        .andExpect(status().is4xxClientError());
  }

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mockMvc
        .perform(get("/user-progress/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("user-progress"));
  }
}
