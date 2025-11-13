package com.hars.queuectl.commands;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hars.queuectl.model.Job;
import com.hars.queuectl.service.JobRepository;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "list",
    description = "List jobs"
)
public class ListCommand implements Runnable {
    
    @Option(names = {"--state", "-s"}, description = "Filter by state (PENDING, PROCESSING, COMPLETED, FAILED, DEAD)")
    private String state;
    
    @Override
    public void run() {
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        List<Job> jobs;
        
        if (state != null && !state.isEmpty()) {
            try {
                Job.JobState jobState = Job.JobState.valueOf(state.toUpperCase());
                jobs = jobRepository.getJobsByState(jobState);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid state: " + state);
                System.err.println("Valid states: PENDING, PROCESSING, COMPLETED, FAILED, DEAD");
                return;
            }
        } else {
            jobs = jobRepository.getAllJobs();
        }
        
        // Print jobs as formatted JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            String json = mapper.writeValueAsString(jobs);
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("Failed to serialize jobs: " + e.getMessage());
        }
    }
}
