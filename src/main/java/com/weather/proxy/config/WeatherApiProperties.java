package com.weather.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the upstream weather API.
 */
@ConfigurationProperties(prefix = "weather.api")
public record WeatherApiProperties(
    String baseUrl,
    int connectTimeoutMs,
    int readTimeoutMs,
    long cacheTtlSeconds
) {}
