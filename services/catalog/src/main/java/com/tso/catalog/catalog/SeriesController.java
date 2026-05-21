package com.tso.catalog.catalog;

import com.tso.catalog.api.CatalogApi;
import com.tso.catalog.error.SeriesNotFoundException;
import com.tso.catalog.model.Episode;
import com.tso.catalog.model.Error;
import com.tso.catalog.model.HealthStatus;
import com.tso.catalog.model.SeriesSummary;
import com.tso.catalog.repo.EpisodeRepository;
import com.tso.catalog.repo.SeriesRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeriesController implements CatalogApi {

  private final SeriesRepository series;
  private final EpisodeRepository episodes;

  SeriesController(SeriesRepository series, EpisodeRepository episodes) {
    this.series = series;
    this.episodes = episodes;
  }

  @Override
  public ResponseEntity<HealthStatus> catalogHealth() {
    return ResponseEntity.ok(
        new HealthStatus().status(HealthStatus.StatusEnum.OK).service("catalog"));
  }

  @Override
  public ResponseEntity<List<SeriesSummary>> listSeries() {
    List<SeriesSummary> body =
        series.findAllByOrderByTitleAsc().stream().map(SeriesSummaryMapper::toDto).toList();
    return ResponseEntity.ok(body);
  }

  @Override
  public ResponseEntity<List<Episode>> listSeriesEpisodes(UUID id) {
    if (!series.existsById(id)) {
      throw new SeriesNotFoundException(id);
    }
    List<Episode> body =
        episodes.findBySeries_IdOrderByEpisodeIndexAsc(id).stream()
            .map(EpisodeMapper::toDto)
            .toList();
    return ResponseEntity.ok(body);
  }

  @ExceptionHandler(SeriesNotFoundException.class)
  ResponseEntity<Error> handleSeriesNotFound(SeriesNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new Error().code("SERIES_NOT_FOUND").message(ex.getMessage()));
  }
}
