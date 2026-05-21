package com.tso.userprogress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "progress")
public class Progress {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

  @Column(name = "episode_index", nullable = false)
  private int episodeIndex;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Progress() {}

  public Progress(UUID userId, UUID seriesId, int episodeIndex) {
    this.userId = userId;
    this.seriesId = seriesId;
    this.episodeIndex = episodeIndex;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getSeriesId() {
    return seriesId;
  }

  public int getEpisodeIndex() {
    return episodeIndex;
  }

  public void setEpisodeIndex(int episodeIndex) {
    this.episodeIndex = episodeIndex;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
