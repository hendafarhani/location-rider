package com.tracker.location_rider.quartz.job;

import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Quartz job that simulates rider location updates and publishes them to Kafka.
 * This job periodically updates the positions of simulated riders within London boundaries
 * and sends their location data to the rider. Location Kafka topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiderLocationJob implements Job {

    // Geographic boundaries for London area (latitude and longitude constraints)
    private static final double MIN_LAT = 51.28;
    private static final double MAX_LAT = 51.72;
    private static final double MIN_LON = -0.489;
    private static final double MAX_LON = 0.236;

    // Number of simulated riders to track
    private static final int RIDER_COUNT = 10;

    // In-memory storage for rider data (shared across job executions)
    private static final List<RiderData> riders = new ArrayList<>();

    // Random number generator for simulating movement
    private static final Random random = new Random();

    // Kafka topic name for publishing rider location updates
    private static final String RIDE_LOCATION_TOPIC = "rider.location";

    // Kafka template for publishing messages
    private final KafkaTemplate<String, Object> kafkaTemplate;

    static {
        log.info("Initializing {} simulated riders within London boundaries", RIDER_COUNT);

        // Initialize riders with random positions within London boundaries
        for (int i = 1; i <= RIDER_COUNT; i++) {
            double latitude = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
            double longitude = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();

            riders.add(RiderData.builder()
                    .identifier("Id" + i)
                    .location(Location.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .build())
                    .build());

            log.debug("Initialized rider Id{} at position [{}, {}]", i, latitude, longitude);
        }

        log.info("Successfully initialized all {} riders", RIDER_COUNT);
    }

    /**
     * Executes the scheduled job to update and publish rider locations.
     * This method:
     * 1. Simulates random movement for each rider
     * 2. Ensures riders stay within geographic boundaries
     * 3. Publishes updated locations to Kafka
     *
     * @param jobExecutionContext The context provided by Quartz scheduler
     * @throws JobExecutionException if there's an error publishing to Kafka
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Starting rider location update job - processing {} riders", riders.size());
        int successCount = 0;
        int errorCount = 0;

        for (RiderData riderData : riders) {
            try {
                // Store original position for logging
                double originalLat = riderData.getLocation().getLatitude();
                double originalLon = riderData.getLocation().getLongitude();

                // Generate random movement deltas (maximum ~1km in any direction)
                // 0.01 degrees is approximately 1.1 km at London's latitude
                double latChange = (random.nextDouble() - 0.5) * 0.01;
                double lonChange = (random.nextDouble() - 0.5) * 0.01;

                log.debug("Rider {} moving from [{}, {}] by delta [{}, {}]",
                        riderData.getIdentifier(), originalLat, originalLon, latChange, lonChange);

                // Apply the random movement
                riderData.moveRandomly(latChange, lonChange);

                // Enforce geographic boundaries - keep riders within London area
                // If a rider crosses a boundary, push them back inside

                // Check and correct southern boundary
                if (riderData.getLocation().getLatitude() < MIN_LAT) {
                    log.debug("Rider {} hit southern boundary, correcting position", riderData.getIdentifier());
                    riderData.moveRandomly(MIN_LAT - riderData.getLocation().getLatitude(), 0);
                }

                // Check and correct northern boundary
                if (riderData.getLocation().getLatitude() > MAX_LAT) {
                    log.debug("Rider {} hit northern boundary, correcting position", riderData.getIdentifier());
                    riderData.moveRandomly(MAX_LAT - riderData.getLocation().getLatitude(), 0);
                }

                // Check and correct western boundary
                if (riderData.getLocation().getLongitude() < MIN_LON) {
                    log.debug("Rider {} hit western boundary, correcting position", riderData.getIdentifier());
                    riderData.moveRandomly(0, MIN_LON - riderData.getLocation().getLongitude());
                }

                // Check and correct eastern boundary
                if (riderData.getLocation().getLongitude() > MAX_LON) {
                    log.debug("Rider {} hit eastern boundary, correcting position", riderData.getIdentifier());
                    riderData.moveRandomly(0, MAX_LON - riderData.getLocation().getLongitude());
                }

                log.debug("Rider {} final position: [{}, {}]",
                        riderData.getIdentifier(),
                        riderData.getLocation().getLatitude(),
                        riderData.getLocation().getLongitude());

                // Publish the updated location to Kafka topic
                // Using .get() to wait for acknowledgment (blocking call for reliability)
                kafkaTemplate.send(RIDE_LOCATION_TOPIC, riderData).get();

                log.debug("Successfully published location for rider {} to Kafka topic '{}'",
                        riderData.getIdentifier(), RIDE_LOCATION_TOPIC);

                successCount++;

            } catch (Exception e) {
                errorCount++;
                log.error("Error processing location update for rider {}: {}",
                        riderData.getIdentifier(), e.getMessage(), e);
                throw new JobExecutionException("Error publishing to Kafka for rider " + riderData.getIdentifier(), e);
            }
        }

        log.info("Rider location update job completed - Success: {}, Errors: {}", successCount, errorCount);
    }

}