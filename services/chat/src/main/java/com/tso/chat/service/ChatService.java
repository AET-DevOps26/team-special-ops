package com.tso.chat.service;

import com.tso.chat.client.GenAiClient;
import com.tso.chat.entity.CatalogEpisode;
import com.tso.chat.exception.NoProgressException;
import com.tso.chat.exception.SeriesNotFoundException;
import com.tso.chat.model.ChatAnswerResponse;
import com.tso.chat.model.ChatQuestionRequest;
import com.tso.chat.model.GenAiAskResponse;
import com.tso.chat.repository.CatalogEpisodeRepository;
import com.tso.chat.repository.CatalogSeriesRepository;
import com.tso.chat.repository.UserProgressRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final CatalogSeriesRepository seriesRepository;
  private final CatalogEpisodeRepository episodeRepository;
  private final UserProgressRepository progressRepository;
  private final ChatHistoryService chatHistoryService;
  private final GenAiClient genAiClient;

  public ChatService(
      CatalogSeriesRepository seriesRepository,
      CatalogEpisodeRepository episodeRepository,
      UserProgressRepository progressRepository,
      ChatHistoryService chatHistoryService,
      GenAiClient genAiClient) {
    this.seriesRepository = seriesRepository;
    this.episodeRepository = episodeRepository;
    this.progressRepository = progressRepository;
    this.chatHistoryService = chatHistoryService;
    this.genAiClient = genAiClient;
  }

  public ChatAnswerResponse askQuestion(UUID userId, ChatQuestionRequest request) {
    UUID seriesId = request.getSeriesId();
    if (!seriesRepository.existsById(seriesId)) {
      throw new SeriesNotFoundException();
    }

    int progress =
        progressRepository
            .findByUserIdAndSeriesId(userId, seriesId)
            .map(p -> p.getEpisodeIndex())
            .orElse(0);

    if (progress <= 0) {
      throw new NoProgressException();
    }

    List<CatalogEpisode> allowedEpisodes =
        episodeRepository.findBySeriesIdAndEpisodeIndexLessThanEqualOrderByEpisodeIndexAsc(
            seriesId, progress);

    GenAiAskResponse genAiResponse = genAiClient.postAsk(request.getQuestion(), allowedEpisodes);

    List<Integer> cited =
        genAiResponse.getCitedEpisodeIndices().stream().filter(idx -> idx <= progress).toList();

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    return chatHistoryService.saveQuestionAndAnswer(
        userId, seriesId, request.getQuestion(), progress, genAiResponse.getAnswer(), cited, now);
  }
}
