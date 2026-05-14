package com.tracker.location_rider.kafka.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class JacksonKafkaSerializerTest {

    @Test
    void shouldRejectNullObjectMapperInConstructor() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    try (JacksonKafkaSerializer<Object> ignored = new JacksonKafkaSerializer<>(null)) {
                        throw new AssertionError("unreachable");
                    }
                })
                .withMessageContaining("objectMapper must not be null");
    }

    @Test
    void shouldReturnEmptyBytesWhenPayloadIsNull() {
        try (JacksonKafkaSerializer<Object> serializer = new JacksonKafkaSerializer<>(new ObjectMapper())) {
            byte[] result = serializer.serialize("topic", new RecordHeaders(), null);

            assertThat(result).isEmpty();
        }
    }

    @Test
    void shouldSerializePayloadWithHeadersOverload() {
        try (JacksonKafkaSerializer<Object> serializer = new JacksonKafkaSerializer<>(new ObjectMapper())) {
            Map<String, Object> payload = Map.of("id", "r-1");

            byte[] actual = serializer.serialize("topic", new RecordHeaders(), payload);

            assertThat(actual).isNotEmpty();
        }
    }

    @Test
    void shouldSerializePayloadWithTopicOnlyOverload() {
        try (JacksonKafkaSerializer<Object> serializer = new JacksonKafkaSerializer<>(new ObjectMapper())) {
            Map<String, Object> payload = Map.of("id", "r-2");

            byte[] actual = serializer.serialize("topic", payload);

            assertThat(actual).isNotEmpty();
        }
    }

    @Test
    void shouldWrapJsonProcessingExceptionAsSerializationException() {
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                };
            }
        };

        Object payload = new Object();
        try (JacksonKafkaSerializer<Object> serializer = new JacksonKafkaSerializer<>(failingObjectMapper)) {
            assertThatExceptionOfType(SerializationException.class)
                    .isThrownBy(() -> serializer.serialize("topic", new RecordHeaders(), payload))
                    .withMessageContaining("Failed to serialize payload of type")
                    .withMessageContaining(payload.getClass().getName())
                    .withCauseInstanceOf(JsonProcessingException.class);
        }
    }

    @Test
    void shouldTreatConfigureAndCloseAsNoOps() {
        try (JacksonKafkaSerializer<Object> serializer = new JacksonKafkaSerializer<>(new ObjectMapper())) {
            assertThatNoException().isThrownBy(() -> serializer.configure(Map.of("a", "b"), false));
            assertThatNoException().isThrownBy(serializer::close);
        }
    }
}
