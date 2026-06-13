package com.tso.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "episode")
public class CatalogEpisode {

  @Id private UUID id;

  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

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

  protected CatalogEpisode() {}

  public UUID getId() {
    return id;
  }

  public UUID getSeriesId() {
    return seriesId;
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
}
