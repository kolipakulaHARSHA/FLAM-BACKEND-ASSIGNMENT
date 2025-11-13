package com.hars.queuectl.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hars.queuectl.model.Job;
import com.hars.queuectl.service.JobRepository;

import picocli.CommandLine.Command;

@Command(
    name = "status",
    description = "Show queue status"
)
public class StatusCommand implements Runnable {
    
    @Override
    public void run() {
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        List<Job> allJobs = jobRepository.getAllJobs();
        
        // Count jobs by state
        Map<Job.JobState, Integer> stateCounts = new HashMap<>();
        for (Job.JobState state : Job.JobState.values()) {
            stateCounts.put(state, 0);
        }
        
        for (Job job : allJobs) {
            stateCounts.put(job.getState(), stateCounts.get(job.getState()) + 1);
        }
        
        // Print status
        System.out.println("Queue Status:");
        System.out.println("=============");
        System.out.println("Total Jobs: " + allJobs.size());
        System.out.println();
        for (Job.JobState state : Job.JobState.values()) {
            System.out.println(state + ": " + stateCounts.get(state));
        }
    }
}
