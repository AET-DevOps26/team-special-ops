package com.tso.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "episode")
public class Episode {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "show_id", nullable = false)
  private Show show;

  @Column(nullable = false)
  private int season;

  @Column(name = "episode_number", nullable = false)
  private int episodeNumber;

  @Column(name = "episode_index", nullable = false)
  private int episodeIndex;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String summary;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Episode() {}

  public UUID getId() {
    return id;
  }

  public Show getShow() {
    return show;
  }

  public int getSeason() {
    return season;
  }

  public int getEpisodeNumber() {
    return episodeNumber;
  }

  public int getEpisodeIndex() {
    return episodeIndex;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
