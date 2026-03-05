package com.weather.proxy.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeatherIntegrationTest {

  private static MockWebServer mockWebServer;

  @Autowired
  private MockMvc mockMvc;

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

  private static final String VALID_RESPONSE = """
      {
        "latitude": 52.52,
        "longitude": 13.41,
        "current": {
          "temperature_2m": 1.2,
          "wind_speed_10m": 9.7
        }
      }
      """;

  @Test
  void fullRequestFlowReturnsNormalizedResponse() throws Exception {
    // given
    mockWebServer.enqueue(new MockResponse()
        .setBody(VALID_RESPONSE)
        .addHeader("Content-Type", "application/json"));

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "48.85")
            .param("lon", "2.35"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("open-meteo"))
        .andExpect(jsonPath("$.current.temperatureC").value(1.2))
        .andExpect(jsonPath("$.retrievedAt").exists());
  }

  @Test
  void upstreamErrorReturns502() throws Exception {
    // given
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "10.0")
            .param("lon", "20.0"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502));
  }

  @Test
  void upstreamTimeoutReturns502() throws Exception {
    // given
    mockWebServer.enqueue(new MockResponse()
        .setBody(VALID_RESPONSE)
        .setBodyDelay(5, TimeUnit.SECONDS));

    // when + then
    mockMvc.perform(get("/api/v1/weather/current")
            .param("lat", "30.0")
            .param("lon", "40.0"))
        .andExpect(status().isBadGateway());
  }
}
