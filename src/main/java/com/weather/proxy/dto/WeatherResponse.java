package com.weather.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Normalized weather response returned by this API.
 */
@Schema(description = "Normalized weather response")
public record WeatherResponse(
    Location location,
    CurrentWeather current,
    @Schema(description = "Data source identifier", example = "open-meteo")
    String source,
    @Schema(description = "Timestamp when data was retrieved",
        example = "2026-01-11T10:12:54Z")
    Instant retrievedAt
) {

  /** Geographic coordinates. */
  @Schema(description = "Geographic coordinates")
  public record Location(
      @Schema(description = "Latitude", example = "52.52") double lat,
      @Schema(description = "Longitude", example = "13.41") double lon
  ) {}

  /** Current weather conditions in normalized units. */
  @Schema(description = "Current weather conditions")
  public record CurrentWeather(
      @Schema(description = "Temperature in Celsius", example = "1.2")
      double temperatureC,
      @Schema(description = "Wind speed in km/h", example = "9.7")
      double windSpeedKmh
  ) {}
}
