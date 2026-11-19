# AGENTS.md — Context for AI coding agents

This file is the single source of truth for any AI agent (Claude Code, Codex, Cursor, Copilot, etc.) working on this
repository. Read it fully before changing anything. `CLAUDE.md` imports it.

Deeper context lives in [`docs/agents/`](docs/agents/):

- [`decisions.md`](docs/agents/decisions.md) — why the code is the way it is (decision log). **Do not reverse a
  decision without discussing it with the maintainer.**

## 1. Project

Backend challenge for Cocos ([`backend-challenge.md`](backend-challenge.md)): a REST API to get a user's portfolio,
search instruments, place orders (MARKET/LIMIT, BUY/SELL, CASH_IN/CASH_OUT) and cancel pending orders.

- Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Data Redis), PostgreSQL 16, Redis 7, Lombok (JPA entities only).
- API docs: springdoc-openapi 3.2 → Swagger UI at `/swagger-ui.html`, OpenAPI document at `/v3/api-docs`.
  Request collections: `api.http` (REST Client) and `postman/cocos-api.postman_collection.json` (Postman v2.1).
- Observability: OpenTelemetry Java agent (attached in the image) + `grafana/otel-lgtm` (Grafana, Prometheus, Loki,
  Tempo, OTel Collector). The app depends only on `opentelemetry-api`.
- Local environment with Docker **or** Podman (auto-detected by the Makefile); the API image is built by the
  multi-stage `Dockerfile` (no local JDK needed).
- Tests: JUnit 5, Mockito, AssertJ, MockMvc, H2 (PostgreSQL mode) loaded with `database.sql`.
- The challenge statement asks for Node.js; the maintainer chose Java. This is documented in the README.

## 2. Commands

```bash
make help               # list targets; shows the detected engine (podman first, then docker)
make up                 # PostgreSQL + Redis + observability + API in containers (network cocos-net) → :8080
make obs-open           # Grafana URLs (dashboard http://localhost:3000/d/cocos-api, Explore for logs/traces)
make run-otel           # local JDK run with the OTel agent sending to the local observability stack
make infra-up           # only PostgreSQL + Redis, then `make run` for the API with the local JDK 25
make app-up             # rebuild and restart only the API container (uses .env if present)
make db-shell | cache-shell | app-logs | status
make down               # stop everything, keep data
make clean              # remove containers, network AND the DB volume (DELETES data — ask first)
make test               # ./mvnw test; no PostgreSQL or Redis needed
./mvnw -o test          # offline (dependencies are already in ~/.m2)
```

- Every Makefile name/port/image is a variable. To verify changes without touching the maintainer's environment,
  run a parallel throwaway stack, e.g.
  `make up NETWORK=cv-net DB_CONTAINER=cv-postgres DB_VOLUME=cv_pgdata DB_PORT=5434 CACHE_CONTAINER=cv-redis
  CACHE_PORT=6380 APP_CONTAINER=cv-api APP_PORT=8090 OBS_CONTAINER=cv-otel GRAFANA_PORT=3001 OTLP_GRPC_PORT=4327
  OTLP_HTTP_PORT=4328`, and remove it with `make clean` plus the same variables.
- Grafana's HTTP API is reachable without login (anonymous admin in otel-lgtm); query data sources through
  `http://localhost:3000/api/datasources/proxy/uid/{prometheus|loki|tempo}/...` to verify telemetry.
- Configuration is environment-only (`DB_*`, `CACHE_TYPE`, `REDIS_*`, `SPRING_DATA_REDIS_*`, `APP_CACHE_TTL_*`,
  `CACHE_FAILURE_COOLDOWN`, `API_DOCS_ENABLED`); see `.env.example` and the README "Variables de entorno".

Always run `./mvnw test` before declaring a change done. Report failures honestly.

## 3. Architecture (hexagonal) — rules that must hold

```
domain/          pure Java. No Spring, no JPA, no Jakarta imports.
  model/         Order, OrderRequest, CashPosition, InstrumentPosition, Portfolio, PositionValuation, enums
  service/       PortfolioCalculator, OrderPlacementService (stateless, receive already-loaded data)
  exception/     DomainException → ResourceNotFoundException (404) | InvalidOrderRequestException (400)
                 | OrderNotCancellableException (409) | InvalidOrderDataException (500)
  shared/        Precision (scales, rounding), Returns (percent change)
application/     depends only on domain. No Spring annotations.
  port/in/       one interface per use case (execute method)
  port/out/      small Load*/Save* interfaces (ISP: reads and writes are separate)
  service/       use case implementations: orchestrate only (load → delegate to domain → persist)
  dto/           commands (input) and *Information records (output)
infrastructure/  Spring and JPA.
  adapter/in/web/           controllers, request bodies, GlobalExceptionHandler (ProblemDetail, RFC 9457),
                            OpenApiExamples (JSON examples used by the OpenAPI annotations)
  adapter/out/persistence/  JPA entities, Spring Data repositories, *PersistenceAdapter, EntityMapper
  adapter/out/cache/        Caching*Port decorators, PortfolioCacheAdapter, CacheRegion, CacheCircuitBreaker,
                            TypedJsonRedisSerializer, CacheNames
  config/                   DomainConfig (domain beans, Clock), UseCasesConfig (wiring + TransactionTemplate),
                            CacheConfig (CacheManager, @Primary cached ports), AppCacheProperties,
                            ObservabilityConfig (OpenTelemetry, UseCaseTelemetry, OrderMetrics, CacheMetrics),
                            OpenApiConfig (API info + descriptions of the application response DTOs)
  observability/            UseCaseTelemetry (span + cocos.usecase.duration), OrderMetrics,
                            ObservedPlaceOrderUseCase / ObservedCancelOrderUseCase (decorators)
observability/grafana/      Grafana provisioning file + dashboards (cocos-api.json), mounted into cocos-otel
```

- Dependencies point inwards: infrastructure → application → domain. Never the other way.
- Business rules go in the domain. Use cases do not contain `if`s about money or status.
- Transactions are added in `UseCasesConfig` with `TransactionTemplate` decorators, not with `@Transactional`
  in the application layer.
- New use case checklist: port/in interface → service in application → bean + transaction in `UseCasesConfig` →
  controller → exception mapping if a new exception type appears → OpenAPI docs → request collections →
  unit + functional tests → README + this file.
- JPA entities map foreign keys as plain `Long` ids (no `@ManyToOne`/`@OneToMany`).
- Avoid N+1: load collections with batch port methods (`loadInstrumentsByIds`, `loadLastMarketdataByInstrumentIds`).
- **Observability rules:**
  - Domain and application never import OpenTelemetry. Spans, business metrics and audit logs are added by
    decorators in `infrastructure` (wired in `UseCasesConfig`: telemetry → transaction → use case).
  - A new use case gets `telemetry.observe("<Name>", ...)`. New business metrics go in an infrastructure class
    built from `OpenTelemetry` (see `OrderMetrics`), with low-cardinality attributes only (never ids as metric
    attributes; ids go on spans).
  - Domain exceptions do not mark spans as errors; unexpected exceptions do.
  - Dashboards live in `observability/grafana/dashboards/*.json`. Use Prometheus names as exported
    (`cocos_orders_placed_total`, `http_server_request_duration_seconds_bucket`, ...) and verify every query
    returns data against a running stack before finishing.
  - Telemetry goes out over OTLP only; switching to Datadog/New Relic/Grafana Cloud must stay a configuration
    change (`OTEL_EXPORTER_OTLP_*`).
- **API documentation rules:**
  - Controllers and request bodies (`adapter/in/web`) carry the springdoc/Swagger annotations (`@Tag`, `@Operation`,
    `@ApiResponse` per status code, `@Schema` on request fields). Examples are constants in `OpenApiExamples`,
    built from the seed data.
  - Response DTOs live in `application/dto` and must stay annotation-free: document their fields in
    `OpenApiConfig.responseDocs()`. A renamed field makes the document fail, and `OpenApiFunctionalTest` catches it.
  - `springdoc.override-with-generic-response=false`: each endpoint declares only the error codes it can return.
  - A new or changed endpoint also updates `api.http` and the Postman collection
    (`postman/cocos-api.postman_collection.json`), keeping the same examples.
- **Cache rules:**
  - Reference data (users, instruments, searches, marketdata) is cached by `Caching*Port` decorators of the outbound
    ports, registered as `@Primary` in `CacheConfig`. Use cases do not know about it.
  - The portfolio is cached through the application port `PortfolioCachePort`. **Any use case that changes a user's
    orders must call `portfolioCache.invalidate(userId)`** (PlaceOrder does it for FILLED/NEW, CancelOrder always).
  - Never cache what validates an order: `loadUserByIdForUpdate` (lock) and the user's orders are always read from
    the database.
  - All cache access goes through `CacheRegion` (errors = miss, shared circuit breaker). A new cache region needs:
    a name in `CacheNames`, a TTL in `AppCacheProperties.Ttl`, a typed serializer entry in `CacheConfig`, and tests.
  - Cache values must be JSON-serializable records with a fixed type per region; never store `null` (use
    `CachedValue` for negative caching).

## 4. Business rules (summary — details in README and decisions.md)

- Cash = FILLED orders: `CASH_IN`/`SELL` add `size*price`, `CASH_OUT`/`BUY` subtract. ARS is instrument id 66
  (type `MONEDA`), price 1, size = pesos.
- Pending (`NEW`) LIMIT BUYs reserve cash; pending LIMIT SELLs reserve shares. Available = balance − reserved.
- Total account value = cash balance (including reserved) + market value of positions.
- Positions use FILLED BUY/SELL only, chronological, **weighted average cost**. A sell larger than the holding
  clamps the holding to zero (seed data has this case: user 1, instrument 31).
- MARKET price = latest `close`; LIMIT price = request price (max 2 decimals). `amount` → `floor(amount/price)`.
- Status: REJECTED (size 0 or insufficient cash/shares, persisted, HTTP 201) | NEW (LIMIT) | FILLED (MARKET).
- Malformed requests → 400 and are NOT persisted. Only NEW orders can be cancelled (else 409).
- Placing an order locks the user row (`SELECT … FOR UPDATE`) to serialize concurrent orders.
- A MARKET order may use a cached price up to `APP_CACHE_TTL_MARKETDATA` old (default 1m); cash and holdings are
  never cached.

## 5. Conventions

- **All code comments, Javadoc, SQL/properties/.http comments and test comments are in English. Never Spanish.**
- Every method and constructor in `src/main` has Javadoc (`@param`, `@return`, `@throws`). Overrides use
  `{@inheritDoc}`.
- Exception messages and API error texts are in English.
- The README is written in Spanish (audience: the challenge reviewers). The maintainer chats in Spanish.
- Money: `BigDecimal`, scale 2, `HALF_UP` (`Precision`). Percentages: scale 2. Never `double`.
- Domain objects are immutable where possible (`record`s; `Order.cancel()` returns a copy).
- Tests:
  - domain → plain unit tests without mocks (`TestData` builds fixtures);
  - application → Mockito on the outbound ports, real domain services;
  - cache → unit tests with `ConcurrentMapCacheManager` and `CacheTestSupport`;
  - telemetry → `OpenTelemetryExtension` (opentelemetry-sdk-testing) and `TelemetryTestSupport` to read metrics;
  - functional → `@FunctionalTest` (full context + MockMvc + H2 with `database.sql`, rollback per test, in-memory
    cache cleared before each test). Expected numbers come from the seed; document the arithmetic in the test
    Javadoc. `CacheFunctionalTest` counts SQL statements with Hibernate statistics to prove cache hits.
- Do not edit `database.sql` (it is the provided seed). Schema additions go in `db/` and are mounted by
  `compose.yml` and the `Makefile`.

## 6. Working agreements for agents

1. **Read before writing.** Read this file, the relevant decision entries and the code you will touch.
2. **Keep context in the repo, not in the agent.** The maintainer switches machines; chat history and agent memory
   do not travel. After any meaningful change, update in the same change:
   - `docs/agents/session-log.md` (what you analysed, changed and verified);
   - `docs/agents/decisions.md` (any new or changed decision, with the why);
   - this file if commands, structure, rules or conventions changed;
   - the README if behaviour visible to API users changed.
3. **Verify, don't assume.** Run the tests; for persistence or concurrency changes also run against real
   PostgreSQL (a throwaway container on another port is fine). State exactly what was and was not verified.
4. **Ask before destructive or outward-facing actions:** `make clean`, dropping data, force pushes, commits or
   pushes, publishing anything. The maintainer commits; agents do not commit unless asked. Prefer a throwaway
   stack with different names/ports (section 2) over touching the maintainer's containers and volumes.
5. **Small, reviewable steps.** Explain the plan for large changes first; prefer focused diffs.
6. **Do not silently change public contracts** (JSON field names, status codes, endpoints). If needed, say so.
7. **Never put secrets in the repo** or in prompts to external services.
