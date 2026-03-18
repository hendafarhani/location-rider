package com.tracker.location_rider.quartz.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuartzConfigTest {

    @Mock
    private Scheduler scheduler;

    private QuartzScheduleProperties properties;

    @BeforeEach
    void setUp() {
        properties = new QuartzScheduleProperties();
    }

    @Test
    void scheduleRiderLocationJob_usesDefaultConfigurationWhenNoJobsProvided() throws Exception {
        properties.setJobs(new ArrayList<>());
        QuartzConfig config = new QuartzConfig(scheduler, properties);

        when(scheduler.isStarted()).thenReturn(false);
        when(scheduler.getMetaData()).thenThrow(new SchedulerException("meta data unavailable"));

        config.scheduleRiderLocationJob();

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        verify(scheduler).start();

        assertThat(jobCaptor.getValue().getKey().getName()).isEqualTo("RiderLocationJob");
        assertThat(triggerCaptor.getValue().getKey().getName()).isEqualTo("triggerIdentity-1");
    }

    @Test
    void scheduleRiderLocationJob_registersProvidedJobs() throws Exception {
        QuartzScheduleProperties.JobConfig jobConfig = new QuartzScheduleProperties.JobConfig();
        jobConfig.setJobId("custom-job");
        jobConfig.setTriggerId("custom-trigger");
        jobConfig.setIntervalSeconds(5);
        jobConfig.setRepeatCount(3);
        properties.setJobs(List.of(jobConfig));

        QuartzConfig config = new QuartzConfig(scheduler, properties);

        when(scheduler.isStarted()).thenReturn(true);
        when(scheduler.getMetaData()).thenThrow(new SchedulerException("meta data unavailable"));

        config.scheduleRiderLocationJob();

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        verify(scheduler, never()).start();

        assertThat(jobCaptor.getValue().getKey().getName()).isEqualTo("custom-job");
        assertThat(triggerCaptor.getValue().getKey().getName()).isEqualTo("custom-trigger");
    }

    @Test
    void shutdownScheduler_stopsOnlyWhenNotAlreadyShutdown() throws Exception {
        QuartzConfig config = new QuartzConfig(scheduler, properties);

        when(scheduler.isShutdown()).thenReturn(false);

        config.shutdownScheduler();

        verify(scheduler).shutdown(true);
    }
}

