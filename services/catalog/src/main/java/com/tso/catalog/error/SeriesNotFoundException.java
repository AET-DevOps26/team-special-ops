package com.tso.catalog.error;

import java.util.UUID;

public class SeriesNotFoundException extends RuntimeException {

  private final UUID seriesId;

  public SeriesNotFoundException(UUID seriesId) {
    super("Series not found: " + seriesId);
    this.seriesId = seriesId;
  }

  public UUID getSeriesId() {
    return seriesId;
  }
}
