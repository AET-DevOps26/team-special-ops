package com.tso.chat.client;

import com.tso.chat.entity.CatalogEpisode;
import com.tso.chat.exception.GenAiUnavailableException;
import com.tso.chat.model.AllowedEpisodeSummary;
import com.tso.chat.model.GenAiAskRequest;
import com.tso.chat.model.GenAiAskResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GenAiClient {

  private final RestClient restClient;

  public GenAiClient(RestClient genAiRestClient) {
    this.restClient = genAiRestClient;
  }

  public GenAiAskResponse postAsk(String question, List<CatalogEpisode> episodes) {
    List<AllowedEpisodeSummary> summaries =
        episodes.stream()
            .map(
                ep ->
                    new AllowedEpisodeSummary()
                        .episodeIndex(ep.getEpisodeIndex())
                        .season(ep.getSeason())
                        .episodeNumber(ep.getEpisodeNumber())
                        .title(ep.getTitle())
                        .summary(ep.getSummary()))
            .toList();

    GenAiAskRequest request = new GenAiAskRequest().question(question).allowedSummaries(summaries);

    try {
      return restClient
          .post()
          .uri("/genai/ask")
          .body(request)
          .retrieve()
          .body(GenAiAskResponse.class);
    } catch (RestClientException e) {
      throw new GenAiUnavailableException("GenAI service is unavailable");
    }
  }
}
