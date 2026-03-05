package com.weather.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weather.proxy.dto.OpenMeteoResponse;
import com.weather.proxy.exception.UpstreamServiceException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class OpenMeteoClientTest {

  private MockWebServer mockWebServer;
  private OpenMeteoClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(1000);
    factory.setReadTimeout(1000);

    RestClient restClient = RestClient.builder()
        .baseUrl(mockWebServer.url("/").toString())
        .requestFactory(factory)
        .build();
    client = new OpenMeteoClient(restClient);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void fetchCurrentWeatherDeserializesResponse() {
    // given
    String json = """
        {
          "latitude": 52.52,
          "longitude": 13.41,
          "current": {
            "temperature_2m": 1.2,
            "wind_speed_10m": 9.7
          }
        }
        """;
    mockWebServer.enqueue(new MockResponse()
        .setBody(json)
        .addHeader("Content-Type", "application/json"));

    // when
    OpenMeteoResponse response = client.fetchCurrentWeather(52.52, 13.41);

    // then
    assertThat(response.latitude()).isEqualTo(52.52);
    assertThat(response.longitude()).isEqualTo(13.41);
    assertThat(response.current().temperature2m()).isEqualTo(1.2);
    assertThat(response.current().windSpeed10m()).isEqualTo(9.7);
  }

  @Test
  void fetchCurrentWeatherThrowsOnUpstreamError() {
    // given
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    // when + then
    assertThatThrownBy(() -> client.fetchCurrentWeather(52.52, 13.41))
        .isInstanceOf(UpstreamServiceException.class);
  }

  @Test
  void fetchCurrentWeatherThrowsOnTimeout() {
    // given
    mockWebServer.enqueue(new MockResponse()
        .setBody("{}")
        .setBodyDelay(3, TimeUnit.SECONDS));

    // when + then
    assertThatThrownBy(() -> client.fetchCurrentWeather(52.52, 13.41))
        .isInstanceOf(UpstreamServiceException.class);
  }
}
