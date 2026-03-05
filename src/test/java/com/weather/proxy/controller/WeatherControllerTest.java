package com.weather.proxy.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weather.proxy.dto.WeatherResponse;
import com.weather.proxy.exception.GlobalExceptionHandler;
import com.weather.proxy.exception.UpstreamServiceException;
import com.weather.proxy.service.WeatherService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
@Import(GlobalExceptionHandler.class)
class WeatherControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WeatherService weatherService;

  @Test
  void getCurrentWeatherReturnsOkForValidRequest() throws Exception {
    // given
    WeatherResponse response = new WeatherResponse(
        new WeatherResponse.Location(52.52, 13.41),
        new WeatherResponse.CurrentWeather(1.2, 9.7),
        "open-meteo",
        Instant.parse("2026-01-11T10:12:54Z"));
    when(weatherService.getCurrentWeather(52.52, 13.41)).thenReturn(response);

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.52")
            .param("lon", "13.41"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location.lat").value(52.52))
        .andExpect(jsonPath("$.location.lon").value(13.41))
        .andExpect(jsonPath("$.current.temperatureC").value(1.2))
        .andExpect(jsonPath("$.current.windSpeedKmh").value(9.7))
        .andExpect(jsonPath("$.source").value("open-meteo"))
        .andExpect(jsonPath("$.retrievedAt").value("2026-01-11T10:12:54Z"));
  }

  @Test
  void getCurrentWeatherReturns400ForInvalidLatitude() throws Exception {
    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "91")
            .param("lon", "13.41"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void getCurrentWeatherReturns400ForInvalidLongitude() throws Exception {
    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.52")
            .param("lon", "181"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void getCurrentWeatherReturns400ForMissingParam() throws Exception {
    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.52"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void getCurrentWeatherReturns502OnUpstreamFailure() throws Exception {
    // given
    when(weatherService.getCurrentWeather(52.52, 13.41))
        .thenThrow(new UpstreamServiceException("Service down"));

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.52")
            .param("lon", "13.41"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.error").value("Bad Gateway"));
  }

  @Test
  void getCurrentWeatherReturns503WhenCircuitOpen() throws Exception {
    // given
    CircuitBreaker cb = CircuitBreaker.ofDefaults("test");
    when(weatherService.getCurrentWeather(52.52, 13.41))
        .thenThrow(CallNotPermittedException.createCallNotPermittedException(cb));

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.52")
            .param("lon", "13.41"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }
}
