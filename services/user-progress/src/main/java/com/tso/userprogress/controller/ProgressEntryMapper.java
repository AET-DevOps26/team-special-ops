package com.tso.userprogress.controller;

import com.tso.userprogress.entity.Progress;
import com.tso.userprogress.model.ProgressEntry;

final class ProgressEntryMapper {

  private ProgressEntryMapper() {}

  static ProgressEntry toDto(Progress progress) {
    return new ProgressEntry()
        .seriesId(progress.getSeriesId())
        .episodeIndex(progress.getEpisodeIndex())
        .updatedAt(progress.getUpdatedAt());
  }
}
