package com.tso.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "show")
public class Show {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String title;

  @Column(name = "seasons_count", nullable = false)
  private int seasonsCount;

  @Column(name = "episodes_count", nullable = false)
  private int episodesCount;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Show() {}

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public int getSeasonsCount() {
    return seasonsCount;
  }

  public int getEpisodesCount() {
    return episodesCount;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
