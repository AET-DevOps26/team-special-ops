package com.tso.chat.exception;

public class SeriesNotFoundException extends RuntimeException {
  public SeriesNotFoundException() {
    super("Series not found");
  }
}
