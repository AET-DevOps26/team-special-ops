package com.tso.userprogress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A user "liking" a series — surfaced as "My Shows" on the client. */
@Entity
@Table(name = "likes")
public class SeriesLike {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  protected SeriesLike() {}

  public SeriesLike(UUID userId, UUID seriesId) {
    this.userId = userId;
    this.seriesId = seriesId;
  }

  public UUID getSeriesId() {
    return seriesId;
  }
}
