package com.tracker.location_rider.quartz.scheduler;

import com.tracker.location_rider.quartz.job.RiderLocationJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@RequiredArgsConstructor
@Configuration
public class QuartzConfig {

    private static final Logger log = LoggerFactory.getLogger(QuartzConfig.class);

    private final Scheduler scheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleRiderLocationJob() throws SchedulerException {
        // Static metadata available to the job during execution.
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobID", "Job-1");

        // Define the job identity and payload.
        JobDetail jobDetail = JobBuilder.newJob(RiderLocationJob.class)
                .withIdentity("RiderLocationJob")
                .usingJobData(jobDataMap)
                .build();

        // Trigger now, then run every 2 seconds for 100 repeats.
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("triggerIdentity-1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(2)
                        .withRepeatCount(100))
                .build();

        try {
            log.info("Scheduling Quartz job '{}' with trigger '{}'", jobDetail.getKey(), trigger.getKey());
            scheduler.scheduleJob(jobDetail, trigger);

            if (!scheduler.isStarted()) {
                scheduler.start();
                log.info("Quartz scheduler started");
            } else {
                log.debug("Quartz scheduler already running");
            }
        } catch (SchedulerException ex) {
            log.error("Failed to schedule/start Quartz job '{}'", jobDetail.getKey(), ex);
            throw ex;
        }
    }
}
