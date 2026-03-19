package com.tracker.location_rider.kafka.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * Minimal Jackson-backed serializer used until KafkaJsonSerializer becomes available in Spring Kafka.
 */
public final class JacksonKafkaSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper;

    public JacksonKafkaSerializer(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "objectMapper must not be null");
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No-op: configuration handled via ObjectMapper setup.
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object data) {
        if (data == null) {
            return new byte[]{};
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Failed to serialize payload of type " + data.getClass().getName(), ex);
        }
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        return serialize(topic, null, data);
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}

