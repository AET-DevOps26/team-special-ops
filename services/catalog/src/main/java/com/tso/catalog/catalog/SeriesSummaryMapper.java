package com.tso.catalog.catalog;

import com.tso.catalog.domain.Series;
import com.tso.catalog.model.SeriesSummary;

final class SeriesSummaryMapper {

  private SeriesSummaryMapper() {}

  static SeriesSummary toDto(Series series) {
    return new SeriesSummary()
        .id(series.getId())
        .title(series.getTitle())
        .seasonsCount(series.getSeasonsCount())
        .episodesCount(series.getEpisodesCount());
  }
}
