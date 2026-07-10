package com.tso.catalog.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class SeriesControllerIT extends PostgresIT {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @Test
  void listSeriesReturnsSeededSeries() throws Exception {
    mvc.perform(get("/catalog/series"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[*].title", hasItem("Stranger Things")))
        .andExpect(jsonPath("$[*].title", hasItem("Off Campus")))
        .andExpect(jsonPath("$[*].title", hasItem("Lupin")))
        .andExpect(jsonPath("$[*].seasonsCount").isNumber())
        .andExpect(jsonPath("$[0].episodesCount").isNumber());
  }

  @Test
  void listSeriesEpisodesReturnsAllEpisodesInIndexOrder() throws Exception {
    MvcResult series = mvc.perform(get("/catalog/series")).andExpect(status().isOk()).andReturn();
    JsonNode body = mapper.readTree(series.getResponse().getContentAsString());
    UUID seriesId = UUID.fromString(body.get(0).get("id").asText());
    int expectedCount = body.get(0).get("episodesCount").asInt();

    mvc.perform(get("/catalog/series/" + seriesId + "/episodes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(expectedCount))
        .andExpect(jsonPath("$[0].episodeIndex").value(1))
        .andExpect(jsonPath("$[0].season").value(1))
        .andExpect(jsonPath("$[0].episodeNumber").value(1))
        .andExpect(jsonPath("$[0].title").isNotEmpty())
        .andExpect(jsonPath("$[0].summary").isNotEmpty());
  }

  @Test
  void allEpisodeSummariesAreCleanPlotText() throws Exception {
    MvcResult series = mvc.perform(get("/catalog/series")).andExpect(status().isOk()).andReturn();
    JsonNode seriesBody = mapper.readTree(series.getResponse().getContentAsString());
      for (JsonNode seriesNode : seriesBody) {
          UUID seriesId = UUID.fromString(seriesNode.get("id").asText());

          MvcResult episodes =
              mvc.perform(get("/catalog/series/" + seriesId + "/episodes"))
                  .andExpect(status().isOk())
                  .andReturn();
          JsonNode body = mapper.readTree(episodes.getResponse().getContentAsString());

          for (JsonNode ep : body) {
              String title = ep.get("title").asText();
              String summary = ep.get("summary").asText();
              // Guards against the Wikipedia-parser bug where a malformed block let the
              // ShortSummary extraction run past the template into ==Production==,
              // wikitables, etc. (the original S3E8 summary was ~15k chars of dump).
              assertThat(summary).as("summary for '%s' should be present", title).isNotBlank();
              assertThat(summary.length())
                  .as("summary for '%s' should be a short plot blurb, not an article dump", title)
                  .isLessThan(5000);
              assertThat(summary)
                  .as("summary for '%s' should not contain leaked wiki section markup", title)
                  .doesNotContain("==Production==", "==Marketing==", "==Reception==", "wikitable");
          }
      }
  }

  @Test
  void allowsCorsFromTheWebClientOrigin() throws Exception {
    mvc.perform(get("/catalog/series").header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void listSeriesEpisodesReturns404ForUnknownSeries() throws Exception {
    UUID missing = UUID.randomUUID();
    mvc.perform(get("/catalog/series/" + missing + "/episodes"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SERIES_NOT_FOUND"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
