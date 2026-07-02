package com.furuiduo.quote.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Void>> handleStatusException(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.fail(ex.getReason()));
  }
}
