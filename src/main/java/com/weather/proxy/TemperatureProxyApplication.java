package com.weather.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Temperature Proxy API.
 */
@SpringBootApplication
public class TemperatureProxyApplication {

  /** Application entry point. */
  public static void main(String[] args) {
    SpringApplication.run(TemperatureProxyApplication.class, args);
  }
}
