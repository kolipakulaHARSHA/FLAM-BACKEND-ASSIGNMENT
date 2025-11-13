package com.hars.queuectl.commands;

import java.time.Instant;
import java.util.Optional;

import com.hars.queuectl.model.Job;
import com.hars.queuectl.service.JobRepository;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "dlq-retry",
    description = "Retry a job from dead letter queue"
)
public class DLQRetryCommand implements Runnable {
    
    @Parameters(index = "0", description = "Job ID to retry")
    private String jobId;
    
    @Override
    public void run() {
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        Optional<Job> optionalJob = jobRepository.findJobById(jobId);
        
        if (optionalJob.isEmpty()) {
            System.err.println("Job not found: " + jobId);
            System.exit(1);
            return;
        }
        
        Job job = optionalJob.get();
        
        if (job.getState() != Job.JobState.DEAD) {
            System.err.println("Job " + jobId + " is not in dead letter queue (current state: " + job.getState() + ")");
            System.exit(1);
            return;
        }
        
        // Reset job for retry
        job.setAttempts(0);
        job.setState(Job.JobState.PENDING);
        job.setUpdatedAt(Instant.now());
        
        jobRepository.updateJob(job);
        
        System.out.println("Job " + jobId + " has been reset and moved back to PENDING state");
    }
}
