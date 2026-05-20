package com.tso.catalog.catalog;

import com.tso.catalog.domain.Episode;

final class EpisodeMapper {

  private EpisodeMapper() {}

  static com.tso.catalog.model.Episode toDto(Episode episode) {
    return new com.tso.catalog.model.Episode()
        .id(episode.getId())
        .season(episode.getSeason())
        .episodeNumber(episode.getEpisodeNumber())
        .episodeIndex(episode.getEpisodeIndex())
        .title(episode.getTitle())
        .summary(episode.getSummary());
  }
}
