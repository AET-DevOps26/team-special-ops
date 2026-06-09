package com.tso.chat.controller;

import com.tso.chat.exception.GenAiUnavailableException;
import com.tso.chat.exception.NoProgressException;
import com.tso.chat.exception.SeriesNotFoundException;
import com.tso.chat.model.Error;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NoProgressException.class)
  public ResponseEntity<Error> handleNoProgress(NoProgressException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new Error("NO_PROGRESS", e.getMessage()));
  }

  @ExceptionHandler(SeriesNotFoundException.class)
  public ResponseEntity<Error> handleSeriesNotFound(SeriesNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new Error("SERIES_NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler(GenAiUnavailableException.class)
  public ResponseEntity<Error> handleGenAiUnavailable(GenAiUnavailableException e) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new Error("GENAI_UNAVAILABLE", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException e) {
    Map<String, Object> details = new HashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
    Error body = new Error("VALIDATION_FAILED", "Validation failed");
    body.setDetails(details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Error> handleGeneric(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Error("INTERNAL_ERROR", "An unexpected error occurred"));
  }
}
