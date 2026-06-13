package com.tso.chat.exception;

public class NoProgressException extends RuntimeException {
  public NoProgressException() {
    super("Set your current episode before asking a question");
  }
}
