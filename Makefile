# Local environment for the Cocos challenge API.
#
# The container engine is detected automatically: Podman if installed, otherwise Docker.
# Force one with: make up CONTAINER_ENGINE=docker
# Run `make help` to list the targets.

CONTAINER_ENGINE ?= $(shell if command -v podman >/dev/null 2>&1; then echo podman; \
	elif command -v docker >/dev/null 2>&1; then echo docker; fi)
ENGINE := $(strip $(CONTAINER_ENGINE))

NETWORK ?= cocos-net

DB_CONTAINER ?= cocos-postgres
DB_IMAGE ?= docker.io/library/postgres:16
DB_VOLUME ?= postgres_data
DB_PORT ?= 5432

CACHE_CONTAINER ?= cocos-redis
CACHE_IMAGE ?= docker.io/library/redis:7-alpine
CACHE_PORT ?= 6379

OBS_CONTAINER ?= cocos-otel
OBS_IMAGE ?= docker.io/grafana/otel-lgtm:0.38.1
GRAFANA_PORT ?= 3000
OTLP_GRPC_PORT ?= 4317
OTLP_HTTP_PORT ?= 4318

APP_CONTAINER ?= cocos-api
APP_IMAGE ?= localhost/cocos-challenge-api:latest
APP_PORT ?= 8080
ENV_FILE ?= .env

# Pod created by older versions of this Makefile (Podman only).
LEGACY_POD ?= cocos-db

# On SELinux hosts bind-mounted files need the :Z label to be readable inside the container.
SELINUX_LABEL := $(shell command -v getenforce >/dev/null 2>&1 && [ "$$(getenforce)" = "Enforcing" ] && echo ",Z")

.DEFAULT_GOAL := help
.PHONY: help check-engine check-legacy network up down recreate clean status \
	db-up db-wait db-down db-logs db-shell \
	cache-up cache-wait cache-down cache-logs cache-shell cache-flush \
	obs-up obs-wait obs-down obs-logs obs-open \
	infra-up app-build app-up app-wait app-down app-logs run run-otel test

help: ## Show this help
	@echo "Container engine: $(if $(ENGINE),$(ENGINE),NONE - install Docker or Podman)"
	@echo
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

check-engine:
ifeq ($(ENGINE),)
	@echo "ERROR: no container engine found."
	@echo "Install Docker (https://docs.docker.com/get-docker/) or Podman (https://podman.io/docs/installation)."
	@exit 1
endif
	@command -v $(ENGINE) >/dev/null 2>&1 || { echo "ERROR: '$(ENGINE)' is not installed. Install Docker (https://docs.docker.com/get-docker/) or Podman (https://podman.io/docs/installation)."; exit 1; }
	@$(ENGINE) info >/dev/null 2>&1 || { echo "ERROR: '$(ENGINE)' is installed but not running (start Docker Desktop / the docker service, or 'podman machine start')."; exit 1; }

check-legacy: check-engine
	@if [ "$(ENGINE)" = "podman" ] && podman pod exists $(LEGACY_POD) 2>/dev/null; then \
		echo "ERROR: found the pod '$(LEGACY_POD)' created by an older version of this Makefile."; \
		echo "Run 'make clean' once (it deletes the local database) and then 'make up'."; \
		exit 1; \
	fi

network: check-legacy
	@$(ENGINE) network inspect $(NETWORK) >/dev/null 2>&1 || $(ENGINE) network create $(NETWORK) >/dev/null

## ---------- Everything ----------

up: infra-up obs-up app-up ## Start PostgreSQL, Redis, observability (Grafana) and the API (in containers)

infra-up: db-up cache-up ## Start PostgreSQL and Redis only (to run the API with `make run`)

down: ## Stop all containers (data is kept)
	-@$(ENGINE) stop $(APP_CONTAINER) $(OBS_CONTAINER) $(CACHE_CONTAINER) $(DB_CONTAINER) >/dev/null 2>&1; echo "Stopped."

recreate: check-engine ## Remove the containers (keeping the database volume) and start everything again
	-@$(ENGINE) rm -f -v $(APP_CONTAINER) $(OBS_CONTAINER) $(CACHE_CONTAINER) $(DB_CONTAINER) >/dev/null 2>&1
	@$(MAKE) --no-print-directory up

clean: check-engine ## Remove containers, network and the database volume (DELETES the local data)
	-@$(ENGINE) rm -f -v $(APP_CONTAINER) $(OBS_CONTAINER) $(CACHE_CONTAINER) $(DB_CONTAINER) >/dev/null 2>&1
	-@if [ "$(ENGINE)" = "podman" ]; then podman pod rm -f $(LEGACY_POD) >/dev/null 2>&1; fi
	-@$(ENGINE) network rm $(NETWORK) >/dev/null 2>&1
	-@$(ENGINE) volume rm -f $(DB_VOLUME) >/dev/null 2>&1
	@echo "Removed containers, network '$(NETWORK)' and volume '$(DB_VOLUME)'."

status: check-engine ## Show the state of the containers
	@$(ENGINE) ps -a --filter name=cocos- --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

## ---------- Database ----------

db-up: network ## Start PostgreSQL (runs database.sql and db/indexes.sql on first creation)
	@if $(ENGINE) container inspect $(DB_CONTAINER) >/dev/null 2>&1; then \
		$(ENGINE) start $(DB_CONTAINER) >/dev/null; \
	else \
		$(ENGINE) run -d --name $(DB_CONTAINER) --network $(NETWORK) \
			-e POSTGRES_DB=cocos -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
			-p 127.0.0.1:$(DB_PORT):5432 \
			-v $(DB_VOLUME):/var/lib/postgresql/data \
			-v "$(CURDIR)/database.sql:/docker-entrypoint-initdb.d/01-database.sql:ro$(SELINUX_LABEL)" \
			-v "$(CURDIR)/db/indexes.sql:/docker-entrypoint-initdb.d/02-indexes.sql:ro$(SELINUX_LABEL)" \
			$(DB_IMAGE) >/dev/null; \
	fi
	@$(MAKE) --no-print-directory db-wait

db-wait:
	@printf "Waiting for PostgreSQL"; \
	for i in $$(seq 1 60); do \
		if $(ENGINE) exec $(DB_CONTAINER) pg_isready -h 127.0.0.1 -U postgres -d cocos >/dev/null 2>&1; then \
			echo " ready on localhost:$(DB_PORT)"; exit 0; \
		fi; printf "."; sleep 1; \
	done; echo; echo "ERROR: PostgreSQL is not ready; see 'make db-logs'."; exit 1

db-down: check-engine ## Stop PostgreSQL
	-@$(ENGINE) stop $(DB_CONTAINER) >/dev/null 2>&1; echo "PostgreSQL stopped."

db-logs: check-engine ## Follow the PostgreSQL logs
	$(ENGINE) logs -f $(DB_CONTAINER)

db-shell: check-engine ## Open psql on the local database
	$(ENGINE) exec -it $(DB_CONTAINER) psql -U postgres -d cocos

## ---------- Cache ----------

cache-up: network ## Start Redis (LRU cache, no persistence)
	@if $(ENGINE) container inspect $(CACHE_CONTAINER) >/dev/null 2>&1; then \
		$(ENGINE) start $(CACHE_CONTAINER) >/dev/null; \
	else \
		$(ENGINE) run -d --name $(CACHE_CONTAINER) --network $(NETWORK) \
			-p 127.0.0.1:$(CACHE_PORT):6379 \
			$(CACHE_IMAGE) redis-server --save "" --appendonly no \
				--maxmemory 128mb --maxmemory-policy allkeys-lru >/dev/null; \
	fi
	@$(MAKE) --no-print-directory cache-wait

cache-wait:
	@printf "Waiting for Redis"; \
	for i in $$(seq 1 30); do \
		if [ "$$($(ENGINE) exec $(CACHE_CONTAINER) redis-cli ping 2>/dev/null)" = "PONG" ]; then \
			echo " ready on localhost:$(CACHE_PORT)"; exit 0; \
		fi; printf "."; sleep 1; \
	done; echo; echo "ERROR: Redis is not ready; see 'make cache-logs'."; exit 1

cache-down: check-engine ## Stop Redis
	-@$(ENGINE) stop $(CACHE_CONTAINER) >/dev/null 2>&1; echo "Redis stopped."

cache-logs: check-engine ## Follow the Redis logs
	$(ENGINE) logs -f $(CACHE_CONTAINER)

cache-shell: check-engine ## Open redis-cli (e.g. KEYS cocos:*)
	$(ENGINE) exec -it $(CACHE_CONTAINER) redis-cli

cache-flush: check-engine ## Delete every cached entry
	$(ENGINE) exec $(CACHE_CONTAINER) redis-cli FLUSHALL

## ---------- Observability ----------

obs-up: network ## Start Grafana + Prometheus + Loki + Tempo + OpenTelemetry Collector (grafana/otel-lgtm)
	@if $(ENGINE) container inspect $(OBS_CONTAINER) >/dev/null 2>&1; then \
		$(ENGINE) start $(OBS_CONTAINER) >/dev/null; \
	else \
		$(ENGINE) run -d --name $(OBS_CONTAINER) --network $(NETWORK) \
			-p 127.0.0.1:$(GRAFANA_PORT):3000 \
			-p 127.0.0.1:$(OTLP_GRPC_PORT):4317 \
			-p 127.0.0.1:$(OTLP_HTTP_PORT):4318 \
			-v "$(CURDIR)/observability/grafana/cocos-dashboards.yaml:/otel-lgtm/grafana/conf/provisioning/dashboards/cocos-dashboards.yaml:ro$(SELINUX_LABEL)" \
			-v "$(CURDIR)/observability/grafana/dashboards:/otel-lgtm/cocos-dashboards:ro$(SELINUX_LABEL)" \
			$(OBS_IMAGE) >/dev/null; \
	fi
	@$(MAKE) --no-print-directory obs-wait

obs-wait:
	@printf "Waiting for Grafana"; \
	for i in $$(seq 1 90); do \
		if curl -sf "http://localhost:$(GRAFANA_PORT)/api/health" >/dev/null 2>&1; then \
			echo " ready on http://localhost:$(GRAFANA_PORT)"; exit 0; \
		fi; printf "."; sleep 1; \
	done; echo; echo "ERROR: Grafana is not ready; see 'make obs-logs'."; exit 1

obs-down: check-engine ## Stop the observability stack (telemetry is kept until the container is removed)
	-@$(ENGINE) stop $(OBS_CONTAINER) >/dev/null 2>&1; echo "Observability stopped."

obs-logs: check-engine ## Follow the observability stack logs
	$(ENGINE) logs -f $(OBS_CONTAINER)

obs-open: ## Print the observability URLs
	@echo "Dashboard:  http://localhost:$(GRAFANA_PORT)/d/cocos-api"
	@echo "Logs:       http://localhost:$(GRAFANA_PORT)/explore (data source: Loki)"
	@echo "Traces:     http://localhost:$(GRAFANA_PORT)/explore (data source: Tempo)"
	@echo "OTLP:       grpc localhost:$(OTLP_GRPC_PORT) / http localhost:$(OTLP_HTTP_PORT)"

## ---------- API ----------

app-build: check-engine ## Build the API image (no local JDK needed)
	$(ENGINE) build -t $(APP_IMAGE) .

app-up: network app-build ## Build and start the API container (uses .env if present)
	-@$(ENGINE) rm -f -v $(APP_CONTAINER) >/dev/null 2>&1
	@if [ -f "$(ENV_FILE)" ]; then \
		echo "Using environment from $(ENV_FILE)"; \
		$(ENGINE) run -d --name $(APP_CONTAINER) --network $(NETWORK) -p 127.0.0.1:$(APP_PORT):8080 \
			--env-file $(ENV_FILE) $(APP_IMAGE) >/dev/null; \
	else \
		echo "No $(ENV_FILE) found: using the local $(DB_CONTAINER), $(CACHE_CONTAINER) and $(OBS_CONTAINER) containers"; \
		if [ "$$($(ENGINE) container inspect -f '{{.State.Running}}' $(OBS_CONTAINER) 2>/dev/null)" = "true" ]; then \
			otel="-e OTEL_EXPORTER_OTLP_ENDPOINT=http://$(OBS_CONTAINER):4318"; \
		else \
			echo "Observability is not running ('make obs-up'): telemetry agent disabled"; \
			otel="-e OTEL_JAVAAGENT_ENABLED=false"; \
		fi; \
		$(ENGINE) run -d --name $(APP_CONTAINER) --network $(NETWORK) -p 127.0.0.1:$(APP_PORT):8080 \
			-e DB_URL=jdbc:postgresql://$(DB_CONTAINER):5432/cocos -e DB_USER=postgres -e DB_PASSWORD=postgres \
			-e CACHE_TYPE=redis -e REDIS_HOST=$(CACHE_CONTAINER) -e REDIS_PORT=6379 \
			$$otel $(APP_IMAGE) >/dev/null; \
	fi
	@$(MAKE) --no-print-directory app-wait

app-wait:
	@printf "Waiting for the API"; \
	for i in $$(seq 1 90); do \
		if curl -sf "http://localhost:$(APP_PORT)/api/v1/instruments?query=ars" >/dev/null 2>&1; then \
			echo " ready on http://localhost:$(APP_PORT)"; exit 0; \
		fi; \
		if ! $(ENGINE) container inspect -f '{{.State.Running}}' $(APP_CONTAINER) 2>/dev/null | grep -q true; then \
			echo; echo "ERROR: the API container stopped; see 'make app-logs'."; exit 1; \
		fi; printf "."; sleep 1; \
	done; echo; echo "ERROR: the API did not answer; see 'make app-logs'."; exit 1

app-down: check-engine ## Stop and remove the API container
	-@$(ENGINE) rm -f -v $(APP_CONTAINER) >/dev/null 2>&1; echo "API stopped."

app-logs: check-engine ## Follow the API logs
	$(ENGINE) logs -f $(APP_CONTAINER)

## ---------- Without containers for the API ----------

run: ## Run the API with the local JDK 25 (./mvnw); configure it with environment variables
	./mvnw spring-boot:run

run-otel: ## Like `run`, with the OpenTelemetry agent sending to the local observability stack
	./mvnw -q -DskipTests package
	OTEL_SERVICE_NAME=$${OTEL_SERVICE_NAME:-cocos-api} \
	OTEL_RESOURCE_ATTRIBUTES=$${OTEL_RESOURCE_ATTRIBUTES:-service.namespace=cocos,deployment.environment.name=local} \
	OTEL_EXPORTER_OTLP_ENDPOINT=$${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:$(OTLP_HTTP_PORT)} \
	OTEL_METRIC_EXPORT_INTERVAL=$${OTEL_METRIC_EXPORT_INTERVAL:-10000} \
	java -javaagent:target/otel/opentelemetry-javaagent.jar -jar $$(ls target/*.jar | head -1)

test: ## Run all tests (no database or Redis needed)
	./mvnw test
