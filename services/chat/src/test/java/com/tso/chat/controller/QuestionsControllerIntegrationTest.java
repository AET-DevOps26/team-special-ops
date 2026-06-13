package com.tso.chat.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tso.chat.client.GenAiClient;
import com.tso.chat.model.ChatQuestionRequest;
import com.tso.chat.model.GenAiAskResponse;
import com.tso.chat.support.ChatPostgresIT;
import com.tso.chat.support.TestJwtFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = ChatPostgresIT.Initializer.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class QuestionsControllerIntegrationTest extends ChatPostgresIT {

  static final UUID SERIES_ID = UUID.fromString("958661e6-226c-5117-9318-5e3265598767");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;

  @MockBean GenAiClient genAiClient;

  @Test
  void askQuestionReturnsAnswerWithCitations() throws Exception {
    UUID userId = UUID.randomUUID();
    seedUserAndProgress(userId, 2);

    when(genAiClient.postAsk(eq("Who is Eleven?"), anyList()))
        .thenReturn(
            new GenAiAskResponse()
                .answer("Eleven is a girl with telekinetic powers.")
                .citedEpisodeIndices(List.of(1, 2)));

    String token = TestJwtFactory.tokenFor(userId);
    ChatQuestionRequest body =
        new ChatQuestionRequest().seriesId(SERIES_ID).question("Who is Eleven?");

    mockMvc
        .perform(
            post("/chat/questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.question").value("Who is Eleven?"))
        .andExpect(jsonPath("$.answer").value("Eleven is a girl with telekinetic powers."))
        .andExpect(jsonPath("$.citedEpisodeIndices", hasSize(2)))
        .andExpect(jsonPath("$.citedEpisodeIndices", hasItem(1)))
        .andExpect(jsonPath("$.progressAtAsk").value(2));
  }

  @Test
  void spoilerTrapFiltersCitationsBeyondProgress() throws Exception {
    UUID userId = UUID.randomUUID();
    seedUserAndProgress(userId, 1);

    when(genAiClient.postAsk(eq("What happens to Barb in later episodes?"), anyList()))
        .thenReturn(
            new GenAiAskResponse()
                .answer("I only know about early events.")
                .citedEpisodeIndices(List.of(1, 3)));

    String token = TestJwtFactory.tokenFor(userId);
    ChatQuestionRequest body =
        new ChatQuestionRequest()
            .seriesId(SERIES_ID)
            .question("What happens to Barb in later episodes?");

    mockMvc
        .perform(
            post("/chat/questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.citedEpisodeIndices", hasSize(1)))
        .andExpect(jsonPath("$.citedEpisodeIndices[0]", lessThanOrEqualTo(1)))
        .andExpect(jsonPath("$.progressAtAsk").value(1));
  }

  @Test
  void askQuestionRejectsWhenNoProgress() throws Exception {
    UUID userId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        userId,
        "noprog@example.com",
        "hash");

    String token = TestJwtFactory.tokenFor(userId);
    ChatQuestionRequest body =
        new ChatQuestionRequest().seriesId(SERIES_ID).question("Who is Eleven?");

    mockMvc
        .perform(
            post("/chat/questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("NO_PROGRESS"));
  }

  @Test
  void askQuestionReturns404ForUnknownSeries() throws Exception {
    UUID userId = UUID.randomUUID();
    seedUserAndProgress(userId, 2);
    String token = TestJwtFactory.tokenFor(userId);
    UUID missing = UUID.randomUUID();
    ChatQuestionRequest body =
        new ChatQuestionRequest().seriesId(missing).question("Who is Eleven?");

    mockMvc
        .perform(
            post("/chat/questions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SERIES_NOT_FOUND"));
  }

  @Test
  void askQuestionRequiresAuth() throws Exception {
    ChatQuestionRequest body =
        new ChatQuestionRequest().seriesId(SERIES_ID).question("Who is Eleven?");

    mockMvc
        .perform(
            post("/chat/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is4xxClientError());
  }

  private void seedUserAndProgress(UUID userId, int episodeIndex) {
    jdbc.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        userId,
        userId + "@example.com",
        "hash");
    jdbc.update(
        "INSERT INTO progress (user_id, series_id, episode_index) VALUES (?, ?, ?)",
        userId,
        SERIES_ID,
        episodeIndex);
  }
}
