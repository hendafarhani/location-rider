package com.tracker.location_rider.quartz.job;

import com.tracker.location_rider.service.RiderLocationService;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RiderLocationJobTest {

    @Test
    void shouldDelegateToRiderLocationService() {
        AtomicInteger invocationCount = new AtomicInteger();
        RiderLocationService riderLocationService = invocationCount::incrementAndGet;
        RiderLocationJob riderLocationJob = new RiderLocationJob(riderLocationService);

        assertThatCode(() -> riderLocationJob.execute(null)).doesNotThrowAnyException();
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    void shouldWrapServiceExceptionAsJobExecutionException() {
        RuntimeException failure = new RuntimeException("service-failure");
        RiderLocationService riderLocationService = () -> {
            throw failure;
        };
        RiderLocationJob riderLocationJob = new RiderLocationJob(riderLocationService);

        assertThatExceptionOfType(JobExecutionException.class)
                .isThrownBy(() -> riderLocationJob.execute(null))
                .withMessage("Error publishing rider locations")
                .withCause(failure);
    }
}

