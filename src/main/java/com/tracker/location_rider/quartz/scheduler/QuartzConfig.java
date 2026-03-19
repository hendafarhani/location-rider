package com.tracker.location_rider.quartz.scheduler;

import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;
import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(QuartzScheduleProperties.class)
public class QuartzConfig {

    private static final Logger log = LoggerFactory.getLogger(QuartzConfig.class);

    private final Scheduler scheduler;
    private final QuartzScheduleProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleRiderLocationJob() throws SchedulerException {
        buildJobRegistrations().forEach(this::scheduleJobSafely);
        startSchedulerIfNeeded();
        logSchedulerStatus();
    }

    private List<JobRegistration> buildJobRegistrations() {
        List<QuartzScheduleProperties.JobConfig> configs = properties.getJobs();
        if (configs == null || configs.isEmpty()) {
            configs = List.of(new QuartzScheduleProperties.JobConfig());
        }

        return configs.stream()
                .map(this::toRegistration)
                .toList();
    }

    private void scheduleJobSafely(JobRegistration registration) {
        try {
            log.info("Scheduling Quartz job '{}' with trigger '{}'",
                    registration.jobDetail().getKey(), registration.trigger().getKey());
            scheduler.scheduleJob(registration.jobDetail(), registration.trigger());
        } catch (SchedulerException ex) {
            String message = "Failed to schedule/start Quartz job '%s' with trigger '%s'"
                    .formatted(registration.jobDetail().getKey(), registration.trigger().getKey());
            log.error(message, ex);
        }
    }

    private JobRegistration toRegistration(QuartzScheduleProperties.JobConfig jobConfig) {
        JobDetail jobDetail = JobBuilder.newJob(jobConfig.getJobClass())
                .withIdentity(jobConfig.getJobId())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobConfig.getTriggerId())
                .startNow()
                .withSchedule(buildSchedule(jobConfig))
                .build();

        return new JobRegistration(jobDetail, trigger);
    }

    private SimpleScheduleBuilder buildSchedule(QuartzScheduleProperties.JobConfig jobConfig) {
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(jobConfig.getIntervalSeconds());

        Integer repeatCount = jobConfig.getRepeatCount();
        if (repeatCount == null || repeatCount < 0) {
            return builder.repeatForever();
        }
        return builder.withRepeatCount(repeatCount);
    }

    private void startSchedulerIfNeeded() throws SchedulerException {
        if (!scheduler.isStarted()) {
            scheduler.start();
            log.info("Quartz scheduler started");
        } else {
            log.debug("Quartz scheduler already running");
        }
    }

    private void logSchedulerStatus() {
        try {
            SchedulerMetaData metaData = scheduler.getMetaData();
            log.info("Quartz scheduler '{}' (instance '{}') - started: {}, standby: {}, jobs executed: {}",
                    metaData.getSchedulerName(),
                    metaData.getSchedulerInstanceId(),
                    metaData.isStarted(),
                    metaData.isInStandbyMode(),
                    metaData.getNumberOfJobsExecuted());
        } catch (SchedulerException ex) {
            log.warn("Unable to read Quartz scheduler metadata", ex);
        }
    }

    @PreDestroy
    public void shutdownScheduler() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown(true);
                log.info("Quartz scheduler shut down gracefully");
            }
        } catch (SchedulerException ex) {
            log.warn("Quartz scheduler shutdown encountered an error", ex);
        }
    }

    private record JobRegistration(JobDetail jobDetail, Trigger trigger) { }
}
