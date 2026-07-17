package com.tso.userprogress.repository;

import com.tso.userprogress.entity.SeriesLike;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesLikeRepository extends JpaRepository<SeriesLike, UUID> {

  /** Most-recently-liked first, so "My Shows" lists newest at the top. */
  List<SeriesLike> findByUserIdOrderByCreatedAtDesc(UUID userId);

  /**
   * Atomic idempotent insert: relies on the UNIQUE(user_id, series_id) constraint so concurrent
   * likes of the same series can't race into a constraint violation. {@code id} and {@code
   * created_at} come from their DB defaults (see V4 migration).
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO likes (user_id, series_id) VALUES (:userId, :seriesId)"
              + " ON CONFLICT (user_id, series_id) DO NOTHING",
      nativeQuery = true)
  void insertIgnore(@Param("userId") UUID userId, @Param("seriesId") UUID seriesId);

  void deleteByUserIdAndSeriesId(UUID userId, UUID seriesId);
}
