package com.weather.proxy.service;

import com.weather.proxy.client.OpenMeteoClient;
import com.weather.proxy.dto.OpenMeteoResponse;
import com.weather.proxy.dto.WeatherResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service that fetches and transforms weather data with caching.
 */
@Service
public class WeatherService {

  private static final Logger LOG = LoggerFactory.getLogger(WeatherService.class);

  private final OpenMeteoClient openMeteoClient;
  private final Clock clock;

  /** Constructs the service with the required dependencies. */
  public WeatherService(OpenMeteoClient openMeteoClient, Clock clock) {
    this.openMeteoClient = openMeteoClient;
    this.clock = clock;
  }

  /**
   * Gets current weather for the given coordinates, with caching.
   *
   * @param lat latitude
   * @param lon longitude
   * @return normalized weather response
   */
  @Cacheable(value = "weather", key = "T(String).format('%.2f,%.2f', #lat, #lon)")
  public WeatherResponse getCurrentWeather(double lat, double lon) {
    LOG.info("Cache miss for lat={}, lon={}", lat, lon);
    OpenMeteoResponse upstream = openMeteoClient.fetchCurrentWeather(lat, lon);
    return toWeatherResponse(upstream, lat, lon);
  }

  private WeatherResponse toWeatherResponse(
      OpenMeteoResponse upstream, double lat, double lon) {
    return new WeatherResponse(
        new WeatherResponse.Location(
            roundToTwoDecimals(lat),
            roundToTwoDecimals(lon)),
        new WeatherResponse.CurrentWeather(
            upstream.current().temperature2m(),
            upstream.current().windSpeed10m()),
        "open-meteo",
        Instant.now(clock).truncatedTo(ChronoUnit.SECONDS));
  }

  private static double roundToTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
