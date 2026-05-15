package com.tso.catalog.catalog;

import com.tso.catalog.domain.Show;
import com.tso.catalog.model.ShowSummary;

final class ShowSummaryMapper {

  private ShowSummaryMapper() {}

  static ShowSummary toDto(Show show) {
    return new ShowSummary()
        .id(show.getId())
        .title(show.getTitle())
        .seasonsCount(show.getSeasonsCount())
        .episodesCount(show.getEpisodesCount());
  }
}
