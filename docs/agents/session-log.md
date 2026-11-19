# Agent session log

Newest entry last. Each entry records what an agent analysed, changed and verified, so the next session (possibly
on another machine, with no chat history) can continue. Keep entries factual and short.

Template:

```
## YYYY-MM-DD — <agent/model> — <topic>
**Analysis:** what was found.
**Changes:** what was changed (files/areas).
**Verification:** what was run and the result; what was NOT verified.
**Open items:** follow-ups.
```

---

## 2026-11-19 — Claude Code (Claude Opus 5.5) — Full review, order endpoints, tests, docs

**Analysis (state found before the changes):**

- The app could not start:
  - `LoadOrderPort` had no adapter;
  - `LoadUserAdapter` was not a `@Component`;
  - `PortfolioCalculator` was not a bean.
- `OrderStatus.CANCELED` did not match the DB value `CANCELLED`: loading cancelled orders would fail.
- `TotalEquity` and `AvailableCash` always returned 0, because `BigDecimal.add` results were discarded. Cash also
  ignored BUY/SELL.
- `InstrumentPosition.processOrder` only validated the size; no holding, cost or return was computed.
- N+1 queries: one instrument query and one marketdata query per position or search result.
- `OrderEntity.size` is `Integer` (DB `INT`) while the domain used `Long`, which broke compilation.
- `InvalidInstrumentException` used `%d` with a String.
- JPA entities had unused bidirectional `@OneToMany` collections.
- Seed data inconsistency: user 1 sells 30 shares of instrument 31 after buying 20.
- No tests, no README, no exception handling (everything became a 500).

**Changes:**

- Domain:
  - rewritten around `CashPosition`, `InstrumentPosition` (weighted average cost, reservations, valuation),
    `Portfolio`/`PositionValuation`, `OrderRequest` (format validation), `OrderPlacementService`, `OrderType`
    enum, `Order.cancel()`;
  - removed `TotalEquity` and `AvailableCash`;
  - new exception hierarchy (`DomainException`, `ResourceNotFoundException`, …).
- Application:
  - new ports `SaveOrderPort` and batch loads;
  - use cases `PlaceOrder` and `CancelOrder`;
  - `GetPortfolio` and `SearchInstrument` refactored to batch queries.
- Infrastructure:
  - persistence adapters for every port;
  - user row lock;
  - `OrderController` (`POST /orders`, `POST /orders/{id}/cancel`);
  - `GlobalExceptionHandler` (ProblemDetail);
  - `DomainConfig`, and `UseCasesConfig` with transactions.
- DB:
  - `db/indexes.sql`, mounted in `compose.yml` and the `Makefile`;
  - `ddl-auto=none`, `open-in-view=false`.
- Tests:
  - 79 tests (domain unit, application with Mockito, functional with H2 + `database.sql`);
  - `OrderFunctionalTest` is the functional test the challenge asks for.
- Docs: `README.md` (Spanish, with sequence diagrams), `api.http`, `AGENTS.md`, `CLAUDE.md`, `docs/agents/*`.
- All comments translated to English (maintainer requirement).

**Verification:**

- `./mvnw test` → 79 tests, 0 failures.
- Manual run against a throwaway PostgreSQL 16 container with the seed. Portfolio, search, MARKET by amount,
  LIMIT, cancel (200/409) and invalid request (400) worked. The concurrency check (5 parallel CASH_OUTs) gave
  1 FILLED and 4 REJECTED.
- The maintainer's local DB was recreated with `make clean && make up`; the indexes are present and the seed has
  11 orders.

**Open items / ideas:**

- LIMIT orders are never executed (no market simulation, by design).
- Possible improvements:
  - a positions table or snapshots for large histories;
  - Testcontainers for PostgreSQL-specific tests;
  - Flyway/Liquibase migrations;
  - authentication, so the user comes from a token and order ownership is checked on cancel.
- Nothing was committed; the maintainer commits.

## 2026-11-19 — Claude Code (Claude Opus 5.5) — Docker/Podman Makefile, containerized API, Redis cache

**Analysis:**

- The previous Makefile was Podman-only (pods), so it would not work for people with Docker.
- The API needed a local JDK 25.
- Every request hit the database.

**Changes:**

- **Makefile** rewritten:
  - engine auto-detection with clear errors when there is no engine, or Docker is installed but not running;
  - containers on the `cocos-net` network instead of a pod;
  - targets for the DB, Redis and the API (`up`, `infra-up`, `app-up`, `run`, `test`, `*-logs`, `*-shell`,
    `status`, `clean`, ...);
  - readiness waits for every service;
  - legacy pod detection.
- **Container files:**
  - `Dockerfile` (multi-stage) and `.dockerignore`;
  - `compose.yml` with postgres, redis and api;
  - `.env.example`, and `.env` added to `.gitignore`.
- **Cache:**
  - application port `PortfolioCachePort`, used by GetPortfolio, PlaceOrder and CancelOrder;
  - infrastructure package `adapter/out/cache` (decorators, generation-based portfolio cache, `CacheRegion`,
    circuit breaker, typed JSON serializer);
  - `CacheConfig` and `AppCacheProperties`;
  - Redis settings in `application.properties`;
  - `spring-boot-starter-data-redis` added to the pom.
- **Tests:** 111 in total.
  - New: cache unit tests and `CacheFunctionalTest` (counts SQL statements with Hibernate statistics).
  - Existing application tests updated for the cache port.
  - The test profile uses the in-memory cache, and `ClearCachesExtension` clears it before each functional test.
- **Docs:** README (requirements, Makefile commands, environment variables with cloud examples, new "Cache"
  section, sequence diagrams with the cache, tests, limitations), AGENTS.md, decisions D-016 to D-022.

**Verification:**

- `./mvnw -o test` → 111 tests, 0 failures.
- `make help`, and the error paths: no engine in PATH, and `CONTAINER_ENGINE=docker` without Docker.
- `make up` detected the legacy pod and asked for `make clean`.
- Full stack verified with Podman on a parallel throwaway stack (`cv-*` names, ports 5434/6380/8090), built and
  started by `make up` in about 1m22s:
  - the second portfolio request did not scan `orders`;
  - Redis keys and TTLs were correct; the absent ARS marketdata was stored as `{"value":null}`;
  - order placement invalidated the cached portfolio;
  - Redis stopped: the API kept answering (0.57s for the first request, then about 45ms) with a single warning;
  - Redis paused (data kept) while a CASH_IN was placed: no stale portfolio once it came back.
- The throwaway stack was removed afterwards (including an anonymous Redis volume, which led to `rm -f -v` in the
  Makefile).

**Not verified:**

- Docker itself (not installed on this machine). Only the commands shared by both engines are used.
- `docker compose` / `podman compose` (no compose CLI here; the YAML was only parsed).
- Windows.

**Open items:**

- The maintainer's own environment still has the legacy pod `cocos-db`. The `make clean` that migrates it was not
  run, because it deletes the local DB volume; the maintainer has to run it.
- Ideas: `MGET` for batch cache reads; Testcontainers for Redis/PostgreSQL integration tests.

## 2026-11-19 — Claude Code (Claude Opus 5.5) — Observability (APM) with OpenTelemetry and Grafana

**Changes:**

- `pom.xml`:
  - `opentelemetry-api`;
  - `opentelemetry-sdk-testing` (test scope);
  - property `otel-javaagent.version=2.33.1`, with `maven-dependency-plugin` copying the agent to `target/otel/`.
- `Dockerfile`: the agent is copied into the image and attached with `-javaagent`; `OTEL_*` defaults.
- Makefile:
  - `obs-up/obs-down/obs-logs/obs-open` (container `cocos-otel`, image `grafana/otel-lgtm:0.38.1`, ports
    3000/4317/4318);
  - `up` includes observability;
  - `app-up` points the agent to `cocos-otel`, or disables it when that container is not running;
  - new `run-otel`.
- `compose.yml`: `otel` service.
- `.env.example`: OTEL settings plus New Relic, Datadog and Grafana Cloud examples.
- Code (infrastructure only):
  - `infrastructure/observability` (`UseCaseTelemetry`, `OrderMetrics`, `Observed*UseCase`, `TelemetryNames`);
  - `ObservabilityConfig`;
  - `CacheMetrics`, used by `CacheRegion` and `CacheCircuitBreaker`;
  - `UseCasesConfig` wraps every use case with telemetry;
  - 4xx logging in `GlobalExceptionHandler`;
  - log pattern with `trace_id`/`span_id`.
- Grafana:
  - `observability/grafana/cocos-dashboards.yaml` (provisioning);
  - `observability/grafana/dashboards/cocos-api.json` (35 panels including rows), generated from the real metric
    names.
- Tests: 119 in total. New: `UseCaseTelemetryTest`, `ObservedOrderUseCasesTest`, `CacheMetricsTest`.
- Docs: README (section "Observabilidad", commands, env vars, architecture, tests, limitations), AGENTS.md,
  decisions D-023 to D-025.

**Verification:**

- `./mvnw -o test` → 119 tests, 0 failures.
- Throwaway stack (`cv-*`, Grafana on 3001) built and started by `make up` in about 1m50s. After generating
  traffic:
  - Prometheus received the HTTP, JVM, Hikari, `cocos_*` and Tempo span metrics;
  - Loki received the logs with `trace_id`/`span_id`;
  - Tempo held traces with JDBC and Redis spans under `UseCase …`;
  - the provisioned dashboard loaded in folder "Cocos", and all 33 panel queries (Prometheus, Loki, Tempo)
    returned data;
  - stopping Redis showed 1 breaker opening, the error/skipped cache results, and the WARN in Loki.
- The throwaway stack was removed afterwards.

**Not verified:**

- The dashboard was not inspected visually (no browser available); only its queries were checked.
- Exporting to Datadog / New Relic / Grafana Cloud (no accounts); the examples follow their OTLP documentation.
- Docker and compose CLIs (not installed here).

**Open items:**

- The maintainer still has to run `make clean && make up` once to migrate from the legacy pod.
- Ideas: Grafana alerts (5xx rate, breaker openings), Pyroscope profiling, a health endpoint for container
  healthchecks.
