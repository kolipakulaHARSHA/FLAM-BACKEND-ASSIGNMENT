package com.hars.queuectl.commands;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hars.queuectl.model.Job;
import com.hars.queuectl.service.JobRepository;

import picocli.CommandLine.Command;

@Command(
    name = "list",
    description = "List dead letter queue jobs"
)
public class DLQListCommand implements Runnable {
    
    @Override
    public void run() {
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        List<Job> deadJobs = jobRepository.getDeadLetterQueue();
        
        System.out.println("Dead Letter Queue (" + deadJobs.size() + " jobs):");
        System.out.println("==========================================");
        
        if (deadJobs.isEmpty()) {
            System.out.println("No jobs in dead letter queue");
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                
                String json = mapper.writeValueAsString(deadJobs);
                System.out.println(json);
            } catch (Exception e) {
                System.err.println("Failed to serialize jobs: " + e.getMessage());
            }
        }
    }
}
