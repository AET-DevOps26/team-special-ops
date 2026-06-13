package com.tso.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "series")
public class CatalogSeries {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  protected CatalogSeries() {}

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }
}
