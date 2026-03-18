package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import com.tracker.location_rider.repository.RiderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderLocationServiceImplTest {

    @Mock
    private KafkaTemplate<String, RiderData> kafkaTemplate;

    @Mock
    private RiderRepository riderRepository;

    private RiderLocationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RiderLocationServiceImpl(kafkaTemplate, riderRepository);
        ReflectionTestUtils.setField(service, "random", new Random(42));
        clearCachedPositions();
    }

    @Test
    void publishLatestLocations_skipsKafkaWhenThereAreNoRiders() throws Exception {
        when(riderRepository.findAll()).thenReturn(List.of());

        service.publishLatestLocations();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishLatestLocations_publishesDeterministicLocationUpdates() throws Exception {
        RiderEntity rider = RiderEntity.builder()
                .identifier("rider-1")
                .name("Rider One")
                .build();
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        presetLocation(rider.getIdentifier(), 51.50, -0.12);

        CompletableFuture<SendResult<String, RiderData>> future = CompletableFuture.completedFuture(null);

        ArgumentCaptor<RiderData> riderDataCaptor = ArgumentCaptor.forClass(RiderData.class);
        when(kafkaTemplate.send(eq("rider.location"), riderDataCaptor.capture())).thenReturn(future);

        service.publishLatestLocations();

        verify(kafkaTemplate).send(eq("rider.location"), any(RiderData.class));
        RiderData sentPayload = riderDataCaptor.getValue();
        assertThat(sentPayload.getIdentifier()).isEqualTo("rider-1");
        assertThat(sentPayload.getUserName()).isEqualTo("Rider One");
        assertThat(sentPayload.getLocation()).isNotNull();
    }

    @Test
    void publishLatestLocations_propagatesKafkaExceptions() {
        RiderEntity rider = RiderEntity.builder()
                .identifier("rider-error")
                .name("Rider Error")
                .build();
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        presetLocation(rider.getIdentifier(), 51.40, -0.20);
        when(kafkaTemplate.send(eq("rider.location"), any(RiderData.class)))
                .thenThrow(new IllegalStateException("Kafka unavailable"));

        assertThatThrownBy(() -> service.publishLatestLocations())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka unavailable");
    }

    private void clearCachedPositions() {
        @SuppressWarnings("unchecked")
        Map<String, Location> positions = (Map<String, Location>) ReflectionTestUtils.getField(service, "riderPositions");
        if (positions != null) {
            positions.clear();
        }
    }

    private void presetLocation(String identifier, double lat, double lon) {
        @SuppressWarnings("unchecked")
        Map<String, Location> positions = (Map<String, Location>) ReflectionTestUtils.getField(service, "riderPositions");
        if (positions != null) {
            positions.put(identifier, Location.builder().latitude(lat).longitude(lon).build());
        }
    }
}

