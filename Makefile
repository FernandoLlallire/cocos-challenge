PODMAN ?= podman
POD_NAME ?= cocos-db
CONTAINER_NAME ?= cocos-postgres
VOLUME_NAME ?= postgres_data
POSTGRES_IMAGE ?= postgres:16
POSTGRES_PORT ?= 5432
DATABASE_SQL ?= $(CURDIR)/database.sql

.PHONY: create up down recreate clean

create:
	@if ! $(PODMAN) volume exists "$(VOLUME_NAME)"; then \
		$(PODMAN) volume create "$(VOLUME_NAME)"; \
	fi
	@if ! $(PODMAN) pod exists "$(POD_NAME)"; then \
		$(PODMAN) pod create --name "$(POD_NAME)" --publish "127.0.0.1:$(POSTGRES_PORT):5432"; \
	fi
	@if ! $(PODMAN) container exists "$(CONTAINER_NAME)"; then \
		$(PODMAN) create \
			--name "$(CONTAINER_NAME)" \
			--pod "$(POD_NAME)" \
			--env POSTGRES_DB=cocos \
			--env POSTGRES_USER=postgres \
			--env POSTGRES_PASSWORD=postgres \
			--volume "$(VOLUME_NAME):/var/lib/postgresql/data" \
			--volume "$(DATABASE_SQL):/docker-entrypoint-initdb.d/01-database.sql:ro,Z" \
			--health-cmd "pg_isready -U postgres -d cocos" \
			--health-interval 5s \
			--health-timeout 5s \
			--health-retries 5 \
			"$(POSTGRES_IMAGE)"; \
	fi

up: create
	@if [ "$$($(PODMAN) pod inspect --format '{{.State}}' "$(POD_NAME)")" != "Running" ]; then \
		$(PODMAN) pod start "$(POD_NAME)"; \
	fi

down:
	$(PODMAN) pod stop "$(POD_NAME)"

recreate:
	@if $(PODMAN) pod exists "$(POD_NAME)"; then \
		$(PODMAN) pod rm --force "$(POD_NAME)"; \
	fi
	$(MAKE) up

clean:
	@if $(PODMAN) pod exists "$(POD_NAME)"; then \
		$(PODMAN) pod rm --force "$(POD_NAME)"; \
	fi
	@if $(PODMAN) volume exists "$(VOLUME_NAME)"; then \
		$(PODMAN) volume rm --force "$(VOLUME_NAME)"; \
	fi
