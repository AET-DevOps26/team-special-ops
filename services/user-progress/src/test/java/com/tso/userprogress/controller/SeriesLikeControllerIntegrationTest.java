package com.tso.userprogress.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tso.userprogress.model.SignupRequest;
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
class SeriesLikeControllerIntegrationTest {

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

  private void like(String token, UUID series) throws Exception {
    mockMvc
        .perform(put("/user-progress/likes/" + series).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void likeThenListReturnsIt() throws Exception {
    String token = signupAndGetToken("like1@example.com");
    UUID series = UUID.randomUUID();

    like(token, series);

    mockMvc
        .perform(get("/user-progress/likes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(series.toString()));
  }

  @Test
  void likeIsIdempotent() throws Exception {
    String token = signupAndGetToken("like2@example.com");
    UUID series = UUID.randomUUID();

    like(token, series);
    like(token, series); // liking again must not error or create a second row

    mockMvc
        .perform(get("/user-progress/likes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void unlikeRemovesTheLikeAndIsIdempotent() throws Exception {
    String token = signupAndGetToken("like3@example.com");
    UUID series = UUID.randomUUID();

    like(token, series);

    mockMvc
        .perform(
            delete("/user-progress/likes/" + series).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    // Unliking again is a no-op, not an error.
    mockMvc
        .perform(
            delete("/user-progress/likes/" + series).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/user-progress/likes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void listReturnsNewestLikedFirst() throws Exception {
    String token = signupAndGetToken("like4@example.com");
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    like(token, first);
    Thread.sleep(10); // ensure a distinct created_at ordering
    like(token, second);

    mockMvc
        .perform(get("/user-progress/likes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0]").value(second.toString()))
        .andExpect(jsonPath("$[1]").value(first.toString()));
  }

  @Test
  void listReturnsOnlyCallersLikes() throws Exception {
    String tokenA = signupAndGetToken("likeA@example.com");
    String tokenB = signupAndGetToken("likeB@example.com");
    UUID seriesA = UUID.randomUUID();
    UUID seriesB = UUID.randomUUID();

    like(tokenA, seriesA);
    like(tokenB, seriesB);

    mockMvc
        .perform(get("/user-progress/likes").header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(seriesB.toString()));
  }

  @Test
  void likeEndpointsRequireAuth() throws Exception {
    UUID series = UUID.randomUUID();
    mockMvc.perform(get("/user-progress/likes")).andExpect(status().is4xxClientError());
    mockMvc.perform(put("/user-progress/likes/" + series)).andExpect(status().is4xxClientError());
    mockMvc
        .perform(delete("/user-progress/likes/" + series))
        .andExpect(status().is4xxClientError());
  }
}
