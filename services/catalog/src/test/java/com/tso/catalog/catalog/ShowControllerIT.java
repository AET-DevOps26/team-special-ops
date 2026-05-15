package com.tso.catalog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tso.catalog.support.PostgresIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresIT.Initializer.class)
class ShowControllerIT extends PostgresIT {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @Test
  void listShowsReturnsSeededShow() throws Exception {
    mvc.perform(get("/catalog/shows"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Stranger Things"))
        .andExpect(jsonPath("$[0].seasonsCount").isNumber())
        .andExpect(jsonPath("$[0].episodesCount").isNumber());
  }

  @Test
  void listShowEpisodesReturnsAllEpisodesInIndexOrder() throws Exception {
    MvcResult shows = mvc.perform(get("/catalog/shows")).andExpect(status().isOk()).andReturn();
    JsonNode body = mapper.readTree(shows.getResponse().getContentAsString());
    UUID showId = UUID.fromString(body.get(0).get("id").asText());
    int expectedCount = body.get(0).get("episodesCount").asInt();

    mvc.perform(get("/catalog/shows/" + showId + "/episodes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(expectedCount))
        .andExpect(jsonPath("$[0].episodeIndex").value(1))
        .andExpect(jsonPath("$[0].season").value(1))
        .andExpect(jsonPath("$[0].episodeNumber").value(1))
        .andExpect(jsonPath("$[0].title").isNotEmpty())
        .andExpect(jsonPath("$[0].summary").isNotEmpty());
  }

  @Test
  void listShowEpisodesReturns404ForUnknownShow() throws Exception {
    UUID missing = UUID.randomUUID();
    mvc.perform(get("/catalog/shows/" + missing + "/episodes"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHOW_NOT_FOUND"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
