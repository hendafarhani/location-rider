package com.tracker.location_rider.quartz.scheduler;

import com.tracker.location_rider.quartz.job.RiderLocationJob;
import lombok.Getter;
import lombok.Setter;
import org.quartz.Job;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "location-rider.quartz")
public class QuartzScheduleProperties {

    private List<JobConfig> jobs = new ArrayList<>();

    @Getter
    @Setter
    public static class JobConfig {
        private String jobId = "RiderLocationJob";
        private String triggerId = "triggerIdentity-1";
        private int intervalSeconds = 2;
        private Integer repeatCount = 100;
        private Class<? extends Job> jobClass = RiderLocationJob.class;
    }
}
