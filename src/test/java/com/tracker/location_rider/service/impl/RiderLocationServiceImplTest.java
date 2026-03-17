package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import com.tracker.location_rider.repository.RiderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        clearCachedPositions();
    }

    @Test
    void publishLatestLocations_skipsKafkaWhenNoRidersFound() throws Exception {
        when(riderRepository.findAll()).thenReturn(List.of());

        service.publishLatestLocations();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishLatestLocations_publishesForPersistedRider() throws Exception {
        RiderEntity rider = RiderEntity.builder()
                .identifier("rider-1")
                .name("Test Rider")
                .build();

        when(riderRepository.findAll()).thenReturn(List.of(rider));
        presetLocation("rider-1", 51.50, -0.12);

        CompletableFuture<SendResult<String, RiderData>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(eq("rider.location"), any(RiderData.class)))
                .thenReturn(future);

        service.publishLatestLocations();

        verify(kafkaTemplate).send(eq("rider.location"), any(RiderData.class));
    }

    @Test
    void publishLatestLocations_propagatesKafkaFailures() {
        RiderEntity rider = RiderEntity.builder()
                .identifier("rider-error")
                .name("Error Rider")
                .build();
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        presetLocation("rider-error", 51.50, -0.12);
        when(kafkaTemplate.send(eq("rider.location"), any(RiderData.class)))
                .thenThrow(new IllegalStateException("Kafka unavailable"));

        assertThatThrownBy(() -> service.publishLatestLocations())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka unavailable");
    }

    private void clearCachedPositions() {
        @SuppressWarnings("unchecked")
        Map<String, Location> positions = (Map<String, Location>) ReflectionTestUtils
                .getField(service, "riderPositions");
        if (positions != null) {
            positions.clear();
        }
    }

    private void presetLocation(String identifier, double lat, double lon) {
        @SuppressWarnings("unchecked")
        Map<String, Location> positions = (Map<String, Location>) ReflectionTestUtils
                .getField(service, "riderPositions");
        if (positions != null) {
            positions.put(identifier, Location.builder().latitude(lat).longitude(lon).build());
        }
    }
}
