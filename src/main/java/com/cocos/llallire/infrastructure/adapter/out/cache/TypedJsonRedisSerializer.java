package com.cocos.llallire.infrastructure.adapter.out.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis serializer that stores values as plain JSON for one known type.
 *
 * <p>Each cache region has a single value type, so no class names are written into the payload. This keeps the
 * entries readable from {@code redis-cli} and avoids polymorphic deserialization.</p>
 *
 * @param <T> value type
 */
public class TypedJsonRedisSerializer<T> implements RedisSerializer<T> {
    private final JsonMapper jsonMapper;
    private final JavaType type;

    /**
     * Creates the serializer.
     *
     * @param jsonMapper JSON mapper
     * @param type       value type of the region
     */
    public TypedJsonRedisSerializer(JsonMapper jsonMapper, JavaType type) {
        this.jsonMapper = jsonMapper;
        this.type = type;
    }

    /**
     * Writes the value as JSON.
     *
     * @param value value, may be {@code null}
     * @return JSON bytes, or {@code null} for a {@code null} value
     * @throws SerializationException if the value cannot be written
     */
    @Override
    public byte[] serialize(T value) {
        if (value == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsBytes(value);
        } catch (JacksonException exception) {
            throw new SerializationException("Cannot serialize cache value of type " + type, exception);
        }
    }

    /**
     * Reads the JSON as the region type.
     *
     * @param bytes JSON bytes, may be {@code null}
     * @return the value, or {@code null} for {@code null} bytes
     * @throws SerializationException if the bytes cannot be read
     */
    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(bytes, type);
        } catch (JacksonException exception) {
            throw new SerializationException("Cannot deserialize cache value of type " + type, exception);
        }
    }
}
