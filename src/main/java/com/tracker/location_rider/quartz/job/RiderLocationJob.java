package com.tracker.location_rider.quartz.job;

import com.tracker.location_rider.service.RiderLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Quartz job that delegates rider location simulation to {@link RiderLocationService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiderLocationJob implements Job {

    private final RiderLocationService riderLocationService;

    /**
     * Executes the scheduled job to update and publish rider locations.
     * This method:
     * 1. Delegates the task of publishing latest rider locations to the RiderLocationService
     *
     * @param jobExecutionContext The context provided by Quartz scheduler
     * @throws JobExecutionException if there's an error during job execution
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            riderLocationService.publishLatestLocations();
        } catch (Exception e) {
            throw new JobExecutionException("Error publishing rider locations", e);
        }
    }
}
