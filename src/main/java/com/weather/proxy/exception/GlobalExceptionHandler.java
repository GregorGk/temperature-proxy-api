package com.weather.proxy.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG =
      LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Standardized error response. */
  @Schema(description = "Standardized error response")
  public record ErrorResponse(
      @Schema(description = "HTTP status code", example = "400") int status,
      @Schema(description = "Error reason phrase", example = "Bad Request")
      String error,
      @Schema(description = "Detailed error message",
          example = "Latitude must be >= -90") String message,
      @Schema(description = "Timestamp of the error",
          example = "2026-01-11T10:12:54Z") Instant timestamp
  ) {}

  /** Handles constraint violations from input validation. */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("; "));
    return new ErrorResponse(400, "Bad Request", message, now());
  }

  /** Handles missing required request parameters. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex) {
    return new ErrorResponse(400, "Bad Request", ex.getMessage(), now());
  }

  /** Handles upstream service failures. */
  @ExceptionHandler(UpstreamServiceException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public ErrorResponse handleUpstreamFailure(UpstreamServiceException ex) {
    LOG.error("Upstream service error: {}", ex.getMessage(), ex);
    return new ErrorResponse(
        502, "Bad Gateway", "Weather service unavailable", now());
  }

  /** Handles circuit breaker open state. */
  @ExceptionHandler(CallNotPermittedException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ErrorResponse handleCircuitOpen(CallNotPermittedException ex) {
    LOG.warn("Circuit breaker open: {}", ex.getMessage());
    return new ErrorResponse(
        503, "Service Unavailable",
        "Weather service temporarily unavailable", now());
  }

  /** Handles requests for missing static resources (e.g. favicon.ico). */
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNoResource(NoResourceFoundException ex) {
    return new ErrorResponse(404, "Not Found", ex.getMessage(), now());
  }

  /** Catch-all for unexpected errors. */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleGeneral(Exception ex) {
    LOG.error("Unexpected error", ex);
    return new ErrorResponse(
        500, "Internal Server Error", "An unexpected error occurred",
        now());
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.SECONDS);
  }
}
