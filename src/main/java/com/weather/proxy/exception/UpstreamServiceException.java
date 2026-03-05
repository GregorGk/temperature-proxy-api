package com.weather.proxy.exception;

/**
 * Thrown when the upstream weather service returns an error or is unreachable.
 */
public class UpstreamServiceException extends RuntimeException {

  /** Creates an exception with the given message. */
  public UpstreamServiceException(String message) {
    super(message);
  }

  /** Creates an exception with the given message and cause. */
  public UpstreamServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
