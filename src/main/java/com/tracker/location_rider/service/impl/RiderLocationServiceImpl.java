package com.tracker.location_rider.service.impl;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import com.tracker.location_rider.repository.RiderRepository;
import com.tracker.location_rider.service.RiderLocationService;
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

    public void publishLatestLocations() throws Exception {
        List<RiderEntity> riders = riderRepository.findAll();
        if (riders.isEmpty()) {
            log.warn("No riders found in MySQL. Location simulation skipped.");
            return;
        }

        log.info("Starting rider location update job - processing {} riders", riders.size());
        int successCount = 0;

        for (RiderEntity riderEntity : riders) {
            successCount += handleRider(riderEntity);
        }

        log.info("Rider location update job completed - Success: {}, Errors: {}",
                successCount,
                riders.size() - successCount);
    }

    private int handleRider(RiderEntity riderEntity) throws Exception {
        try {
            RiderData riderData = RiderData.builder()
                    .identifier(riderEntity.getIdentifier())
                    .userName(riderEntity.getName())
                    .location(currentLocationFor(riderEntity))
                    .build();

            applyRandomMovement(riderData);
            enforceBoundaries(riderData);

            kafkaTemplate.send(RIDE_LOCATION_TOPIC, riderData).get();
            log.debug("Successfully published location for rider {} to Kafka topic '{}'",
                    riderData.getIdentifier(), RIDE_LOCATION_TOPIC);
            return 1;
        } catch (Exception ex) {
            log.error("Error processing location update for rider {}: {}",
                    riderEntity.getIdentifier(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private Location currentLocationFor(RiderEntity riderEntity) {
        return riderPositions.computeIfAbsent(
                riderEntity.getIdentifier(),
                ignored -> randomLondonLocation()
        );
    }

    private void applyRandomMovement(RiderData riderData) {
        double latChange = (random.nextDouble() - 0.5) * 0.01;
        double lonChange = (random.nextDouble() - 0.5) * 0.01;

        log.debug("Rider {} moving from [{}, {}] by delta [{}, {}]",
                riderData.getIdentifier(),
                riderData.getLocation().getLatitude(),
                riderData.getLocation().getLongitude(),
                latChange,
                lonChange);

        riderData.moveRandomly(latChange, lonChange);
    }

    private void enforceBoundaries(RiderData riderData) {
        Location location = riderData.getLocation();

        if (location.getLatitude() < MIN_LAT) {
            riderData.moveRandomly(MIN_LAT - location.getLatitude(), 0);
        } else if (location.getLatitude() > MAX_LAT) {
            riderData.moveRandomly(MAX_LAT - location.getLatitude(), 0);
        }

        if (location.getLongitude() < MIN_LON) {
            riderData.moveRandomly(0, MIN_LON - location.getLongitude());
        } else if (location.getLongitude() > MAX_LON) {
            riderData.moveRandomly(0, MAX_LON - location.getLongitude());
        }
    }

    private Location randomLondonLocation() {
        return Location.builder()
                .latitude(MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble())
                .longitude(MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble())
                .build();
    }
}
