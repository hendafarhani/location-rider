package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import com.tracker.location_rider.repository.RiderRepository;
import com.tracker.location_rider.service.RiderLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RiderLocationServiceImpl implements RiderLocationService {

    private static final double MIN_LAT = 51.28;
    private static final double MAX_LAT = 51.72;
    private static final double MIN_LON = -0.489;
    private static final double MAX_LON = 0.236;

    private final KafkaTemplate<String, RiderData> kafkaTemplate;
    private final RiderRepository riderRepository;
    private final String riderLocationTopic;

    public RiderLocationServiceImpl(
            KafkaTemplate<String, RiderData> kafkaTemplate,
            RiderRepository riderRepository,
            @Value("${kafka.topics.rider-location}") String riderLocationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.riderRepository = riderRepository;
        this.riderLocationTopic = riderLocationTopic;
    }

    private final Map<String, Location> riderPositions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void publishLatestLocations() {
        List<RiderEntity> riders = riderRepository.findAll();
        if (shouldSkipLocationPublishing(riders)) {
            return;
        }

        logLocationPublishingStarted(riders);
        PublishSummary summary = publishLocationsFor(riders);
        logLocationPublishingCompleted(summary);
    }

    private boolean shouldSkipLocationPublishing(List<RiderEntity> riders) {
        if (!riders.isEmpty()) {
            return false;
        }
        log.warn("No riders found in MySQL. Location simulation skipped.");
        return true;
    }

    private void logLocationPublishingStarted(List<RiderEntity> riders) {
        log.info("Starting rider location update job - processing {} riders", riders.size());
    }

    private PublishSummary publishLocationsFor(List<RiderEntity> riders) {
        int successCount = 0;
        for (RiderEntity rider : riders) {
            if (publishLocationFor(rider)) {
                successCount++;
            }
        }
        return new PublishSummary(riders.size(), successCount);
    }

    private void logLocationPublishingCompleted(PublishSummary summary) {
        log.info("Rider location update job completed - Success: {}, Errors: {}",
                summary.successCount(),
                summary.errorCount());
    }

    private boolean publishLocationFor(RiderEntity rider) {
        try {
            RiderData riderData = buildRiderData(rider);
            publishToKafka(riderData);
            logSuccessfulPublish(riderData);
            return true;
        } catch (InterruptedException ie) {
            handleInterruptedPublish(rider, ie);
            return false;
        } catch (Exception ex) {
            handleFailedPublish(rider, ex);
            return false;
        }
    }

    private void publishToKafka(RiderData riderData) throws Exception {
        kafkaTemplate.send(riderLocationTopic, riderData.getIdentifier(), riderData).get();
    }

    private void logSuccessfulPublish(RiderData riderData) {
        log.debug("Successfully published location for rider {} to Kafka topic '{}'",
                riderData.getIdentifier(), riderLocationTopic);
    }

    private void handleInterruptedPublish(RiderEntity rider, InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Publishing interrupted for rider {}", rider.getIdentifier(), exception);
    }

    private void handleFailedPublish(RiderEntity rider, Exception exception) {
        log.error("Error processing location update for rider {}: {}",
                rider.getIdentifier(), exception.getMessage(), exception);
        log.warn("Failed to publish location for rider {}", rider.getIdentifier());
    }

    private RiderData buildRiderData(RiderEntity riderEntity) {
        Location nextLocation = nextBoundedLocation(riderEntity);
        return RiderData.builder()
                .identifier(riderEntity.getIdentifier())
                .userName(riderEntity.getName())
                .location(nextLocation)
                .build();
    }

    private Location currentLocationFor(RiderEntity riderEntity) {
        return riderPositions.computeIfAbsent(
                riderEntity.getIdentifier(),
                ignored -> randomLondonLocation()
        );
    }

    private Location nextBoundedLocation(RiderEntity riderEntity) {
        Location current = currentLocationFor(riderEntity);
        Location moved = applyRandomMovement(current, riderEntity.getIdentifier());
        Location bounded = clampToBounds(moved);
        riderPositions.put(riderEntity.getIdentifier(), bounded);
        return bounded;
    }

    private Location applyRandomMovement(Location current, String riderIdentifier) {
        double latChange = (random.nextDouble() - 0.5) * 0.01;
        double lonChange = (random.nextDouble() - 0.5) * 0.01;

        log.debug("Rider {} moving from [{}, {}] by delta [{}, {}]",
                riderIdentifier,
                current.getLatitude(),
                current.getLongitude(),
                latChange,
                lonChange);

        return Location.builder()
                .latitude(current.getLatitude() + latChange)
                .longitude(current.getLongitude() + lonChange)
                .build();
    }

    private Location clampToBounds(Location location) {

        double boundedLat = Math.clamp(location.getLatitude(), MIN_LAT, MAX_LAT);
        double boundedLon = Math.clamp(location.getLongitude(), MIN_LON, MAX_LON);

        return Location.builder()
                .latitude(boundedLat)
                .longitude(boundedLon)
                .build();
    }


    private Location randomLondonLocation() {
        return Location.builder()
                .latitude(MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble())
                .longitude(MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble())
                .build();
    }

    private record PublishSummary(int totalCount, int successCount) {

        int errorCount() {
            return totalCount - successCount;
        }
    }
}
