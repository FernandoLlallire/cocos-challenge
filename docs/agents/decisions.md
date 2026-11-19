# Decision log

Each entry: context → decision → consequences. Numbered and never renumbered. To change a decision, add a new
entry that supersedes the old one (and mark the old one as superseded) instead of editing history.

---

## D-001 — Hexagonal architecture with a framework-free core

- **Context:** the challenge evaluates design. The project already had `domain` / `application` / `infrastructure`.
- **Decision:** `domain` and `application` have no Spring/JPA imports. Use cases are plain classes wired in
  `infrastructure/config`. Ports are small (`Load*Port`, `SaveOrderPort`).
- **Consequences:** the domain is unit-testable without mocks. Adding a framework annotation in those layers is a
  violation.

## D-002 — Transactions via `TransactionTemplate` decorators

- **Context:** use cases need transactions (the order flow locks the user), but `@Transactional` would bring Spring
  into the application layer.
- **Decision:** `UseCasesConfig` wraps each use case in a lambda that runs it inside a `TransactionTemplate`:
  read-write for orders, read-only for queries.
- **Consequences:** the application layer has no Spring imports. A new use case must be wrapped there.

## D-003 — Cash and positions are derived from `orders`

- **Context:** the statement says to compute holdings and cash from the `orders` table using `size`.
- **Decision:** no positions table. `CashPosition` and `InstrumentPosition` are computed on every request from the
  user's FILLED and NEW orders.
- **Consequences:** simple and always consistent. It does not scale to huge histories (future work: a positions
  table or snapshots updated when an order is filled).

## D-004 — Pending LIMIT orders reserve funds

- **Context:** without reservations, a user could place several LIMIT BUYs that together exceed the cash.
- **Decision:** NEW BUY reserves `size*price` of cash; NEW SELL reserves shares. Available = balance − reserved.
  Total account value includes the reserved cash, because it still belongs to the user.
- **Consequences:** for seed user 1, available cash is 627,500 (not 753,000). Cancelling releases the reservation.

## D-005 — Weighted average cost for the total return

- **Context:** the statement asks for the total return (%) per position but does not define the cost method.
- **Decision:** weighted average cost, with orders processed chronologically. Sells release cost proportionally.
  Return = `(quantity*close − cost) / cost * 100`.
- **Consequences:** realized P&L from past sells is not part of the position's return.

## D-006 — Oversold positions clamp to zero

- **Context:** the seed has user 1 selling 30 shares of instrument 31 after buying 20. The first implementation
  threw an exception and broke the portfolio endpoint.
- **Decision:** when a sell exceeds the holding, the holding becomes 0 (no short selling) and the position is
  hidden.
- **Consequences:** the portfolio endpoint never fails because of that inconsistent historical data.

## D-007 — Rejected orders are persisted and return 201; malformed requests return 400

- **Context:** the statement says rejected orders must be saved with status REJECTED.
- **Decision:** business rejections (insufficient cash/shares, amount below one share) are saved and returned
  with HTTP 201 and `status: REJECTED`. Format errors (`OrderRequest` validation, wrong side for the instrument
  type, unknown enum) return 400 and are not saved.
- **Consequences:** clients must check `status`, not only the HTTP code.

## D-008 — Order request rules

- Exactly one of `size` or `amount`. `amount` → `floor(amount / price)` shares; 0 shares → REJECTED.
- LIMIT requires a positive `price` with at most 2 decimals (the DB column is `NUMERIC(10,2)`); MARKET must not
  send a price and uses the latest `close`.
- CASH_IN/CASH_OUT must be MARKET and use the currency instrument (ARS); BUY/SELL cannot use it. Price is 1 and
  size is the amount in whole pesos (`size` is an INT column).

## D-009 — Pessimistic lock per user when placing orders

- **Context:** two concurrent orders could both pass the cash check.
- **Decision:** `PlaceOrder` first calls `loadUserByIdForUpdate` (`SELECT … FOR UPDATE` on `users`).
- **Consequences:** orders of the same user are serialized. Verified on PostgreSQL 16: of 5 concurrent CASH_OUTs
  that together exceeded the cash, 1 was FILLED and 4 REJECTED.

## D-010 — Cancel endpoint is `POST /orders/{id}/cancel`

- **Decision:** cancelling is a state transition, not a deletion, so it is a POST action. The rule lives in
  `Order.cancel()` (NEW only, otherwise `OrderNotCancellableException` → 409).

## D-011 — Database changes are additive

- **Decision:** `database.sql` stays untouched. `db/indexes.sql` adds `orders(userid, status)` and
  `marketdata(instrumentid, date)` and is mounted as the second init script in `compose.yml` and the `Makefile`.
  Hibernate `ddl-auto=none`, `open-in-view=false`.
- **Consequences:** an existing volume does not re-run init scripts: recreate it (`make clean && make up`) or
  apply the script manually.

## D-012 — JPA entities map foreign keys as ids

- **Decision:** `OrderEntity` and `MarketdataEntity` hold `instrumentId`/`userId` as `Long`; the unused
  bidirectional collections were removed.
- **Consequences:** no lazy loading, simpler mapping, no N+1.

## D-013 — Functional tests use H2 in PostgreSQL mode

- **Context:** `./mvnw test` must run on any machine without Docker/Podman.
- **Decision:** H2 (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`) loaded with the real `database.sql` and
  `db/indexes.sql`. Each functional test rolls back.
- **Consequences:** PostgreSQL-specific behaviour (locks under real concurrency) is verified manually against a
  throwaway PostgreSQL container. Testcontainers is a possible future improvement.

## D-014 — API naming

- Portfolio response: `totalAccountValue`, `availableCash`, `positions[]` (`quantity`, `marketValue`,
  `totalReturnPct`). Search response: `lastPrice`, `dailyReturnPct` (they replaced `totalEquity` and
  `performanceRate`).
- Errors use `ProblemDetail` (RFC 9457).

## D-015 — Language conventions

- Code, comments, Javadoc, exception messages and agent docs: **English**. README: Spanish. Conversation with
  the maintainer: Spanish.

## D-016 — Container engine auto-detection; plain containers instead of Podman pods

- **Context:** the project is shared with people who may have Docker instead of Podman.
- **Decision:** the Makefile detects the engine (`podman` first, then `docker`, overridable with
  `CONTAINER_ENGINE`) and fails with installation links if there is none, or if Docker is installed but not
  running. It uses only commands both engines share (`network`, `run`, `start`, `exec`, `inspect`, `rm -f -v`),
  so the Podman pod `cocos-db` was replaced by containers on the `cocos-net` network. The `:Z` SELinux label is
  added only when SELinux is enforcing. Image names are fully qualified (`docker.io/library/...`) to avoid Podman
  short-name prompts.
- **Consequences:** the old pod is detected and the user is asked to run `make clean` once. `compose.yml` remains
  as an alternative.

## D-017 — API runs in a container built from a multi-stage Dockerfile

- **Decision:** `make up` builds the API image (JDK 25 build stage with a Maven cache mount, JRE runtime, non-root
  user) so a reviewer only needs Docker/Podman. `make run` keeps the local-JDK option.
- **Consequences:** the first `make up` downloads images and Maven dependencies (about 1–2 minutes).

## D-018 — Configuration only through environment variables; `.env` for the container

- **Decision:** every connection setting has an env var with a local default (`DB_*`, `CACHE_TYPE`, `REDIS_*`,
  Spring relaxed binding for `SPRING_DATA_REDIS_URL/USERNAME/PASSWORD` and `APP_CACHE_TTL_*`). `make app-up`
  passes `.env` with `--env-file` if it exists, otherwise points to the local containers. `.env` is git-ignored;
  `.env.example` documents local and cloud values.

## D-019 — Redis cache: decorators for reference data, application port for the portfolio

- **Context:** reduce database load from repeated reads while keeping the hexagonal boundaries and order
  correctness.
- **Decision:**
  - Users, instruments, searches and latest marketdata are cached by `Caching*Port` decorators of the outbound
    ports (cache-aside), registered as `@Primary`. Use cases are unchanged.
  - The portfolio, whose validity depends on the user's orders (a business rule), is cached through
    `PortfolioCachePort` (`getOrCompute` / `invalidate`), used by GetPortfolio, PlaceOrder and CancelOrder.
  - Orders and the user lock are never cached.
  - TTLs: users 1h, instruments 1h, searches 10m, marketdata 1m, portfolio 5m (a safety net, since invalidation is
    explicit).
- **Consequences:** a repeated portfolio or search request runs 0 SQL statements (`CacheFunctionalTest`). A MARKET
  order may use a price up to the marketdata TTL old.

## D-020 — Portfolio invalidation with generation tokens, after commit and with pending retries

- **Decision:** portfolios are stored under `userId:generation`. Invalidating replaces the random generation token:
  - immediately and again in `afterCommit`, which avoids the cache-aside race of a reader that loaded pre-commit
    data;
  - if the token cannot be written (cache unreachable), the user is kept in a local pending set, retried before any
    cache operation, and that user's portfolio bypasses the cache meanwhile.
- **Consequences:** verified with `podman pause` on Redis: no stale portfolio after the cache comes back. If an
  instance dies with pending invalidations, the portfolio TTL bounds the staleness.

## D-021 — The cache is never a hard dependency

- **Decision:**
  - all cache access goes through `CacheRegion`, which turns errors into misses;
  - a shared `CacheCircuitBreaker` skips the cache for `CACHE_FAILURE_COOLDOWN` (30s) after an error;
  - Redis timeouts are short (500ms command, 1s connect);
  - Redis runs as a pure LRU cache (no persistence, 128MB).
- **Consequences:** with Redis down the first request pays one timeout (about 0.5s), the next ones about 45ms, and
  only one warning is logged per outage (without the breaker a portfolio took 6.6s).

## D-022 — Typed JSON in Redis

- **Decision:** one value type per region, serialized by `TypedJsonRedisSerializer` with a mapper that ignores
  derived `isX()` methods. No class names in the payload; `CachedValue` wraps values that may be absent.
- **Consequences:** entries are readable in `redis-cli`; changing a cached record's shape requires flushing the
  cache (`make cache-flush`) or a key prefix change.

## D-023 — Observability with OpenTelemetry: Java agent + grafana/otel-lgtm

- **Context:** the maintainer wants Datadog/New Relic-style APM (dashboards, metrics, logs, traces) running locally
  with Podman.
- **Decision:**
  - The OpenTelemetry Java agent (version pinned in `pom.xml`, copied by `maven-dependency-plugin` on `package`)
    is attached with `-javaagent` in the Docker image. It auto-instruments HTTP, JDBC, Lettuce, HikariCP, the JVM
    and Logback.
  - The backend is the `grafana/otel-lgtm` image (Collector + Prometheus + Loki + Tempo + Grafana) as the
    `cocos-otel` container (`make obs-up`, part of `make up`).
  - Everything is exported over OTLP, so moving to Datadog (Agent OTLP ingest), New Relic (`otlp.nr-data.net`) or
    Grafana Cloud is only `OTEL_EXPORTER_OTLP_*` configuration.
  - Versions chosen at least two weeks old: agent 2.33.1 (bundles API 1.67, compatible with the Boot-managed API
    1.62), otel-lgtm 0.38.1.
- **Consequences:**
  - The application depends only on `opentelemetry-api`; without the agent it is a no-op (tests, `make run`).
  - `make app-up` disables the agent if the observability container is not running, to avoid export errors.
  - Telemetry data is lost when the container is removed (development setup).

## D-024 — Custom telemetry through infrastructure decorators

- **Decision:**
  - `UseCaseTelemetry` wraps every use case (outside the transaction) with a span `UseCase <name>` and the
    histogram `cocos.usecase.duration{use_case,outcome}`.
  - `ObservedPlaceOrderUseCase` and `ObservedCancelOrderUseCase` add order attributes to the span, the counters
    `cocos.orders.placed{side,type,status}`, `cocos.orders.cancelled` and `cocos.orders.amount`, and an INFO
    audit log.
  - `CacheMetrics` counts `cocos.cache.requests{region,operation,result}` and breaker openings.
  - `GlobalExceptionHandler` logs 4xx at INFO.
  - Log lines include `trace_id`/`span_id`.
- **Consequences:**
  - Domain and application stay free of telemetry.
  - Domain exceptions are span events, not errors.
  - Metric attributes are low-cardinality; ids only go on spans.

## D-025 — Dashboard as code

- **Decision:** the "Cocos API" dashboard is JSON in `observability/grafana/dashboards/`, loaded by a Grafana
  provisioning file mounted into the container. It covers:
  - RED metrics per endpoint;
  - business metrics (orders, use cases);
  - cache (hit ratio, errors, breaker, Redis latency);
  - database (query rate/latency from Tempo span metrics, Hikari pool);
  - JVM;
  - logs (Loki);
  - recent traces (Tempo).
- **Consequences:** queries use the Prometheus names produced by the OTLP translation (`_total`, unit suffixes);
  they were verified against a running stack (33 queries, all returning data).

## D-026 — OpenAPI with springdoc; response DTOs documented from infrastructure

- **Context:** the maintainer asked for Swagger docs for every endpoint (with the examples of `api.http` and the
  meaning of each DTO field) and a Postman collection.
- **Decision:**
  - `springdoc-openapi-starter-webmvc-ui` 3.2.0 (the 3.x line targets Spring Boot 4; 3.2.0 is built against Boot
    4.1.2). Swagger UI at `/swagger-ui.html`, document at `/v3/api-docs`.
  - Controllers and `PlaceOrderRequest` (infrastructure) are annotated; examples are constants in
    `OpenApiExamples` with values from the seed.
  - The response DTOs are in `application/dto`, which must not depend on frameworks (D-001), so they are not
    annotated: `OpenApiConfig` adds their field descriptions with an `OpenApiCustomizer` that fails if a
    documented schema/property no longer exists (guarded by `OpenApiFunctionalTest`).
  - `springdoc.override-with-generic-response=false` so the `GlobalExceptionHandler` statuses are not added to
    every operation; each endpoint lists only its own error codes.
  - The Postman collection (`postman/cocos-api.postman_collection.json`, v2.1) mirrors `api.http`, adds error
    cases, saved example responses and status-code tests.
- **Consequences:** the docs are public on the API port (no auth in this project). Field documentation of the
  response DTOs is kept by name in `OpenApiConfig`, away from the records. Verified: every request of the Postman
  collection returned the expected status against the app on a throwaway PostgreSQL with the seed.

## D-027 — Spring MVC client errors keep their status in `GlobalExceptionHandler`

- **Context:** the catch-all `@ExceptionHandler(Exception.class)` also caught Spring MVC's own exceptions, so an
  unknown path (`NoResourceFoundException`) answered 500 and was logged at ERROR as "Unexpected error". It showed
  up in Loki when `/v3/api-docs` was requested on an API image built before springdoc was added.
- **Decision:** in the catch-all, exceptions implementing `org.springframework.web.ErrorResponse` (unknown path
  → 404, wrong method → 405, unsupported media type → 415, missing parameter → 400, ...) return their own
  `ProblemDetail` and are logged at INFO. Everything else stays 500 + ERROR.
- **Consequences:** public behaviour change for invalid requests only (500 → correct 4xx); no change for valid
  endpoints. Error logs and error spans now mean real server failures. Guarded by `ErrorHandlingFunctionalTest`.

## D-028 — Quiet startup: no Redis repositories, API docs behind a switch

- **Context:** two startup log lines in Loki. Spring Data Redis (INFO) scanned the JPA repositories because both
  Spring Data modules are on the classpath, although Redis is only used as a cache. springdoc (WARN) reported that
  Swagger UI is enabled by default; springdoc 3.2.0 logs it whenever the UI is enabled, even if set explicitly.
- **Decision:** `spring.data.redis.repositories.enabled=false`. `springdoc.api-docs.enabled` and
  `springdoc.swagger-ui.enabled` read `API_DOCS_ENABLED` (default `true`, the docs are part of the challenge), and
  the `SpringDocAppInitializer` logger is set to ERROR because the exposure is now an explicit setting.
- **Consequences:** no behaviour change by default. `API_DOCS_ENABLED=false` hides the docs in production. If
  Redis repositories are ever needed, remove the property.
