package com.weather.proxy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CachingIntegrationTest {

  private static MockWebServer mockWebServer;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CacheManager cacheManager;

  @BeforeAll
  static void startMockServer() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterAll
  static void stopMockServer() throws Exception {
    mockWebServer.shutdown();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("weather.api.base-url",
        () -> mockWebServer.url("/").toString());
  }

  @BeforeEach
  void clearCache() {
    cacheManager.getCache("weather").clear();
  }

  private static final String WEATHER_JSON = """
      {
        "latitude": 52.52,
        "longitude": 13.41,
        "current": {
          "temperature_2m": 5.0,
          "wind_speed_10m": 12.0
        }
      }
      """;

  @Test
  void sameCoordinatesHitCacheOnSecondCall() throws Exception {
    // given
    mockWebServer.enqueue(new MockResponse()
        .setBody(WEATHER_JSON)
        .addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(new MockResponse()
        .setBody(WEATHER_JSON)
        .addHeader("Content-Type", "application/json"));

    int initialRequestCount = mockWebServer.getRequestCount();

    // when
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.521")
            .param("lon", "13.411"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "52.522")
            .param("lon", "13.412"))
        .andExpect(status().isOk());

    // then
    assertThat(mockWebServer.getRequestCount() - initialRequestCount)
        .as("Only one upstream call should be made due to caching")
        .isEqualTo(1);
  }

  @Test
  void differentCoordinatesMakeSeparateUpstreamCalls() throws Exception {
    // given
    mockWebServer.enqueue(new MockResponse()
        .setBody(WEATHER_JSON)
        .addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(new MockResponse()
        .setBody(WEATHER_JSON)
        .addHeader("Content-Type", "application/json"));

    int initialRequestCount = mockWebServer.getRequestCount();

    // when
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "40.71")
            .param("lon", "-74.01"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "35.68")
            .param("lon", "139.69"))
        .andExpect(status().isOk());

    // then
    assertThat(mockWebServer.getRequestCount() - initialRequestCount)
        .as("Different coordinates should result in separate upstream calls")
        .isEqualTo(2);
  }
}
