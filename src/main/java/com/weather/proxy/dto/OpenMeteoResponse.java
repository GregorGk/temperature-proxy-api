package com.weather.proxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deserialization target for the Open-Meteo current weather API response.
 */
public record OpenMeteoResponse(
    double latitude,
    double longitude,
    @JsonProperty("current") Current current
) {

  /** Current weather conditions from Open-Meteo. */
  public record Current(
      @JsonProperty("temperature_2m") double temperature2m,
      @JsonProperty("wind_speed_10m") double windSpeed10m
  ) {}
}
