package com.cocos.llallire.infrastructure.observability;

/**
 * Shared telemetry names.
 */
public final class TelemetryNames {

    /** Instrumentation scope of every span and metric created by this application. */
    public static final String INSTRUMENTATION_SCOPE = "com.cocos.llallire";

    /**
     * Constants class; not instantiable.
     */
    private TelemetryNames() {
    }
}
