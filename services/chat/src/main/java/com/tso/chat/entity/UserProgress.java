package com.tso.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "progress")
public class UserProgress {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

  @Column(name = "episode_index", nullable = false)
  private int episodeIndex;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected UserProgress() {}

  public UUID getUserId() {
    return userId;
  }

  public UUID getSeriesId() {
    return seriesId;
  }

  public int getEpisodeIndex() {
    return episodeIndex;
  }
}
