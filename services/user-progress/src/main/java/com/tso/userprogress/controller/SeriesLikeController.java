package com.tso.userprogress.controller;

import com.tso.userprogress.api.LikesApi;
import com.tso.userprogress.service.SeriesLikeService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * "My Shows" — the set of series a user has liked. Implements the generated {@link LikesApi} so the
 * contract stays single-sourced in api/openapi.yaml.
 */
@RestController
public class SeriesLikeController implements LikesApi {

  private final SeriesLikeService likeService;

  public SeriesLikeController(SeriesLikeService likeService) {
    this.likeService = likeService;
  }

  @Override
  public ResponseEntity<List<UUID>> listLikes() {
    return ResponseEntity.ok(likeService.likedSeriesIds(currentUserId()));
  }

  @Override
  public ResponseEntity<Void> likeSeries(UUID seriesId) {
    likeService.like(currentUserId(), seriesId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> unlikeSeries(UUID seriesId) {
    likeService.unlike(currentUserId(), seriesId);
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
