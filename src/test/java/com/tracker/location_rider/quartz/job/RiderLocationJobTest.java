package com.tracker.location_rider.quartz.job;

import com.tracker.location_rider.model.RiderData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderLocationJobTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private RiderLocationJob riderLocationJob;

    @Test
    void execute_shouldSendRiderLocationsToKafka() throws Exception {
        // Arrange
        // Mock KafkaTemplate to simulate successful sends
        when(kafkaTemplate.send(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        riderLocationJob.execute(jobExecutionContext);

        // Assert
        // Capture and verify Kafka interactions
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(10)).send(eq("rider.location"), captor.capture());

        // Validate that all sent objects are RiderData
        List<Object> capturedRiders = captor.getAllValues();
        assertTrue(capturedRiders.stream().allMatch(obj -> obj instanceof RiderData));

        AtomicInteger idCounter = new AtomicInteger(1);
        // Visualize the generated identifiers and validate
        capturedRiders.forEach(riderData -> {
            String expectedId = "Id" + idCounter.getAndIncrement();
            String actualId = ((RiderData) riderData).getIdentifier();
            assertEquals(expectedId, actualId);
        });
    }

    @Test
    void execute_shouldHandleKafkaException() {
        // Arrange
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        // Expect a JobExecutionException when Kafka throws an exception
        Exception exception = assertThrows(JobExecutionException.class, () -> {
            riderLocationJob.execute(jobExecutionContext);
        });

        assertTrue(exception.getMessage().contains("Error publishing to Kafka"));
    }
}
