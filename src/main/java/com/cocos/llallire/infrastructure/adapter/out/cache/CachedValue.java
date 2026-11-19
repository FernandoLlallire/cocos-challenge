package com.cocos.llallire.infrastructure.adapter.out.cache;

/**
 * Wrapper that allows caching the absence of a value ({@code value == null}), so repeated lookups of something
 * that does not exist (for example, marketdata of the ARS currency) do not hit the database.
 *
 * @param value cached value, or {@code null} if it is known not to exist
 * @param <T>   value type
 */
public record CachedValue<T>(T value) {

    /**
     * Wraps a value that may be {@code null}.
     *
     * @param value value or {@code null}
     * @param <T>   value type
     * @return wrapper
     */
    public static <T> CachedValue<T> of(T value) {
        return new CachedValue<>(value);
    }
}
