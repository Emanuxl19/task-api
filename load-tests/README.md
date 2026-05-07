# Load Testing

This directory covers the kind of testing that JUnit and MockMvc do not cover:

- throughput
- burst traffic
- concurrency races
- rate-limit behavior under repeated requests

These tests use `k6`, not Maven.

## What Exists Today

The repository already has valid functional coverage:

- unit tests
- controller tests
- integration tests

That proves correctness. It does not prove behavior under heavy traffic.

## Scenarios Included

### `scenarios/rate-limit.js`

Validates the current production-style limits:

- login: `5 req/min per IP`
- general API: `60 req/min per IP`

Use this against the default application profile or the current docker-compose setup.

### `scenarios/refresh-race.js`

Fires multiple concurrent refresh requests using the same refresh token to expose token-rotation race conditions.

Expected result:

- one request succeeds with `200`
- the rest are rejected with `401`

### `scenarios/tasks-throughput.js`

Exercises authenticated task creation, listing, and fetch under load.

Important:

- this scenario is meant for a dedicated load-test profile
- it should not be run against the default rate limits
- if you run it against the normal profile, `429` responses are expected and the test should fail

## Prerequisites

1. Start PostgreSQL and the API.
2. Install `k6` locally.
3. Point `BASE_URL` at the running API.

## Running The API

### Production-like limits

Use this for `rate-limit.js` and `refresh-race.js`:

```bash
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Or run the existing compose stack:

```bash
docker compose up -d
```

### Relaxed limits for throughput tests

Use this for `tasks-throughput.js`:

```bash
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=postgres,loadtest
```

Or with Docker Compose override:

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml up -d --build
```

The `loadtest` profile raises the rate limits so the throughput script measures the app and database, not just the rate limiter.

## Running k6

### Validate rate limiting

```bash
k6 run load-tests/scenarios/rate-limit.js
```

### Validate refresh token concurrency

```bash
k6 run load-tests/scenarios/refresh-race.js
```

### Run authenticated throughput against the load-test profile

```bash
k6 run -e BASE_URL=http://localhost:8080 -e VUS=20 -e ITERATIONS=25 load-tests/scenarios/tasks-throughput.js
```

## Useful Environment Variables

- `BASE_URL`: API base URL. Default: `http://localhost:8080`
- `TEST_PASSWORD`: password used by generated users
- `LOGIN_VUS`: VUs for the login limit scenario
- `LOGIN_REQUESTS`: total login requests for the login limit scenario
- `GENERAL_VUS`: VUs for the general limit scenario
- `GENERAL_REQUESTS`: total general requests for the general limit scenario
- `ATTEMPTS`: concurrent refresh attempts in `refresh-race.js`
- `VUS`: VUs for `tasks-throughput.js`
- `ITERATIONS`: iterations per VU for `tasks-throughput.js`
- `SLEEP_SECONDS`: pause between task iterations
- `RUN_ID`: custom suffix to avoid email collisions between runs

## How To Read Results

For `rate-limit.js`:

- high `login_429_rate` is good
- high `general_429_rate` is good
- `unexpected_status_rate` must stay at `0`

For `refresh-race.js`:

- `refresh_success_rate` should stay low
- `refresh_rejected_rate` should stay high
- `refresh_unexpected_rate` must stay at `0`

For `tasks-throughput.js`:

- `http_req_duration p(95)` is your latency signal
- `http_req_failed` should stay low
- `rate_limit_hit` must stay `0`
- if `rate_limit_hit` goes above `0`, you are testing the limiter, not throughput

## What These Tests Prove

- repeated requests are throttled as designed
- refresh-token rotation behaves correctly under concurrency pressure
- authenticated task flow latency can be measured under sustained load

## What These Tests Still Do Not Prove

- multi-node behavior
- database saturation limits in production infrastructure
- real external OAuth2 provider latency
- real AI provider latency and cost behavior

AI load tests are intentionally not included here because they depend on external providers, billing, and provider-side quotas.
