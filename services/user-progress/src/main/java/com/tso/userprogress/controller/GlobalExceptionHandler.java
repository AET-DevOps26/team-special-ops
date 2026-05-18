package com.tso.userprogress.controller;

import com.tso.userprogress.exception.InvalidCredentialsException;
import com.tso.userprogress.exception.UserAlreadyExistsException;
import com.tso.userprogress.model.Error;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Error> handleUserAlreadyExists(UserAlreadyExistsException e) {
    Error error = new Error("USER_ALREADY_EXISTS", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Error> handleInvalidCredentials(InvalidCredentialsException e) {
    Error error = new Error("INVALID_CREDENTIALS", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Error> handleValidationException(MethodArgumentNotValidException e) {
    Map<String, Object> details = new HashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));

    Error error = new Error("VALIDATION_FAILED", "Validation failed");
    error.setDetails(details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Error> handleGenericException(Exception e) {
    Error error = new Error("INTERNAL_ERROR", "An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
