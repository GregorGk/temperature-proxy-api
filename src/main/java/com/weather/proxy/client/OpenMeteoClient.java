package com.weather.proxy.client;

import com.weather.proxy.dto.OpenMeteoResponse;
import com.weather.proxy.exception.UpstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for the Open-Meteo current weather API.
 */
@Component
public class OpenMeteoClient {

  private static final Logger LOG = LoggerFactory.getLogger(OpenMeteoClient.class);

  private final RestClient restClient;

  /** Constructs the client with the configured RestClient. */
  public OpenMeteoClient(RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * Fetches current weather data for the given coordinates.
   *
   * @param latitude  the latitude
   * @param longitude the longitude
   * @return the upstream response
   * @throws UpstreamServiceException if the upstream call fails
   */
  @CircuitBreaker(name = "openMeteo", fallbackMethod = "fallback")
  public OpenMeteoResponse fetchCurrentWeather(double latitude, double longitude) {
    LOG.info("Fetching weather from Open-Meteo for lat={}, lon={}", latitude, longitude);
    try {
      OpenMeteoResponse response = restClient.get()
          .uri("/v1/forecast?latitude={lat}&longitude={lon}"
              + "&current=temperature_2m,wind_speed_10m", latitude, longitude)
          .retrieve()
          .body(OpenMeteoResponse.class);

      if (response == null) {
        throw new UpstreamServiceException("Null response from Open-Meteo");
      }
      return response;
    } catch (RestClientException ex) {
      throw new UpstreamServiceException("Failed to fetch weather from Open-Meteo", ex);
    }
  }

  @SuppressWarnings("unused")
  private OpenMeteoResponse fallback(double latitude, double longitude, Throwable throwable) {
    LOG.warn("Circuit breaker fallback for lat={}, lon={}: {}",
        latitude, longitude, throwable.getMessage());
    throw new UpstreamServiceException(
        "Weather service temporarily unavailable", throwable);
  }
}
