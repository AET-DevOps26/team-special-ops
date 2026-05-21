package com.tso.userprogress.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tso.userprogress.model.SignupRequest;
import com.tso.userprogress.model.UpdateProgressRequest;
import java.util.UUID;
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
class ProgressControllerIntegrationTest {

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

  /** Signs up a user and returns their bearer access token. */
  private String signupAndGetToken(String email) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/user-progress/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(new SignupRequest(email, "password123"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("accessToken").asText();
  }

  private String body(UUID seriesId, int episodeIndex) throws Exception {
    return objectMapper.writeValueAsString(
        new UpdateProgressRequest().seriesId(seriesId).episodeIndex(episodeIndex));
  }

  @Test
  void putCreatesProgressThenGetReturnsIt() throws Exception {
    String token = signupAndGetToken("p1@example.com");
    UUID series = UUID.randomUUID();

    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(series, 5)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seriesId").value(series.toString()))
        .andExpect(jsonPath("$.episodeIndex").value(5))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));

    mockMvc
        .perform(get("/user-progress/progress").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].seriesId").value(series.toString()))
        .andExpect(jsonPath("$[0].episodeIndex").value(5));
  }

  @Test
  void putUpsertsAndAllowsMovingBackwards() throws Exception {
    String token = signupAndGetToken("p2@example.com");
    UUID series = UUID.randomUUID();

    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(series, 9)))
        .andExpect(status().isOk());

    // Same series, lower index — allowed (synced-rewatch scenario).
    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(series, 3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.episodeIndex").value(3));

    mockMvc
        .perform(get("/user-progress/progress").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1)) // upsert, not a second row
        .andExpect(jsonPath("$[0].episodeIndex").value(3));
  }

  @Test
  void getReturnsOnlyCallersEntries() throws Exception {
    String tokenA = signupAndGetToken("a@example.com");
    String tokenB = signupAndGetToken("b@example.com");
    UUID seriesA = UUID.randomUUID();
    UUID seriesB = UUID.randomUUID();

    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(seriesA, 4)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(seriesB, 7)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/user-progress/progress").header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].seriesId").value(seriesB.toString()));
  }

  @Test
  void putRejectsNegativeEpisodeIndex() throws Exception {
    String token = signupAndGetToken("neg@example.com");
    mockMvc
        .perform(
            put("/user-progress/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID(), -1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void progressEndpointsRequireAuth() throws Exception {
    // No bearer token → Spring Security rejects before the controller (4xx).
    mockMvc.perform(get("/user-progress/progress")).andExpect(status().is4xxClientError());
    mockMvc
        .perform(
            put("/user-progress/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID(), 1)))
        .andExpect(status().is4xxClientError());
  }
}
