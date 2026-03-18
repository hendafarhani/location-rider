package com.tracker.location_rider.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.location_rider.kafka.serializer.JacksonKafkaSerializer;
import com.tracker.location_rider.model.RiderData;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Bean
    public KafkaTemplate<String, RiderData> kafkaTemplate(ProducerFactory<String, RiderData> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, RiderData> producerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                                           ObjectMapper objectMapper){
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        return new DefaultKafkaProducerFactory<>(
                config,
                new StringSerializer(),
                new JacksonKafkaSerializer<>(objectMapper)
        );
    }
}
