package com.cocos.llallire.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cocos.llallire.infrastructure.adapter.out.cache.CacheMetrics;
import com.cocos.llallire.infrastructure.observability.OrderMetrics;
import com.cocos.llallire.infrastructure.observability.TelemetryNames;
import com.cocos.llallire.infrastructure.observability.UseCaseTelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Telemetry beans.
 *
 * <p>The application only depends on the OpenTelemetry API. At runtime the OpenTelemetry Java agent (bundled in
 * the Docker image) registers the global implementation and exports traces, metrics and logs over OTLP. Without
 * the agent (tests, {@code make run}) the global instance is a no-op and telemetry costs nothing.</p>
 */
@Configuration
public class ObservabilityConfig {

    /**
     * OpenTelemetry instance installed by the agent, or a no-op one.
     *
     * @return global OpenTelemetry
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    /**
     * Use case spans and duration histogram.
     *
     * @param openTelemetry OpenTelemetry instance
     * @return telemetry helper
     */
    @Bean
    public UseCaseTelemetry useCaseTelemetry(OpenTelemetry openTelemetry) {
        return new UseCaseTelemetry(openTelemetry);
    }

    /**
     * Order business metrics.
     *
     * @param openTelemetry OpenTelemetry instance
     * @return order metrics
     */
    @Bean
    public OrderMetrics orderMetrics(OpenTelemetry openTelemetry) {
        return new OrderMetrics(openTelemetry);
    }

    /**
     * Cache hit/miss/error metrics.
     *
     * @param openTelemetry OpenTelemetry instance
     * @return cache metrics
     */
    @Bean
    public CacheMetrics cacheMetrics(OpenTelemetry openTelemetry) {
        return new CacheMetrics(openTelemetry.getMeter(TelemetryNames.INSTRUMENTATION_SCOPE));
    }
}
