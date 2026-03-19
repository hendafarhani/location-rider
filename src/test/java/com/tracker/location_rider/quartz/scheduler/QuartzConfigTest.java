package com.tracker.location_rider.quartz.scheduler;

import com.tracker.location_rider.quartz.job.RiderLocationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuartzConfigTest {

    @Mock
    private Scheduler scheduler;

    private QuartzScheduleProperties properties;

    private QuartzConfig config;

    @BeforeEach
    void setUp() throws SchedulerException {
        properties = new QuartzScheduleProperties();
        config = new QuartzConfig(scheduler, properties);
    }

    @Test
    void shouldScheduleDefaultJobWhenConfigEmpty() throws SchedulerException {
        properties.setJobs(new ArrayList<>());
        seedSchedulerMetaData(true);

        config.scheduleRiderLocationJob();

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        verify(scheduler).start();
        assertThat(jobCaptor.getValue().getJobClass()).isEqualTo(RiderLocationJob.class);
        assertThat(jobCaptor.getValue().getKey().getName()).isEqualTo("RiderLocationJob");
        assertThat(triggerCaptor.getValue().getKey().getName()).isEqualTo("triggerIdentity-1");
    }

    @Test
    void shouldScheduleProvidedJobsWithoutRestartingRunningScheduler() throws SchedulerException {
        QuartzScheduleProperties.JobConfig jobConfig = new QuartzScheduleProperties.JobConfig();
        jobConfig.setJobId("customJob");
        jobConfig.setTriggerId("customTrigger");
        jobConfig.setIntervalSeconds(5);
        jobConfig.setRepeatCount(2);
        properties.setJobs(List.of(jobConfig));
        config = new QuartzConfig(scheduler, properties);
        seedSchedulerMetaData(true);
        when(scheduler.isStarted()).thenReturn(true);

        config.scheduleRiderLocationJob();

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        verify(scheduler, never()).start();
        assertThat(jobCaptor.getValue().getKey().getName()).isEqualTo("customJob");
        assertThat(triggerCaptor.getValue().getKey().getName()).isEqualTo("customTrigger");
    }

    @Test
    void shouldLogAndContinueWhenSchedulingFails() throws SchedulerException {
        properties.setJobs(List.of(new QuartzScheduleProperties.JobConfig()));
        when(scheduler.isStarted()).thenReturn(false);
        seedSchedulerMetaData(false);
        doThrow(new SchedulerException("boom"))
                .when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        assertThatCode(() -> config.scheduleRiderLocationJob()).doesNotThrowAnyException();
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler).start();
    }

    @Test
    void shouldShutdownSchedulerGracefully() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);

        config.shutdownScheduler();

        verify(scheduler).shutdown(true);
    }

    @Test
    void shouldSkipShutdownWhenAlreadyStopped() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);

        config.shutdownScheduler();

        verify(scheduler, never()).shutdown(true);
    }

    @Test
    void shouldSwallowShutdownExceptions() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        doThrow(new SchedulerException("shutdown-failure"))
                .when(scheduler).shutdown(true);

        assertThatCode(() -> config.shutdownScheduler()).doesNotThrowAnyException();
        verify(scheduler).shutdown(true);
    }

    private void seedSchedulerMetaData(boolean started) throws SchedulerException {
        SchedulerMetaData metaData = mock(SchedulerMetaData.class);
        when(metaData.getSchedulerName()).thenReturn("testScheduler");
        when(metaData.getSchedulerInstanceId()).thenReturn("instance-1");
        when(metaData.isStarted()).thenReturn(started);
        when(metaData.isInStandbyMode()).thenReturn(false);
        when(metaData.getNumberOfJobsExecuted()).thenReturn(0);
        when(scheduler.getMetaData()).thenReturn(metaData);
    }
}
