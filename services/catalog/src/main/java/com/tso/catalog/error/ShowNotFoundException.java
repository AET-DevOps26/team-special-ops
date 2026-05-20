package com.tso.catalog.error;

import java.util.UUID;

public class ShowNotFoundException extends RuntimeException {

  private final UUID showId;

  public ShowNotFoundException(UUID showId) {
    super("Show not found: " + showId);
    this.showId = showId;
  }

  public UUID getShowId() {
    return showId;
  }
}
