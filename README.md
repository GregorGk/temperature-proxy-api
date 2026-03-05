# Temperature Proxy API

REST API that proxies current weather data from [Open-Meteo](https://open-meteo.com/), returning a normalized JSON response.

Built with Java 21, Spring Boot 3.4, and Gradle.

## Quick Start

```bash
# Build and test
./gradlew build

# Run locally (starts on port 8080)
./gradlew bootRun

# Try it
curl "http://localhost:8080/api/v1/weather/current?lat=52.52&lon=13.41"
```

**Example response:**

```json
{
  "location": { "lat": 52.52, "lon": 13.41 },
  "current": { "temperatureC": 1.2, "windSpeedKmh": 9.7 },
  "source": "open-meteo",
  "retrievedAt": "2026-01-11T10:12:54Z"
}
```

### Docker

```bash
docker build -t temperature-proxy-api .
docker run -p 8080:8080 temperature-proxy-api
```

## API

```
GET /api/v1/weather/current?lat={lat}&lon={lon}
```

| Parameter | Required | Range        |
|-----------|----------|--------------|
| `lat`     | yes      | -90 to 90    |
| `lon`     | yes      | -180 to 180  |

Swagger UI available at [/swagger-ui.html](http://localhost:8080/swagger-ui.html) when running.

## Stability

- **Cache** — Caffeine in-process cache keyed by `(lat, lon)` rounded to 2 decimals (~1 km), 60s TTL
- **Circuit breaker** — Resilience4j, count-based (10-call window, 50% failure threshold, 30s open)
- **Timeouts** — 1s connect + 1s read on the upstream HTTP client

No retries by design — the circuit breaker and cache provide resilience without compounding latency.

## Observability

| Endpoint                      | Description                            |
|-------------------------------|----------------------------------------|
| `GET /actuator/health`        | Health with circuit breaker status     |
| `GET /actuator/prometheus`    | Metrics in Prometheus format           |
| `GET /actuator/caches`        | Cache listing and eviction             |

Cache hit/miss stats are available via Prometheus metrics (`cache_gets_total`, `cache_size`, `cache_evictions_total`).

## Configuration

All in `application.yml`:

| Property                          | Default                        |
|-----------------------------------|--------------------------------|
| `weather.api.base-url`            | `https://api.open-meteo.com`   |
| `weather.api.connect-timeout-ms`  | `1000`                         |
| `weather.api.read-timeout-ms`     | `1000`                         |
| `weather.api.cache-ttl-seconds`   | `60`                           |

The `docker` Spring profile activates JSON structured logging.


## Tests

```bash
./gradlew test
```
