package com.tracker.location_rider.quartz.job;

import com.tracker.location_rider.model.Location;
import com.tracker.location_rider.model.RiderData;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RiderLocationJob implements Job {

    private static final double MIN_LAT = 51.28;
    private static final double MAX_LAT = 51.72;
    private static final double MIN_LON = -0.489;
    private static final double MAX_LON = 0.236;

    private static final int RIDER_COUNT = 10;
    private static final List<RiderData> riders = new ArrayList<>();
    private static final Random random = new Random();

    private static final String RIDE_LOCATION_TOPIC = "rider.location";

    private final KafkaTemplate<String, Object> kafkaTemplate;


    static {
        // Initialize riders
        for (int i = 1; i <= RIDER_COUNT; i++) {
            double latitude = MIN_LAT + (MAX_LAT - MIN_LAT) * random.nextDouble();
            double longitude = MIN_LON + (MAX_LON - MIN_LON) * random.nextDouble();
            riders.add(RiderData.builder().identifier("Id" + i)
                    .location(Location.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .build())
                    .build());
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        riders.get(0).equals(riders.get(1));
        Objects.equals(riders.get(0), riders.get(1));
        Map<RiderData, Long> riderDataLongMap = new HashMap<>();
        riderDataLongMap.get(riders.get(0));

        for (RiderData riderData : riders) {
            // Random movement
            double latChange = (random.nextDouble() - 0.5) * 0.01;
            double lonChange = (random.nextDouble() - 0.5) * 0.01;

            riderData.moveRandomly(latChange, lonChange);

            // Keep riders within bounds
            if (riderData.getLocation().getLatitude() < MIN_LAT) riderData.moveRandomly(MIN_LAT - riderData.getLocation().getLatitude(), 0);
            if (riderData.getLocation().getLatitude() > MAX_LAT) riderData.moveRandomly(MAX_LAT - riderData.getLocation().getLatitude(), 0);

            if (riderData.getLocation().getLongitude() < MIN_LON) riderData.moveRandomly(0, MIN_LON - riderData.getLocation().getLongitude());
            if (riderData.getLocation().getLongitude() > MAX_LON) riderData.moveRandomly(0, MAX_LON - riderData.getLocation().getLongitude());

            try {
                // Publish to Kafka topic
                kafkaTemplate.send(RIDE_LOCATION_TOPIC, riderData).get();
            } catch (Exception e) {
                throw new JobExecutionException("Error publishing to Kafka", e);
            }
        }
    }

}