package com.cocos.llallire.infrastructure.observability;

import java.util.function.Supplier;

import com.cocos.llallire.domain.exception.DomainException;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Wraps use case executions with a span and a duration metric.
 *
 * <p>The span sits between the HTTP span and the JDBC/Redis spans created by the OpenTelemetry agent, so a trace
 * shows which use case issued each query. The histogram {@code cocos.usecase.duration} has the attributes
 * {@code use_case} and {@code outcome} ({@code success} or the exception class name).</p>
 *
 * <p>Domain exceptions (not found, invalid request, not cancellable) are expected outcomes: they are recorded as
 * span events but do not mark the span as an error. Any other exception does.</p>
 */
public class UseCaseTelemetry {
    static final AttributeKey<String> USE_CASE = AttributeKey.stringKey("use_case");
    static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");

    private final Tracer tracer;
    private final DoubleHistogram duration;

    /**
     * Creates the telemetry helper.
     *
     * @param openTelemetry OpenTelemetry instance (the agent's, or a no-op one without agent)
     */
    public UseCaseTelemetry(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(TelemetryNames.INSTRUMENTATION_SCOPE);
        this.duration = openTelemetry.getMeter(TelemetryNames.INSTRUMENTATION_SCOPE)
                .histogramBuilder("cocos.usecase.duration")
                .setDescription("Duration of use case executions")
                .setUnit("s")
                .build();
    }

    /**
     * Runs the action inside a span named {@code UseCase <name>} and records its duration.
     *
     * @param useCase use case name
     * @param action  use case execution
     * @param <T>     result type
     * @return the action result
     */
    public <T> T observe(String useCase, Supplier<T> action) {
        Span span = tracer.spanBuilder("UseCase " + useCase)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(USE_CASE, useCase)
                .startSpan();
        long start = System.nanoTime();
        String outcome = "success";
        try (Scope ignored = span.makeCurrent()) {
            return action.get();
        } catch (RuntimeException exception) {
            outcome = exception.getClass().getSimpleName();
            span.recordException(exception);
            if (!(exception instanceof DomainException)) {
                span.setStatus(StatusCode.ERROR, exception.getMessage());
            }
            throw exception;
        } finally {
            duration.record((System.nanoTime() - start) / 1e9, Attributes.of(USE_CASE, useCase, OUTCOME, outcome));
            span.end();
        }
    }
}
