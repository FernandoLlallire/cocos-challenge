# Cocos Challenge — API de portfolio y órdenes

API REST que resuelve el [challenge de backend](backend-challenge.md). Permite:

- **Portfolio**: valor total de la cuenta, pesos disponibles para operar y los activos que posee el usuario (cantidad, valor de mercado y rendimiento total).
- **Buscar activos** por ticker o nombre.
- **Enviar órdenes** de compra o venta (`MARKET` y `LIMIT`), por cantidad exacta o por monto en pesos, y transferencias de pesos (`CASH_IN` / `CASH_OUT`).
- **Cancelar órdenes** pendientes (`NEW`).

**Stack:** Java 25 · Spring Boot 4.1 (Web MVC, Data JPA, Data Redis) · PostgreSQL 16 · Redis 7 · OpenTelemetry + Grafana (Prometheus, Loki, Tempo) · Docker o Podman · JUnit 5, Mockito, AssertJ y H2 para los tests.

> El enunciado pide Node.js. Esta implementación usa Java + Spring Boot. Los requisitos funcionales y técnicos (API REST, acceso a datos, test funcional del envío de órdenes, documentación) se cumplen igual.

---

## Índice

1. [Cómo ejecutarlo](#cómo-ejecutarlo)
2. [Endpoints](#endpoints)
3. [Arquitectura](#arquitectura)
4. [Reglas de negocio y decisiones](#reglas-de-negocio-y-decisiones)
5. [Cache](#cache)
6. [Observabilidad](#observabilidad)
7. [Diagramas de secuencia](#diagramas-de-secuencia)
8. [Cambios en la base de datos](#cambios-en-la-base-de-datos)
9. [Tests](#tests-1)
10. [Limitaciones y trabajo futuro](#limitaciones-y-trabajo-futuro)
11. [Trabajo con agentes de IA](#trabajo-con-agentes-de-ia)

---

## Cómo ejecutarlo

### Requisitos

- **Docker o Podman.** El `Makefile` detecta cuál está instalado y lo usa:
  - si hay Podman, usa Podman; si no, Docker;
  - para forzar uno: `make up CONTAINER_ENGINE=docker`;
  - si no hay ninguno, `make` muestra un error con los links de instalación;
  - si Docker está instalado pero el daemon no corre, también lo avisa.
- `make` y `curl`. En Windows, usar WSL2.
- **JDK 25 no es obligatorio.** La API se compila dentro de una imagen con el `Dockerfile` multi-stage. El JDK solo hace falta para `make run` o `make test`.

### Inicio rápido

```bash
make up        # PostgreSQL + Redis + observabilidad + API en contenedores → http://localhost:8080
make obs-open  # URLs de Grafana: dashboard, logs y trazas → http://localhost:3000
make status    # estado de los contenedores
make down      # detiene todo (los datos se conservan)
```

`make up` crea la red `cocos-net` y los contenedores `cocos-postgres`, `cocos-redis`, `cocos-otel` y `cocos-api`. Además construye la imagen de la API y espera a que cada servicio responda. La primera vez tarda unos minutos, porque descarga las imágenes y las dependencias de Maven.

### Comandos del Makefile

`make help` lista todos los comandos.

| Comando | Qué hace |
|---|---|
| `make up` | Levanta base, cache, observabilidad y API. |
| `make infra-up` | Levanta solo base y cache, para correr la API con `make run`. |
| `make db-up` / `db-down` / `db-logs` / `db-shell` | PostgreSQL. `db-shell` abre `psql`. |
| `make cache-up` / `cache-down` / `cache-logs` / `cache-shell` / `cache-flush` | Redis. `cache-shell` abre `redis-cli` y `cache-flush` vacía el cache. |
| `make obs-up` / `obs-down` / `obs-logs` / `obs-open` | Observabilidad: Grafana, Prometheus, Loki, Tempo y el OpenTelemetry Collector. `obs-open` muestra las URLs. |
| `make app-build` / `app-up` / `app-down` / `app-logs` | Imagen y contenedor de la API, con el agente de OpenTelemetry. `app-up` usa `.env` si existe; sin `.env`, desactiva el agente si la observabilidad no está corriendo. |
| `make run` | Corre la API con el JDK local (`./mvnw spring-boot:run`). |
| `make run-otel` | Igual que `run`, pero con el agente de OpenTelemetry enviando a la observabilidad local. |
| `make test` | Corre todos los tests. No necesita base ni Redis. |
| `make recreate` | Vuelve a crear los contenedores, conservando el volumen de la base. |
| `make clean` | Borra contenedores, red y el **volumen de la base** (borra los datos). |

Al crear la base por primera vez se ejecutan `database.sql` (esquema y datos provistos) y `db/indexes.sql` (índices agregados).

Nombres, puertos e imágenes son variables que se pueden sobreescribir. Por ejemplo, `make up APP_PORT=9090 DB_PORT=5433` levanta un segundo entorno sin chocar con el primero.

> **Migración desde la versión anterior del Makefile.** Antes la base corría en un *pod* de Podman llamado `cocos-db`. Si el Makefile lo encuentra, pide correr `make clean` una vez. Eso borra la base local, que se recrea con el seed.

**Alternativa con Compose:** `docker compose up -d --build` (o `podman compose up -d --build`) levanta los mismos cuatro servicios definidos en `compose.yml`.

### Variables de entorno

La API se configura **solo** con variables de entorno, así que el mismo build puede apuntar a una base o a un cache locales o en la nube.

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cocos` | URL JDBC de PostgreSQL. |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | Credenciales de la base. |
| `CACHE_TYPE` | `redis` | `redis` (cache compartido), `memory` (en el proceso, sin Redis) o `none` (sin cache). |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Servidor Redis. |
| `REDIS_SSL` | `false` | `true` para conectarse con TLS (servicios administrados). |
| `SPRING_DATA_REDIS_USERNAME` / `SPRING_DATA_REDIS_PASSWORD` | — | Credenciales de Redis (ACL / AUTH). |
| `SPRING_DATA_REDIS_URL` | — | URL completa (`redis://` o `rediss://`); reemplaza host, puerto, TLS y credenciales. |
| `REDIS_TIMEOUT` / `REDIS_CONNECT_TIMEOUT` | `500ms` / `1s` | Timeouts cortos: si Redis responde lento, la API sigue con la base. |
| `CACHE_FAILURE_COOLDOWN` | `30s` | Tiempo que se deja de usar Redis después de un error (circuit breaker). |
| `APP_CACHE_TTL_USERS`, `APP_CACHE_TTL_INSTRUMENTS`, `APP_CACHE_TTL_INSTRUMENT_SEARCH`, `APP_CACHE_TTL_MARKETDATA`, `APP_CACHE_TTL_PORTFOLIO` | `1h`, `1h`, `10m`, `1m`, `5m` | TTL de cada región del cache (ver [Cache](#cache)). |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | Destino de la telemetría (OTLP/HTTP): la observabilidad local, Datadog, New Relic, Grafana Cloud, etc. |
| `OTEL_EXPORTER_OTLP_HEADERS` | — | Credenciales del proveedor, por ejemplo `api-key=…` en New Relic. |
| `OTEL_SERVICE_NAME` / `OTEL_RESOURCE_ATTRIBUTES` | `cocos-api` / `service.namespace=cocos,deployment.environment.name=local` | Nombre del servicio y atributos (entorno, versión) con los que se agrupa la telemetría. |
| `OTEL_JAVAAGENT_ENABLED` | `true` | `false` desactiva el agente por completo. |
| `OTEL_METRIC_EXPORT_INTERVAL` | `10000` | Cada cuántos ms se envían las métricas. |

**Con la API en contenedor (`make app-up`):**

1. Copiá `.env.example` a `.env`.
2. Editá los valores. Si existe `.env`, se le pasa al contenedor con `--env-file`; si no existe, la API apunta a los contenedores locales.
3. Escribí líneas `CLAVE=valor` simples: sin comillas y sin `export`.
4. `.env` está en `.gitignore` porque puede tener secretos.

Ejemplo con base y cache en la nube:

```bash
cp .env.example .env
# .env
DB_URL=jdbc:postgresql://mi-db.ejemplo.com:5432/cocos?sslmode=require
DB_USER=cocos_app
DB_PASSWORD=********
REDIS_HOST=mi-cache.ejemplo.com
REDIS_PORT=6380
REDIS_SSL=true
SPRING_DATA_REDIS_PASSWORD=********
```

```bash
make app-up    # solo la API: no levanta base ni Redis locales
```

**Con la API local (`make run`):** se usan las variables del shell.

```bash
make infra-up                      # base y Redis locales
make run                           # usa los defaults (localhost)

DB_URL=jdbc:postgresql://mi-db.ejemplo.com:5432/cocos?sslmode=require \
DB_USER=cocos_app DB_PASSWORD=******** \
SPRING_DATA_REDIS_URL=rediss://default:********@mi-cache.ejemplo.com:6380 \
make run                           # base y cache en la nube

CACHE_TYPE=memory make run         # sin Redis
```

### Tests

```bash
make test      # o ./mvnw test
```

No necesitan PostgreSQL, Redis ni la observabilidad:
- los tests funcionales usan H2 en memoria en modo PostgreSQL, cargada con el mismo `database.sql`;
- el cache corre en memoria.

### Colección de requests

[`api.http`](api.http) tiene ejemplos de todos los endpoints. Se usa con la extensión **REST Client** de VS Code o con el cliente HTTP de IntelliJ.

---

## Endpoints

Base: `/api/v1`. Los errores se devuelven en formato [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457).

| Método | Ruta                       | Descripción                         | Respuestas                    |
|--------|----------------------------|-------------------------------------|-------------------------------|
| GET    | `/portfolio/{userId}`      | Portfolio del usuario               | 200, 400, 404                 |
| GET    | `/instruments?query=`      | Búsqueda de activos                 | 200                           |
| POST   | `/orders`                  | Enviar una orden                    | 201, 400, 404                 |
| POST   | `/orders/{orderId}/cancel` | Cancelar una orden `NEW`            | 200, 400, 404, 409            |

### `GET /portfolio/{userId}`

```bash
curl localhost:8080/api/v1/portfolio/1
```

```json
{
  "totalAccountValue": 904784.00,
  "availableCash": 627500.00,
  "positions": [
    { "instrumentId": 54, "ticker": "METR", "name": "MetroGAS S.A.", "quantity": 500,
      "marketValue": 114750.00, "totalReturnPct": -8.20 },
    { "instrumentId": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "quantity": 40,
      "marketValue": 37034.00, "totalReturnPct": -0.45 }
  ]
}
```

| Campo                       | Significado                                                                   |
|-----------------------------|-------------------------------------------------------------------------------|
| `totalAccountValue`         | Saldo en pesos (incluido lo reservado) + valor de mercado de las posiciones.  |
| `availableCash`             | Pesos disponibles para operar: saldo − reservado por compras `LIMIT` pendientes. |
| `positions[].quantity`      | Acciones tenidas (solo órdenes `FILLED`).                                     |
| `positions[].marketValue`   | `quantity × close`.                                                           |
| `positions[].totalReturnPct`| `(marketValue − costo) / costo × 100`, con costo promedio ponderado.          |

Las posiciones con tenencia cero no se incluyen y se ordenan por ticker.

### `GET /instruments?query=`

```bash
curl "localhost:8080/api/v1/instruments?query=pamp"
```

```json
[ { "id": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "type": "ACCIONES",
    "lastPrice": 925.85, "dailyReturnPct": 0.44 } ]
```

- Busca coincidencias parciales en el ticker **o** en el nombre, sin distinguir mayúsculas.
- Sin `query` (o con `query` vacío) devuelve todos los instrumentos.
- Los resultados se ordenan por ticker.
- `dailyReturnPct = (close − previousClose) / previousClose × 100`.
- Si un instrumento no tiene marketdata (por ejemplo, ARS), `lastPrice` y `dailyReturnPct` vuelven en `null`.

### `POST /orders`

```bash
curl -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"userId":1,"instrumentId":47,"side":"BUY","type":"MARKET","amount":10000}'
```

| Campo          | Obligatorio            | Descripción                                                                          |
|----------------|------------------------|--------------------------------------------------------------------------------------|
| `userId`       | sí                     | Usuario que envía la orden.                                                           |
| `instrumentId` | sí                     | Acción a operar; para `CASH_IN`/`CASH_OUT`, el instrumento ARS (id 66).              |
| `side`         | sí                     | `BUY`, `SELL`, `CASH_IN`, `CASH_OUT`.                                                 |
| `type`         | sí                     | `MARKET` o `LIMIT`. Las transferencias son siempre `MARKET`.                          |
| `size`         | uno de `size`/`amount` | Cantidad exacta de acciones (o de pesos, en transferencias).                          |
| `amount`       | uno de `size`/`amount` | Monto en pesos. Se envía la máxima cantidad entera: `floor(amount / price)`.          |
| `price`        | solo `LIMIT`           | Precio límite, positivo y con hasta 2 decimales. Las `MARKET` no lo admiten.          |

Respuesta `201 Created`, con header `Location: /api/v1/orders/{id}`:

```json
{ "id": 12, "instrumentId": 47, "userId": 1, "side": "BUY", "type": "MARKET", "size": 10,
  "price": 925.85, "status": "FILLED", "dateTime": "2026-11-19T11:30:20.132" }
```

La respuesta es **201 también cuando la orden queda `REJECTED`**. El rechazo es un resultado de negocio: la orden se registra en la tabla `orders`, como pide el enunciado, y el cliente lo ve en `status`. Las solicitudes mal formadas, en cambio, devuelven **400** y no se graban.

### `POST /orders/{orderId}/cancel`

Cancela una orden en estado `NEW` y responde 200 con la orden en estado `CANCELLED`. Si la orden está en otro estado, responde **409 Conflict**; si no existe, **404**.

Se usa `POST .../cancel` en lugar de `DELETE` porque la orden no se borra: cambia de estado y queda en el historial.

---

## Arquitectura

El proyecto sigue una **arquitectura hexagonal** (ports & adapters) en tres capas. Las dependencias apuntan siempre hacia el dominio:

```mermaid
flowchart LR
    subgraph infrastructure
        WEB[adapter/in/web<br/>Controllers, ExceptionHandler]
        CACHE[adapter/out/cache<br/>Caching* decorators,<br/>PortfolioCacheAdapter]
        PERS[adapter/out/persistence<br/>JPA entities, repositories, adapters]
        OBS[observability<br/>spans, métricas de negocio]
        CFG[config<br/>wiring, transacciones, cache,<br/>telemetría]
    end
    subgraph application
        IN[port/in<br/>UseCase interfaces]
        SVC[service<br/>GetPortfolio, SearchInstrument,<br/>PlaceOrder, CancelOrder]
        OUT[port/out<br/>Load*/Save* ports,<br/>PortfolioCachePort]
    end
    subgraph domain
        MODEL[model<br/>Order, CashPosition,<br/>InstrumentPosition, Portfolio...]
        DSVC[service<br/>PortfolioCalculator,<br/>OrderPlacementService]
    end
    WEB --> IN
    SVC -. implements .-> IN
    SVC --> OUT
    PERS -. implements .-> OUT
    CACHE -. implements .-> OUT
    CACHE -- delega en caso de miss --> PERS
    SVC --> DSVC
    SVC --> MODEL
    DSVC --> MODEL
    CFG --> SVC
```

```
src/main/java/com/cocos/llallire
├── domain                      # Sin dependencias de frameworks
│   ├── model                   # Order, OrderRequest, CashPosition, InstrumentPosition, Portfolio, ...
│   ├── service                 # PortfolioCalculator, OrderPlacementService
│   ├── exception               # DomainException, ResourceNotFoundException y subtipos
│   └── shared                  # Precision, Returns
├── application                 # Casos de uso; depende solo del dominio
│   ├── port/in                 # GetPortfolioUseCase, SearchInstrumentUseCase, PlaceOrderUseCase, CancelOrderUseCase
│   ├── port/out                # LoadUserPort, LoadInstrumentPort, LoadMarketdataPort, LoadOrderPort, SaveOrderPort,
│   │                           # PortfolioCachePort
│   ├── service                 # Implementaciones de los casos de uso
│   └── dto                     # Comandos de entrada y DTOs de salida
└── infrastructure              # Spring, JPA, Redis, HTTP
    ├── adapter/in/web          # Controllers REST, request bodies, GlobalExceptionHandler
    ├── adapter/out/persistence # Entidades JPA, repositorios Spring Data, adaptadores, EntityMapper
    ├── adapter/out/cache       # Decorators de cache, PortfolioCacheAdapter, CacheRegion, circuit breaker, métricas
    ├── observability           # UseCaseTelemetry, OrderMetrics, decorators Observed*UseCase
    └── config                  # Beans, transacciones (UseCasesConfig), cache (CacheConfig), telemetría (ObservabilityConfig)
```

### Responsabilidades y principios SOLID

| Principio | Cómo se aplica |
|---|---|
| **S** — Responsabilidad única | `CashPosition` calcula pesos; `InstrumentPosition` calcula una tenencia; `PortfolioCalculator` arma el portfolio; `OrderPlacementService` decide precio, cantidad y estado de una orden; `OrderRequest` valida el formato. Los casos de uso solo orquestan: cargan datos, delegan en el dominio y persisten. Los controllers solo traducen HTTP. |
| **O** — Abierto/cerrado | Los casos de uso dependen de interfaces (`port/out`). Para cambiar JPA por otra tecnología basta con escribir otro adaptador, sin tocar dominio ni aplicación. Las transacciones y el cache de datos de referencia se agregan como *decorators* (`UseCasesConfig`, `Caching*Port`), sin modificar los casos de uso. |
| **L** — Sustitución de Liskov | Cualquier implementación de un puerto (adaptador JPA, decorator con cache, mock de Mockito) es intercambiable. Por eso el cache se agregó sin tocar los casos de uso. |
| **I** — Segregación de interfaces | Puertos chicos y específicos: lectura y escritura de órdenes están separadas (`LoadOrderPort` / `SaveOrderPort`), así `GetPortfolio` no ve operaciones de escritura. |
| **D** — Inversión de dependencias | Aplicación y dominio definen las interfaces; la infraestructura las implementa. Dominio y aplicación no importan nada de Spring ni de JPA: los beans se crean en `infrastructure/config`. |

Otras decisiones de diseño:

- **Dominio puro.** Los servicios de dominio no tienen estado y reciben todos los datos ya cargados. Se testean sin mocks.
- **Modelos inmutables.** `Order` es un `record` y `cancel()` devuelve una copia nueva.
- **Transacciones fuera de la aplicación.** `UseCasesConfig` envuelve cada caso de uso con un `TransactionTemplate`: lectura/escritura para órdenes y solo lectura para consultas. Así la capa de aplicación no depende de `@Transactional`.
- **Cache como infraestructura.** Ver [Cache](#cache). La aplicación solo conoce `PortfolioCachePort`, porque la invalidación del portfolio es una regla del caso de uso. El resto del cache es transparente.
- **Observabilidad como infraestructura.** Ver [Observabilidad](#observabilidad). Los spans, las métricas de negocio y los logs de auditoría se agregan con decorators en `infrastructure`; dominio y aplicación no importan OpenTelemetry.
- **Reloj inyectado.** `PlaceOrder` recibe un `java.time.Clock`, lo que permite fijar la fecha en los tests.
- **Comentarios en inglés.** Todos los comentarios y el Javadoc del código están en inglés. Cada método y constructor de `src/main` tiene Javadoc.
- **Errores tipados.** Las excepciones de dominio se mapean a HTTP en un solo lugar (`GlobalExceptionHandler`):
  - `ResourceNotFoundException` → 404.
  - `InvalidOrderRequestException` → 400.
  - `OrderNotCancellableException` → 409.
  - Cualquier otro error → 500, sin exponer detalles internos.

---

## Reglas de negocio y decisiones

### Cálculo de pesos

Se calculan a partir de la tabla `orders`, con `size × price`:

| Orden                         | Efecto en el saldo | Efecto en lo reservado |
|-------------------------------|--------------------|------------------------|
| `CASH_IN` `FILLED`            | + monto            |                        |
| `SELL` `FILLED`               | + `size × price`   |                        |
| `CASH_OUT` `FILLED`           | − monto            |                        |
| `BUY` `FILLED`                | − `size × price`   |                        |
| `BUY` `NEW` (LIMIT pendiente) |                    | + `size × price`       |
| `REJECTED`, `CANCELLED`       | se ignoran         |                        |

- **Pesos disponibles** = saldo − reservado.
- **Valor total de la cuenta** = saldo + valor de mercado de las posiciones. Lo reservado sigue siendo del usuario, así que cuenta en el total.

> **Decisión:** las compras `LIMIT` pendientes **reservan** pesos y las ventas `LIMIT` pendientes **reservan** acciones. Sin esa reserva, un usuario podría enviar varias órdenes límite que, ejecutadas, gastarían más de lo que tiene. Al cancelar la orden, la reserva se libera.

### Cálculo de tenencias

- La cantidad de cada activo sale de las órdenes `BUY`/`SELL` en estado `FILLED`, como pide el enunciado.
- El **costo** se calcula por **costo promedio ponderado**, procesando las órdenes en orden cronológico:
  - **compra:** suma `size × price` al costo;
  - **venta:** descarga del costo la parte proporcional (`costo promedio × size`), sin alterar el costo promedio de las acciones que quedan.
- **Rendimiento total** = `(quantity × close − costo) / costo × 100`.
- **Datos inconsistentes.** En el seed, el usuario 1 vende 30 acciones del instrumento 31 habiendo comprado 20. Como no se admiten ventas en corto, en ese caso la tenencia queda en **cero** en lugar de volverse negativa, y la posición no aparece en el portfolio.

### Envío de órdenes

1. **Validación de formato** (`OrderRequest`). Si falla, responde 400 y la orden no se graba:
   - campos obligatorios presentes;
   - exactamente uno de `size` o `amount`, y positivo;
   - `LIMIT` con precio positivo de hasta 2 decimales; `MARKET` sin precio;
   - transferencias siempre `MARKET`;
   - `BUY`/`SELL` solo sobre acciones y `CASH_IN`/`CASH_OUT` solo sobre ARS.
2. **Precio:**
   - `MARKET` → último `close` del instrumento;
   - `LIMIT` → el precio enviado;
   - transferencias → 1, porque `size` es el monto en pesos, igual que en el seed.
3. **Cantidad:** el `size` enviado o `floor(amount / price)`. No se admiten fracciones de acciones.
4. **Estado:**
   - `REJECTED` si la cantidad resultante es 0 (el monto no alcanza para una acción);
   - `REJECTED` si `BUY`/`CASH_OUT` supera los pesos disponibles, o si `SELL` supera las acciones disponibles;
   - en otro caso, `FILLED` para `MARKET` y `NEW` para `LIMIT`.
5. La orden se graba en `orders` con el estado resultante y la fecha actual.

**Concurrencia.** `PlaceOrder` corre en una transacción y lo primero que hace es bloquear la fila del usuario (`SELECT … FOR UPDATE`). Así, dos órdenes simultáneas del mismo usuario no pueden validar contra el mismo saldo. Se verificó contra PostgreSQL: de 5 `CASH_OUT` concurrentes que juntos superaban el disponible, solo uno quedó `FILLED` y los otros 4 `REJECTED`.

### Otras decisiones

- Las órdenes `LIMIT` quedan en `NEW` y nunca se ejecutan, porque el enunciado indica que no hace falta simular el mercado.
- Los montos se devuelven con 2 decimales y los porcentajes con 2 decimales, con redondeo `HALF_UP`.
- No hay autenticación, como indica el enunciado: el `userId` viaja en la ruta o en el body. Por la misma razón, cancelar no verifica quién es el dueño de la orden.

---

## Cache

El cache evita que las consultas repetidas lleguen a la base. Se implementa con **Redis** y respeta la arquitectura hexagonal.

### Qué se cachea

| Región (clave Redis `cocos:<región>::<clave>`) | Contenido | TTL | Cómo se mantiene al día |
|---|---|---|---|
| `users` | Usuario por id (solo los existentes) | 1 h | Los usuarios no cambian desde la API. |
| `instruments` | Instrumento por id | 1 h | Datos de referencia. |
| `instrument-search` | Resultado de búsqueda por texto normalizado (`" PAMP "` = `"pamp"`) | 10 min | TTL. |
| `marketdata` | Último precio por instrumento, **incluida la ausencia** (`{"value":null}`, por ejemplo ARS) | 1 min | TTL: un precio cacheado tiene, como mucho, esa antigüedad. |
| `portfolio` | Portfolio calculado, por `usuario:generación` | 5 min | Se **invalida** al enviar o cancelar órdenes; el TTL es solo una red de seguridad. |
| `portfolio-generation` | Token de generación por usuario | 1 día | Se renueva en cada invalidación. |

**Qué no se cachea:**
- las órdenes y el lock del usuario (`loadUserByIdForUpdate`);
- el cálculo de pesos y acciones disponibles al enviar una orden.

Una orden se valida siempre contra la base. Usar datos cacheados ahí podría aceptar una orden sin fondos.

### Diseño

- **Datos de referencia → decorators de los puertos de salida.**
  - `CachingUserPort`, `CachingInstrumentPort` y `CachingMarketdataPort` implementan los mismos puertos que los adaptadores JPA y los envuelven con *cache-aside*: buscan en el cache y, si no está, consultan la base y guardan el resultado.
  - `CacheConfig` los registra como implementación `@Primary`, así que **los casos de uso no cambiaron**.
  - Las cargas por lote (`loadInstrumentsByIds`, `loadLastMarketdataByInstrumentIds`) consultan en la base **solo los ids que faltan** en el cache.
- **Portfolio → puerto de aplicación `PortfolioCachePort`.**
  - Invalidar el portfolio es una regla de negocio: cuando cambian las órdenes de un usuario, su portfolio deja de ser válido. Por eso la aplicación conoce el puerto:
    - `GetPortfolio` usa `getOrCompute`;
    - `PlaceOrder` invalida si la orden quedó `FILLED` o `NEW` (una `REJECTED` no cambia nada);
    - `CancelOrder` siempre invalida.
  - La implementación (`PortfolioCacheAdapter`) usa **tokens de generación**:
    - el portfolio se guarda bajo `usuario:generación`, e invalidar significa cambiar la generación;
    - así se evita la carrera clásica del cache-aside: un lector que leyó la base antes del commit de una orden guarda su resultado bajo la generación vieja, que ya nadie consulta;
    - la invalidación se hace en el momento y **se repite después del commit**.
- **Tolerancia a fallos.** El cache es una optimización, nunca una dependencia:
  - Cualquier error de Redis se trata como *miss* y la API responde desde la base. Los timeouts son cortos: 500 ms por comando.
  - Un **circuit breaker** compartido deja de usar Redis durante `CACHE_FAILURE_COOLDOWN` (30 s) después de un error. Así no se paga un timeout en cada llamada. Verificado: con Redis caído, la primera consulta tarda alrededor de 0,5 s y las siguientes unos 45 ms; se registra un solo warning.
  - Si una invalidación no se puede escribir, queda **pendiente** en la instancia y se reintenta antes de cualquier otra operación. Mientras tanto, el portfolio de ese usuario se calcula desde la base. Esto evita servir un portfolio viejo cuando Redis vuelve sin haber perdido sus datos, por ejemplo después de un corte de red.
- **Serialización.** JSON tipado por región (`TypedJsonRedisSerializer`): sin nombres de clase y legible desde `redis-cli`. Los montos `BigDecimal` conservan su escala.
- **Redis como cache puro.** Sin persistencia (`--save ""`, `--appendonly no`), con 128 MB de memoria máxima y política `allkeys-lru`.
- **Otras implementaciones.** `CACHE_TYPE=memory` usa un cache en el proceso, sin TTL; lo usan los tests y sirve para trabajar sin Redis. `CACHE_TYPE=none` desactiva el cache. Con más de una instancia de la API hay que usar `redis`, para que todas compartan las invalidaciones.

Para inspeccionarlo: `make cache-shell` y luego `KEYS cocos:*`, `GET cocos:users::1` o `TTL cocos:marketdata::47`.

---

## Observabilidad

El objetivo es tener lo mismo que da un APM comercial como Datadog o New Relic (dashboards, métricas, logs y trazas correlacionados), pero corriendo local con Podman o Docker y con estándares abiertos.

```bash
make up         # incluye la observabilidad
make obs-open   # http://localhost:3000/d/cocos-api
```

### Componentes

```mermaid
flowchart LR
    subgraph api[cocos-api]
        APP[Spring Boot app]
        AG[OpenTelemetry Java agent<br/>-javaagent]
    end
    subgraph otel[cocos-otel · grafana/otel-lgtm]
        COL[OpenTelemetry Collector<br/>:4317 gRPC · :4318 HTTP]
        PROM[(Prometheus<br/>métricas)]
        LOKI[(Loki<br/>logs)]
        TEMPO[(Tempo<br/>trazas)]
        GRAF[Grafana :3000<br/>dashboard Cocos API]
    end
    APP --- AG
    AG -- OTLP: trazas, métricas, logs --> COL
    COL --> PROM & LOKI & TEMPO
    TEMPO -- span metrics --> PROM
    PROM & LOKI & TEMPO --> GRAF
```

- **Agente APM.** El [OpenTelemetry Java agent](https://opentelemetry.io/docs/zero-code/java/agent/) se adjunta a la JVM dentro de la imagen (`-javaagent`), igual que los agentes de Datadog o New Relic. Instrumenta automáticamente, sin tocar el código:
  - requests HTTP de Spring MVC;
  - queries JDBC;
  - comandos Redis (Lettuce);
  - el pool HikariCP;
  - la JVM (memoria, GC, CPU, threads);
  - los logs de Logback.

  La versión está fijada en el `pom.xml` (`otel-javaagent.version`) y Maven la descarga al hacer `package`.
- **Backend.** [`grafana/otel-lgtm`](https://github.com/grafana/docker-otel-lgtm) junta en un contenedor el OpenTelemetry Collector, Prometheus, Loki, Tempo y Grafana, con los datasources ya conectados entre sí (de un log se salta a su traza y de una traza a sus logs).
- **Proveedor intercambiable.** Todo viaja por OTLP. Para usar Datadog, New Relic o Grafana Cloud alcanza con cambiar `OTEL_EXPORTER_OTLP_ENDPOINT` (y `OTEL_EXPORTER_OTLP_HEADERS`); `.env.example` tiene los tres ejemplos. No hace falta tocar el código.

### Qué se ve

**Dashboard "Cocos API"** (carpeta *Cocos*), definido en [`observability/grafana/dashboards/cocos-api.json`](observability/grafana/dashboards/cocos-api.json) y cargado por *provisioning*:

| Sección | Paneles |
|---|---|
| Overview (RED) | Requests/s, % de errores 5xx, % de 4xx, latencia p95, órdenes enviadas, hit ratio del cache; throughput, latencias p50/p95/p99 y respuestas por código, por endpoint. |
| Business | Órdenes por estado (`FILLED`/`NEW`/`REJECTED`), por lado y tipo; pesos aceptados por minuto; p95 por caso de uso; resultados no exitosos por caso de uso (excepciones de dominio y errores). |
| Cache | Hit ratio por región; operaciones por resultado (`hit`, `miss`, `error`, `skipped`); aperturas del circuit breaker; latencia p95 de Redis. |
| Database | Queries/s y latencia p95 por sentencia SQL; estado del pool de conexiones. |
| JVM | Heap, CPU, tiempo de GC, threads. |
| Logs | Volumen por nivel y visor de logs con búsqueda (variable *Log search*). |
| Traces | Últimos requests; al hacer click se abre la traza completa. |

**Trazas** (Grafana → Explore → Tempo). Cada request muestra su cadena completa, por ejemplo:

```
POST /api/v1/orders
└─ UseCase PlaceOrder            order.id, order.side, order.type, order.status
   ├─ SELECT cocos.users … FOR UPDATE
   ├─ GET  (Redis: instruments, marketdata)
   ├─ SELECT cocos.orders
   ├─ INSERT cocos.orders
   ├─ SET  (Redis: portfolio-generation)
   └─ Transaction.commit
```

**Logs** (Grafana → Explore → Loki). Todos los logs de la API llegan con `trace_id` y `span_id`, que también aparecen en la consola (`[trace_id=… span_id=…]`). Hay líneas de auditoría como `Order placed: id=16 … status=NEW`, `Order cancelled: …` y `Request rejected with 404: …`. La consulta `{service_name="cocos-api"} |= "REJECTED"` filtra los rechazos.

### Telemetría propia (además de la automática)

Se agrega en la capa de infraestructura con la API de OpenTelemetry. Sin el agente (en los tests o con `make run`) es un *no-op* sin costo.

| Señal | Nombre | Atributos | Dónde |
|---|---|---|---|
| Span | `UseCase <nombre>` | `use_case`; en órdenes, `order.id`, `order.side`, `order.type`, `order.status` | `UseCaseTelemetry` y los decorators `Observed*UseCase` |
| Histograma | `cocos.usecase.duration` (s) | `use_case`, `outcome` (`success` o nombre de la excepción) | `UseCaseTelemetry` |
| Contador | `cocos.orders.placed` | `side`, `type`, `status` | `OrderMetrics` |
| Contador | `cocos.orders.cancelled` | `side` | `OrderMetrics` |
| Contador | `cocos.orders.amount` (ARS) | `side` (solo `FILLED` y `NEW`) | `OrderMetrics` |
| Contador | `cocos.cache.requests` | `region`, `operation`, `result` | `CacheRegion` vía `CacheMetrics` |
| Contador | `cocos.cache.circuit_breaker.opened` | — | `CacheCircuitBreaker` |
| Log | `Order placed` / `Order cancelled` / `Request rejected with 4xx` | `trace_id`, `span_id` | decorators y `GlobalExceptionHandler` |

Las excepciones de dominio (404/400/409) quedan como evento en el span, pero no lo marcan como error: son resultados esperados. Los errores inesperados sí lo marcan.

### Verificado

Con el stack levantado por `make up`:
- las 33 consultas del dashboard devuelven datos;
- los logs llegan a Loki con su `trace_id`;
- las trazas incluyen los spans de JDBC y Redis bajo el span del caso de uso;
- al detener Redis, el dashboard registra la apertura del circuit breaker y los `error`/`skipped`, y el warning aparece en Loki.

---

## Diagramas de secuencia

Notación: `Caching*Port` son los decorators de cache, `*Persistence` los adaptadores JPA y `DB` es PostgreSQL. Para no recargar los diagramas, se omite la telemetría: cada caso de uso corre dentro de su span `UseCase <nombre>`, que abre `UseCaseTelemetry` antes de la transacción (ver [Observabilidad](#observabilidad)). Las flechas a `Redis` se omiten cuando el cache no está disponible: en ese caso el circuit breaker devuelve un *miss* sin intentar conectarse.

### Consultar portfolio

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant PC as PortfolioController
    participant TX as TransactionTemplate (read-only)
    participant GP as GetPortfolio
    participant PCA as PortfolioCacheAdapter
    participant R as Redis
    participant UP as CachingUserPort
    participant OP as OrderPersistence
    participant IP as CachingInstrumentPort
    participant MP as CachingMarketdataPort
    participant DB as PostgreSQL
    participant CALC as PortfolioCalculator

    C->>PC: GET /api/v1/portfolio/{userId}
    PC->>TX: execute(userId)
    TX->>GP: execute(userId)
    GP->>PCA: getOrCompute(userId, loader)
    PCA->>R: GET portfolio-generation::{userId}
    PCA->>R: GET portfolio::{userId}:{generation}
    alt cache hit
        R-->>PCA: PortfolioInformation
        PCA-->>GP: portfolio (0 queries a la base)
    else cache miss
        PCA->>GP: loader.get()
        GP->>UP: loadUserById(userId)
        UP->>R: GET users::{userId}
        opt miss
            UP->>DB: SELECT users
            UP->>R: SET users::{userId} (TTL 1h)
        end
        alt usuario inexistente
            GP-->>PC: UserNotFoundException (no se cachea)
            PC-->>C: 404 Problem Details
        end
        GP->>OP: loadOrdersByUserIdAndStatuses(userId, {FILLED, NEW})
        OP->>DB: SELECT orders (siempre desde la base)
        GP->>IP: loadInstrumentsByIds(ids)
        IP->>R: GET instruments::{id} (por id)
        opt ids faltantes
            IP->>DB: SELECT instruments WHERE id IN (faltantes)
            IP->>R: SET instruments::{id} (TTL 1h)
        end
        GP->>MP: loadLastMarketdataByInstrumentIds(ids)
        MP->>R: GET marketdata::{id} (por id)
        opt ids faltantes
            MP->>DB: SELECT último marketdata WHERE instrumentid IN (faltantes)
            MP->>R: SET marketdata::{id} = {value | null} (TTL 1m)
        end
        GP->>CALC: calculate(orders, instruments, marketdata)
        CALC-->>GP: Portfolio
        GP-->>PCA: PortfolioInformation
        PCA->>R: SET portfolio::{userId}:{generation} (TTL 5m)
        PCA-->>GP: portfolio
    end
    GP-->>PC: PortfolioInformation
    PC-->>C: 200 OK
```

### Buscar activos

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant IC as InstrumentController
    participant SI as SearchInstrument
    participant IP as CachingInstrumentPort
    participant MP as CachingMarketdataPort
    participant R as Redis
    participant DB as PostgreSQL

    C->>IC: GET /api/v1/instruments?query=pamp
    IC->>SI: execute(query)
    SI->>IP: searchByTickerOrName(query)
    IP->>R: GET instrument-search::{query normalizada}
    opt miss
        IP->>DB: SELECT instruments WHERE ticker/name ILIKE
        IP->>R: SET instrument-search::{query} (TTL 10m, incluso vacía)
    end
    IP-->>SI: List<Instrument>
    alt sin resultados
        SI-->>IC: []
    else con resultados
        SI->>MP: loadLastMarketdataByInstrumentIds(ids)
        MP->>R: GET marketdata::{id} (por id)
        opt ids faltantes
            MP->>DB: SELECT último marketdata WHERE instrumentid IN (faltantes)
            MP->>R: SET marketdata::{id} (TTL 1m)
        end
        MP-->>SI: Map<instrumentId, Marketdata>
        Note over SI: lastPrice = close<br/>dailyReturnPct = (close − previousClose) / previousClose × 100
        SI-->>IC: List<InstrumentInformation>
    end
    IC-->>C: 200 OK
```

### Enviar orden

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant OC as OrderController
    participant TX as TransactionTemplate
    participant PO as PlaceOrder
    participant UP as CachingUserPort
    participant IP as CachingInstrumentPort
    participant MP as CachingMarketdataPort
    participant OP as OrderPersistence
    participant OPS as OrderPlacementService
    participant PCA as PortfolioCacheAdapter
    participant R as Redis
    participant DB as PostgreSQL

    C->>OC: POST /api/v1/orders {userId, instrumentId, side, type, size|amount, price?}
    OC->>TX: execute(PlaceOrderCommand)
    TX->>PO: execute(command)
    PO->>PO: command.toOrderRequest()
    alt formato inválido
        PO-->>OC: InvalidOrderRequestException
        OC-->>C: 400 Problem Details (no se graba)
    end
    PO->>UP: loadUserByIdForUpdate(userId)
    UP->>DB: SELECT … FOR UPDATE (nunca cacheado: serializa las órdenes del usuario)
    PO->>IP: loadInstrumentById(instrumentId)
    IP->>R: GET instruments::{id} (si falta: SELECT y SET)
    opt MARKET sobre una acción
        PO->>MP: loadLastMarketdataByInstrumentId(instrumentId)
        MP->>R: GET marketdata::{id} (si falta: SELECT y SET, TTL 1m)
    end
    PO->>OP: loadOrdersByUserIdAndStatuses(userId, {FILLED, NEW})
    OP->>DB: SELECT orders (siempre desde la base: define pesos y acciones disponibles)
    PO->>OPS: place(request, instrument, marketPrice, orders, now)
    Note over OPS: 1. lado coherente con el tipo de instrumento<br/>2. precio: close / limit / 1<br/>3. size o floor(amount / price)<br/>4. REJECTED si no alcanzan pesos o acciones<br/>   si no: FILLED (MARKET) o NEW (LIMIT)
    OPS-->>PO: Order
    PO->>OP: save(order)
    OP->>DB: INSERT orders
    opt status FILLED o NEW
        PO->>PCA: invalidate(userId)
        PCA->>R: SET portfolio-generation::{userId} = nuevo token
        Note over PCA: se registra afterCommit para repetir la rotación
    end
    PO-->>TX: OrderInformation
    TX->>DB: COMMIT (libera el lock)
    TX->>PCA: afterCommit
    PCA->>R: SET portfolio-generation::{userId} = nuevo token
    OC-->>C: 201 Created (FILLED / NEW / REJECTED)
```

### Cancelar orden

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant OC as OrderController
    participant TX as TransactionTemplate
    participant CO as CancelOrder
    participant OP as OrderPersistence
    participant O as Order
    participant PCA as PortfolioCacheAdapter
    participant R as Redis
    participant DB as PostgreSQL

    C->>OC: POST /api/v1/orders/{orderId}/cancel
    OC->>TX: execute(orderId)
    TX->>CO: execute(orderId)
    CO->>OP: loadOrderById(orderId)
    OP->>DB: SELECT orders (siempre desde la base)
    alt no existe
        CO-->>OC: OrderNotFoundException
        OC-->>C: 404
    end
    CO->>O: cancel()
    alt estado distinto de NEW
        O-->>CO: OrderNotCancellableException
        OC-->>C: 409 Conflict
    else NEW
        O-->>CO: copia con status CANCELLED
        CO->>OP: save(cancelled)
        OP->>DB: UPDATE orders
        CO->>PCA: invalidate(userId)
        PCA->>R: SET portfolio-generation::{userId} = nuevo token
        CO-->>TX: OrderInformation
        TX->>DB: COMMIT
        TX->>PCA: afterCommit → nueva rotación de la generación
        OC-->>C: 200 OK
    end
```

---

## Cambios en la base de datos

El esquema de `database.sql` se usa sin modificaciones. Los cambios son:

| Cambio | Motivo |
|---|---|
| `db/indexes.sql`: índice `orders (userid, status)` | Portfolio y envío de órdenes filtran siempre las órdenes por usuario y estado. Sin índice, cada consulta recorre toda la tabla. |
| `db/indexes.sql`: índice `marketdata (instrumentid, date)` | Resuelve la búsqueda del último precio de cada instrumento, que se hace en todos los endpoints. |
| `compose.yml` y `Makefile` montan `db/indexes.sql` como segundo script de inicialización | Aplica los índices automáticamente al crear la base. |

Para no saturar la base con lecturas repetidas, además de los índices se agregó un [cache](#cache) con Redis.

En el mapeo JPA:

- Las claves foráneas de `orders` y `marketdata` se mapean como ids (`Long`) y no como relaciones `@ManyToOne`/`@OneToMany`. El dominio solo necesita los identificadores, y así se evitan cargas lazy y consultas N+1.
- `spring.jpa.hibernate.ddl-auto=none`: Hibernate no modifica el esquema.
- `spring.jpa.open-in-view=false`: la sesión no queda abierta durante la vista.

---

## Tests

```bash
make test   # o ./mvnw test — 119 tests
```

| Nivel | Clases | Qué cubre |
|---|---|---|
| **Dominio** (unitarios, sin mocks) | `OrderTest`, `OrderRequestTest`, `CashPositionTest`, `InstrumentPositionTest`, `MarketdataTest`, `ReturnsTest`, `PortfolioCalculatorTest`, `OrderPlacementServiceTest` | Validaciones de formato, cálculo de pesos y reservas, costo promedio ponderado, orden cronológico, ventas mayores a la tenencia, conversión de monto a acciones, y todos los casos de `FILLED`/`NEW`/`REJECTED`. |
| **Aplicación** (Mockito sobre los puertos) | `GetPortfolioTest`, `SearchInstrumentTest`, `PlaceOrderTest`, `CancelOrderTest` | Orquestación: qué puertos se llaman y en qué casos no se llaman, bloqueo del usuario, persistencia de órdenes rechazadas, errores 404, invalidación del portfolio cacheado (sí con `FILLED`/`NEW`/cancelación, no con `REJECTED`). |
| **Cache** (unitarios con cache en memoria) | `CachingUserPortTest`, `CachingInstrumentPortTest`, `CachingMarketdataPortTest`, `PortfolioCacheAdapterTest`, `CacheRegionTest`, `TypedJsonRedisSerializerTest` | Hit y miss; consulta solo de los ids faltantes; cache de ausencias; el lock nunca se cachea; generaciones y carrera lector/escritor; repetición después del commit; invalidaciones pendientes con Redis inaccesible; circuit breaker y cooldown; ida y vuelta del JSON de cada tipo que se guarda en Redis. |
| **Telemetría** (SDK de OpenTelemetry en memoria) | `UseCaseTelemetryTest`, `ObservedOrderUseCasesTest`, `CacheMetricsTest` | Span y duración por caso de uso; excepciones de dominio frente a errores inesperados; métricas de órdenes por estado (el monto no suma los `REJECTED`); hits, misses, errores y aperturas del circuit breaker. |
| **Funcionales** (contexto Spring completo + MockMvc + H2 con `database.sql`) | `OrderFunctionalTest`, `PortfolioFunctionalTest`, `InstrumentFunctionalTest`, `CacheFunctionalTest` | Flujo HTTP de punta a punta contra los datos del seed: envío de órdenes y su efecto en el portfolio, reservas de órdenes LIMIT, cancelación, rechazos grabados, códigos 400/404/409. Cada test hace rollback y limpia el cache. |

`OrderFunctionalTest` es el **test funcional sobre el envío de órdenes** que pide el enunciado.

`CacheFunctionalTest` **cuenta las sentencias SQL** que ejecuta Hibernate (estadísticas activadas en el perfil de test) para comprobar que:
- la segunda consulta de portfolio y de búsqueda ejecuta **0 sentencias**;
- enviar o cancelar una orden invalida el portfolio cacheado;
- una orden `REJECTED` conserva el cache;
- enviar una orden sigue leyendo de la base solo lo necesario: lock, órdenes e insert.

Los tests usan H2 en modo PostgreSQL y el cache en memoria, para que `make test` funcione sin infraestructura. Además se verificó manualmente con el stack completo levantado por `make up`, en contenedores con PostgreSQL 16 y Redis 7:
- lock pesimista ante órdenes concurrentes;
- la segunda consulta de portfolio no lee la tabla `orders`;
- claves y TTL en Redis;
- con Redis caído o pausado la API sigue respondiendo y no sirve datos viejos al volver.

---

## Limitaciones y trabajo futuro

- **Ejecución de órdenes LIMIT:** no se simula el mercado, así que las órdenes `NEW` no pasan a `FILLED`.
- **Pesos con centavos:** la columna `size` es `INT`, por lo que las transferencias se registran en pesos enteros (`floor(amount)`). Para soportar centavos habría que cambiar el tipo de la columna.
- **Escala del portfolio:** se calcula en memoria a partir de todas las órdenes del usuario. Con historiales muy grandes convendría mantener una tabla de posiciones (o snapshots) actualizada al ejecutar cada orden.
- **Autenticación:** fuera de alcance. Con autenticación, el `userId` saldría del token y la cancelación verificaría el dueño de la orden.
- **Migraciones:** los índices se aplican como script de inicialización. En un entorno real se gestionarían con Flyway o Liquibase.
- **Cache:**
  - Las lecturas por lote hacen un `GET` a Redis por id. Con muchos ids convendría `MGET` o un pipeline.
  - Un precio cacheado puede tener hasta `APP_CACHE_TTL_MARKETDATA` de antigüedad, también al ejecutar órdenes MARKET. Si el precio de ejecución tiene que ser exacto, conviene bajar ese TTL o leer el precio de la base en `PlaceOrder`.
  - Si una instancia se detiene con invalidaciones pendientes, el TTL del portfolio acota cuánto tiempo puede verse un dato viejo.
  - No hay tests automáticos contra un Redis real; se podrían agregar con Testcontainers.
- **Observabilidad:**
  - `grafana/otel-lgtm` es para desarrollo: guarda los datos dentro del contenedor y se pierden al eliminarlo.
  - No hay alertas configuradas; se podrían provisionar en Grafana (por ejemplo, error rate 5xx > 1% o aperturas del circuit breaker).
  - No hay profiling continuo (la imagen incluye Pyroscope, pero no se activó).

---

## Trabajo con agentes de IA

Este proyecto se desarrolla con asistencia de agentes de IA (Claude Code). El contexto que un agente acumula durante una sesión, como el análisis del código, las decisiones y los motivos, no se guarda en el repositorio: se pierde al cerrar la sesión o al cambiar de computadora. Por eso ese contexto se versiona junto al código.

### Dónde está el contexto

| Archivo | Para qué sirve |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Punto de entrada para cualquier agente (Claude Code, Codex, Cursor, Copilot…). Resume el proyecto, los comandos, las reglas de arquitectura, las reglas de negocio, las convenciones y los acuerdos de trabajo. |
| [`CLAUDE.md`](CLAUDE.md) | Lo lee Claude Code automáticamente. Importa `AGENTS.md` (`@AGENTS.md`) para no duplicar contenido y agrega indicaciones propias de Claude. |
| [`docs/agents/decisions.md`](docs/agents/decisions.md) | Registro de decisiones numeradas (contexto → decisión → consecuencias). Explica *por qué* el código es como es, para que ningún agente revierta una decisión sin saberlo. |
| [`docs/agents/session-log.md`](docs/agents/session-log.md) | Bitácora por sesión: qué se analizó, qué se cambió, cómo se verificó y qué quedó pendiente. Permite retomar el trabajo en otra máquina. |

Están en inglés, como el código. Este README sigue en español porque está dirigido a quienes evalúan el challenge.

### Buenas prácticas

1. **El contexto vive en el repo, no en el chat.** Toda decisión o hallazgo relevante se escribe en `docs/agents/` en el mismo cambio que lo produce. Si no está en el repo, el próximo agente no lo sabe.
2. **Un archivo de instrucciones único y compartido.** `AGENTS.md` es el estándar que leen la mayoría de las herramientas. `CLAUDE.md` lo importa en vez de copiarlo, para que no haya dos versiones que diverjan.
3. **Reglas explícitas y verificables.** Por ejemplo: "dominio y aplicación no importan Spring", "comentarios en inglés", "Javadoc en todo método de `src/main`", "no modificar `database.sql`". Un agente cumple mejor reglas concretas que intenciones generales.
4. **Registrar decisiones con su motivo.** Cada entrada de `decisions.md` dice qué se decidió y por qué (por ejemplo, D-004: las órdenes LIMIT reservan fondos). Una decisión no se edita: se agrega otra que la reemplaza.
5. **Bitácora al cerrar cada sesión.** Qué se encontró, qué se cambió, qué se verificó y, en especial, qué **no** se verificó.
6. **Verificar en lugar de suponer.** El agente corre `./mvnw test` antes de dar algo por terminado. Si el cambio toca persistencia o concurrencia, también prueba contra PostgreSQL real con un contenedor descartable, sin tocar la base de desarrollo. Informa los fallos tal como salen.
7. **Tests como red de seguridad.** Los tests funcionales usan el seed real y documentan la aritmética esperada. Así un agente detecta enseguida si un cambio altera los resultados del negocio.
8. **Pedir confirmación antes de acciones destructivas o externas.** Por ejemplo `make clean`, borrar datos, hacer commits o pushes, o publicar. El agente propone; el mantenedor decide y hace los commits.
9. **Cambios chicos y revisables.** Para cambios grandes, el agente presenta primero el plan. No cambia contratos públicos (campos JSON, códigos HTTP, rutas) sin avisar.
10. **Sin secretos.** Ni en el repo, ni en los prompts, ni en las llamadas a servicios externos.

### Flujo recomendado para una sesión nueva

1. El agente lee `AGENTS.md`, `docs/agents/decisions.md` y la última entrada de `docs/agents/session-log.md`. Claude Code carga `CLAUDE.md` solo.
2. Analiza el código afectado y, si el cambio es grande, presenta un plan.
3. Implementa respetando las reglas de arquitectura y las convenciones.
4. Verifica con `./mvnw test` y, si corresponde, contra PostgreSQL.
5. Actualiza en el mismo cambio:
   - `session-log.md`;
   - `decisions.md`, si hubo decisiones nuevas;
   - `AGENTS.md`, si cambiaron comandos, estructura o reglas;
   - el README, si cambió el comportamiento de la API.
6. El mantenedor revisa y hace el commit.
