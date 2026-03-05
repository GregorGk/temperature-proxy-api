package com.weather.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.weather.proxy.client.OpenMeteoClient;
import com.weather.proxy.dto.OpenMeteoResponse;
import com.weather.proxy.dto.WeatherResponse;
import com.weather.proxy.exception.UpstreamServiceException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

  private static final Instant FIXED_INSTANT =
      Instant.parse("2026-01-11T10:12:54Z");

  @Mock
  private OpenMeteoClient openMeteoClient;

  private WeatherService weatherService;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    weatherService = new WeatherService(openMeteoClient, fixedClock);
  }

  @Test
  void getCurrentWeatherTransformsUpstreamResponse() {
    // given
    OpenMeteoResponse upstream = new OpenMeteoResponse(
        52.52, 13.41,
        new OpenMeteoResponse.Current(1.2, 9.7));
    when(openMeteoClient.fetchCurrentWeather(52.52, 13.41)).thenReturn(upstream);

    // when
    WeatherResponse result = weatherService.getCurrentWeather(52.52, 13.41);

    // then
    assertThat(result.location().lat()).isEqualTo(52.52);
    assertThat(result.location().lon()).isEqualTo(13.41);
    assertThat(result.current().temperatureC()).isEqualTo(1.2);
    assertThat(result.current().windSpeedKmh()).isEqualTo(9.7);
    assertThat(result.source()).isEqualTo("open-meteo");
    assertThat(result.retrievedAt()).isEqualTo(FIXED_INSTANT);
  }

  @Test
  void getCurrentWeatherRoundsCoordinates() {
    // given
    OpenMeteoResponse upstream = new OpenMeteoResponse(
        52.5200, 13.4050,
        new OpenMeteoResponse.Current(5.0, 10.0));
    when(openMeteoClient.fetchCurrentWeather(52.52345, 13.40567))
        .thenReturn(upstream);

    // when
    WeatherResponse result = weatherService.getCurrentWeather(52.52345, 13.40567);

    // then
    assertThat(result.location().lat()).isEqualTo(52.52);
    assertThat(result.location().lon()).isEqualTo(13.41);
  }

  @Test
  void getCurrentWeatherPropagatesUpstreamException() {
    // given
    when(openMeteoClient.fetchCurrentWeather(52.52, 13.41))
        .thenThrow(new UpstreamServiceException("Service unavailable"));

    // when + then
    assertThatThrownBy(() -> weatherService.getCurrentWeather(52.52, 13.41))
        .isInstanceOf(UpstreamServiceException.class)
        .hasMessage("Service unavailable");
  }
}
