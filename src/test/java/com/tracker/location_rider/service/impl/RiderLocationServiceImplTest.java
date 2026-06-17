package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
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

    private RiderEntity rider;
    private static final String RIDER_LOCATION_TOPIC = "rider.location";

    @BeforeEach
    void setUp() {
        service = new RiderLocationServiceImpl(kafkaTemplate, riderRepository, RIDER_LOCATION_TOPIC);
        rider = RiderEntity.builder()
                .identifier("rider-1")
                .name("Rider One")
                .build();
    }

    @Test
    void shouldSkipPublishingWhenNoRidersFound() {
        when(riderRepository.findAll()).thenReturn(List.of());

        service.publishLatestLocations();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPublishLocationsForAllRiders() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));

        CompletableFuture<SendResult<String, RiderData>> future = CompletableFuture.completedFuture(null);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RiderData> payloadCaptor = ArgumentCaptor.forClass(RiderData.class);
        doReturn(future).when(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), keyCaptor.capture(), payloadCaptor.capture());

        service.publishLatestLocations();

        verify(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), eq("rider-1"), any(RiderData.class));
        org.assertj.core.api.Assertions.assertThat(keyCaptor.getValue()).isEqualTo("rider-1");
        RiderData sent = payloadCaptor.getValue();
        verifyPayload(sent);
    }

    @Test
    void shouldContinueWhenKafkaSendFails() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));

        CompletableFuture<SendResult<String, RiderData>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        doReturn(future).when(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), eq("rider-1"), any(RiderData.class));

        assertThatCode(() -> service.publishLatestLocations()).doesNotThrowAnyException();
        verify(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), eq("rider-1"), any(RiderData.class));
    }

    @Test void shouldRestoreInterruptStatusWhenKafkaSendInterrupted() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));

        CompletableFuture<SendResult<String, RiderData>> interruptedFuture = new CompletableFuture<>() {
            @Override
            public SendResult<String, RiderData> get() throws InterruptedException {
                throw new InterruptedException("forced-interrupt");
            }

            @Override
            public SendResult<String, RiderData> get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("forced-interrupt");
            }
        };

        doReturn(interruptedFuture).when(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), eq("rider-1"), any(RiderData.class));

        assertThatCode(() -> service.publishLatestLocations()).doesNotThrowAnyException();
        verify(kafkaTemplate).send(eq(RIDER_LOCATION_TOPIC), eq("rider-1"), any(RiderData.class));
    }

    private void verifyPayload(RiderData payload) {
        org.assertj.core.api.Assertions.assertThat(payload).isNotNull();
        org.assertj.core.api.Assertions.assertThat(payload.getIdentifier()).isEqualTo("rider-1");
        org.assertj.core.api.Assertions.assertThat(payload.getUserName()).isEqualTo("Rider One");
    }
}
