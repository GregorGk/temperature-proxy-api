package com.weather.proxy.controller;

import com.weather.proxy.dto.WeatherResponse;
import com.weather.proxy.exception.GlobalExceptionHandler.ErrorResponse;
import com.weather.proxy.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for current weather data.
 */
@RestController
@RequestMapping("/api/v1/weather")
@Validated
@Tag(name = "Weather", description = "Current weather data")
public class WeatherController {

  private final WeatherService weatherService;

  /** Constructs the controller with the weather service. */
  public WeatherController(WeatherService weatherService) {
    this.weatherService = weatherService;
  }

  /**
   * Returns current weather for the given coordinates.
   *
   * @param lat latitude (-90 to 90)
   * @param lon longitude (-180 to 180)
   * @return normalized weather response
   */
  @GetMapping("/current")
  @Operation(
      summary = "Get current weather",
      description = "Returns current temperature and wind speed for the given"
          + " WGS84 coordinates. Example: 52.52, 13.41 for Berlin, Germany."
  )
  @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully",
      content = @Content(schema = @Schema(implementation = WeatherResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid coordinates",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = """
              {"status":400,"error":"Bad Request",\
              "message":"Latitude must be >= -90",\
              "timestamp":"2026-01-11T10:12:54Z"}""")))
  @ApiResponse(responseCode = "502", description = "Upstream service failure",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = """
              {"status":502,"error":"Bad Gateway",\
              "message":"Weather service unavailable",\
              "timestamp":"2026-01-11T10:12:54Z"}""")))
  @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = """
              {"status":500,"error":"Internal Server Error",\
              "message":"An unexpected error occurred",\
              "timestamp":"2026-01-11T10:12:54Z"}""")))
  @ApiResponse(responseCode = "503", description = "Circuit breaker open",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class),
          examples = @ExampleObject(value = """
              {"status":503,"error":"Service Unavailable",\
              "message":"Weather service temporarily unavailable",\
              "timestamp":"2026-01-11T10:12:54Z"}""")))
  public WeatherResponse getCurrentWeather(
      @RequestParam
      @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
      @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
      @Parameter(description = "WGS84 latitude (-90 to 90), e.g. 52.52 for Berlin",
          example = "52.52")
      double lat,
      @RequestParam
      @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
      @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
      @Parameter(description = "WGS84 longitude (-180 to 180), e.g. 13.41 for Berlin",
          example = "13.41")
      double lon) {
    return weatherService.getCurrentWeather(lat, lon);
  }
}
