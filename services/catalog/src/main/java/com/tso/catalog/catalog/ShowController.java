package com.tso.catalog.catalog;

import com.tso.catalog.api.CatalogApi;
import com.tso.catalog.error.ShowNotFoundException;
import com.tso.catalog.model.Episode;
import com.tso.catalog.model.Error;
import com.tso.catalog.model.HealthStatus;
import com.tso.catalog.model.ShowSummary;
import com.tso.catalog.repo.EpisodeRepository;
import com.tso.catalog.repo.ShowRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShowController implements CatalogApi {

  private final ShowRepository shows;
  private final EpisodeRepository episodes;

  ShowController(ShowRepository shows, EpisodeRepository episodes) {
    this.shows = shows;
    this.episodes = episodes;
  }

  @Override
  public ResponseEntity<HealthStatus> catalogHealth() {
    return ResponseEntity.ok(
        new HealthStatus().status(HealthStatus.StatusEnum.OK).service("catalog"));
  }

  @Override
  public ResponseEntity<List<ShowSummary>> listShows() {
    List<ShowSummary> body =
        shows.findAllByOrderByTitleAsc().stream().map(ShowSummaryMapper::toDto).toList();
    return ResponseEntity.ok(body);
  }

  @Override
  public ResponseEntity<List<Episode>> listShowEpisodes(UUID id) {
    if (!shows.existsById(id)) {
      throw new ShowNotFoundException(id);
    }
    List<Episode> body =
        episodes.findByShow_IdOrderByEpisodeIndexAsc(id).stream()
            .map(EpisodeMapper::toDto)
            .toList();
    return ResponseEntity.ok(body);
  }

  @ExceptionHandler(ShowNotFoundException.class)
  ResponseEntity<Error> handleShowNotFound(ShowNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new Error().code("SHOW_NOT_FOUND").message(ex.getMessage()));
  }
}
