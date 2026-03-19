package com.tracker.location_rider.quartz.scheduler;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuartzConfigTest {

    @Mock
    private Scheduler scheduler;

    private QuartzScheduleProperties properties;

    private QuartzConfig config;

    @BeforeEach
    void setUp() {
        properties = new QuartzScheduleProperties();
        properties.setJobs(Collections.singletonList(new QuartzScheduleProperties.JobConfig()));
        config = new QuartzConfig(scheduler, properties);
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

    @Test
    void shouldLogAndContinueWhenSchedulingJobFails() throws SchedulerException {
        SchedulerMetaData metaData = mock(SchedulerMetaData.class);
        when(metaData.getSchedulerName()).thenReturn("test-scheduler");
        when(metaData.getSchedulerInstanceId()).thenReturn("instance-1");
        when(metaData.isInStandbyMode()).thenReturn(false);
        when(metaData.getNumberOfJobsExecuted()).thenReturn(0);
        when(scheduler.getMetaData()).thenReturn(metaData);
        when(scheduler.isStarted()).thenReturn(false);

        doThrow(new SchedulerException("boom"))
                .when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        Assertions.assertThatCode(() -> config.scheduleRiderLocationJob())
                .doesNotThrowAnyException();

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler).start();
    }
}
