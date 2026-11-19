# syntax=docker/dockerfile:1
# Multi-stage build of the API image. Works with Docker (BuildKit) and Podman (Buildah).

# --- Build stage: compiles the application with the Maven wrapper (no local JDK or Maven needed). ---
FROM docker.io/library/eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# The cache mount keeps downloaded dependencies between builds. `package` also copies the OpenTelemetry Java agent
# (version pinned in pom.xml) to target/otel/.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && cp target/*.jar app.jar \
    && cp target/otel/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# --- Runtime stage: JRE only, non-root user, OpenTelemetry agent attached (APM: traces, metrics and logs). ---
FROM docker.io/library/eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
COPY --from=build /workspace/opentelemetry-javaagent.jar opentelemetry-javaagent.jar
USER app
EXPOSE 8080
# Use up to 75% of the container memory limit for the heap.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# Telemetry defaults; override them at run time (see .env.example). OTEL_JAVAAGENT_ENABLED=false disables the agent.
ENV OTEL_SERVICE_NAME="cocos-api" \
    OTEL_RESOURCE_ATTRIBUTES="service.namespace=cocos,deployment.environment.name=local" \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318" \
    OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf" \
    OTEL_METRIC_EXPORT_INTERVAL="10000"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar /app/app.jar"]
