package com.tracker.location_rider.quartz.scheduler;

import com.tracker.location_rider.quartz.job.RiderLocationJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class QuartzConfig {

    private final Scheduler scheduler;

    @Bean
    public Scheduler scheduler() throws SchedulerException {

        // Add the job definition and trigger here
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobID", "Job-1");

        JobDetail jobDetail = JobBuilder.newJob(RiderLocationJob.class)
                .withIdentity("RiderLocationJob")
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("triggerIdentity-1")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(2)
                        .withRepeatCount(100))
                        .build();

        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start(); // Ensure scheduler starts
        return scheduler;
    }
}
