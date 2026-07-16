package com.tso.userprogress.service;

import com.tso.userprogress.entity.SeriesLike;
import com.tso.userprogress.repository.SeriesLikeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeriesLikeService {

  private final SeriesLikeRepository repository;

  public SeriesLikeService(SeriesLikeRepository repository) {
    this.repository = repository;
  }

  /** The caller's liked series ids, most recently liked first. */
  @Transactional(readOnly = true)
  public List<UUID> likedSeriesIds(UUID userId) {
    return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(SeriesLike::getSeriesId)
        .toList();
  }

  /** Idempotent: liking an already-liked series is a no-op, safe under concurrency. */
  @Transactional
  public void like(UUID userId, UUID seriesId) {
    repository.insertIgnore(userId, seriesId);
  }

  /** Idempotent: unliking a series that isn't liked is a no-op. */
  @Transactional
  public void unlike(UUID userId, UUID seriesId) {
    repository.deleteByUserIdAndSeriesId(userId, seriesId);
  }
}
