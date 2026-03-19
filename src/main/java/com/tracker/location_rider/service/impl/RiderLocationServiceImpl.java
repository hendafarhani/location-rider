package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import com.tracker.location_rider.repository.RiderRepository;
import com.tracker.location_rider.service.RiderLocationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiderLocationServiceImpl implements RiderLocationService {

    private static final double MIN_LAT = 51.28;
    private static final double MAX_LAT = 51.72;
    private static final double MIN_LON = -0.489;
    private static final double MAX_LON = 0.236;
    private static final String RIDE_LOCATION_TOPIC = "rider.location";

    private final KafkaTemplate<String, RiderData> kafkaTemplate;
    private final RiderRepository riderRepository;

    private final Map<String, Location> riderPositions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void publishLatestLocations() {
        List<RiderEntity> riders = riderRepository.findAll();
        if (riders.isEmpty()) {
            log.warn("No riders found in MySQL. Location simulation skipped.");
            return;
        }

        log.info("Starting rider location update job - processing {} riders", riders.size());
        int successCount = Math.toIntExact(riders.stream()
                .filter(this::sendRiderLocation)
                .count());

        log.info("Rider location update job completed - Success: {}, Errors: {}",
                successCount,
                riders.size() - successCount);
    }

    private boolean sendRiderLocation(RiderEntity riderEntity) {
        try {
            RiderData riderData = buildRiderData(riderEntity);
            kafkaTemplate.send(RIDE_LOCATION_TOPIC, riderData).get();
            log.debug("Successfully published location for rider {} to Kafka topic '{}'",
                    riderData.getIdentifier(), RIDE_LOCATION_TOPIC);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Publishing interrupted for rider {}", riderEntity.getIdentifier(), ie);
            return false;
        } catch (Exception ex) {
            log.error("Error processing location update for rider {}: {}",
                    riderEntity.getIdentifier(), ex.getMessage(), ex);
            return false;
        }
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
        double boundedLat = Math.max(MIN_LAT, Math.min(MAX_LAT, location.getLatitude()));
        double boundedLon = Math.max(MIN_LON, Math.min(MAX_LON, location.getLongitude()));
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
}
