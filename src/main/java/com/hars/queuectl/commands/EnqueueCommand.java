package com.hars.queuectl.commands;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hars.queuectl.model.Job;
import com.hars.queuectl.service.JobRepository;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "enqueue",
    description = "Add a new job to the queue"
)
public class EnqueueCommand implements Runnable {
    
    @Parameters(index = "0", description = "Job JSON string")
    private String jobJson;
    
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    
    public EnqueueCommand() {
        this.jobRepository = new JobRepository();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.jobRepository.initialize();
    }
    
    @Override
    public void run() {
        try {
            // Parse the JSON string into a Job object
            Job job = objectMapper.readValue(jobJson, Job.class);
            
            // Set default values
            if (job.getId() == null || job.getId().isEmpty()) {
                job.setId(UUID.randomUUID().toString());
            }
            job.setState(Job.JobState.PENDING);
            job.setAttempts(0);
            
            // Set default max_retries if not provided
            if (job.getMaxRetries() == 0) {
                job.setMaxRetries(3);
            }
            
            Instant now = Instant.now();
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            
            // Add job to repository
            jobRepository.addJob(job);
            
            System.out.println("Job enqueued successfully: " + job.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to enqueue job: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
