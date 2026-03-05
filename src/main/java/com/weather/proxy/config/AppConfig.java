package com.weather.proxy.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Application configuration for RestClient, cache, and clock beans.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(WeatherApiProperties.class)
public class AppConfig {

  /** Creates a RestClient configured with timeouts from properties. */
  @Bean
  public RestClient restClient(WeatherApiProperties properties) {
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeoutMs()))
        .setSocketTimeout(Timeout.ofMilliseconds(properties.readTimeoutMs()))
        .build();

    HttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeoutMs()))
        .build();

    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .build();

    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(factory)
        .build();
  }

  /** Creates a Caffeine-backed cache manager with TTL from properties. */
  @Bean
  public CacheManager cacheManager(WeatherApiProperties properties) {
    CaffeineCacheManager manager = new CaffeineCacheManager("weather");
    manager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(properties.cacheTtlSeconds(), TimeUnit.SECONDS)
        .recordStats());
    return manager;
  }

  /** Provides a Clock bean for testable timestamps. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
