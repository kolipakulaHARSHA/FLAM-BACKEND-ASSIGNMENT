package com.hars.queuectl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hars.queuectl.model.Job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class JobRepository {
    private static final String JOBS_FILE = "jobs.json";
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock;
    private final Lock readLock;
    private final Lock writeLock;

    public JobRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Ensures the jobs.json file exists
     */
    public void initialize() {
        writeLock.lock();
        try {
            File file = new File(JOBS_FILE);
            if (!file.exists()) {
                // Create empty jobs list
                objectMapper.writeValue(file, new ArrayList<Job>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize jobs file", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Finds the next job in PENDING state
     */
    public Optional<Job> findNextPendingJob() {
        readLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            return jobs.stream()
                    .filter(job -> job.getState() == Job.JobState.PENDING)
                    .findFirst();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Atomically finds and locks the next pending job by changing its state to PROCESSING.
     * This prevents multiple workers from picking up the same job.
     * 
     * @return Optional containing the job if found and locked, empty otherwise
     */
    public Optional<Job> findAndLockNextPendingJob() {
        writeLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            
            // Find the first pending job
            Optional<Job> pendingJob = jobs.stream()
                    .filter(job -> job.getState() == Job.JobState.PENDING)
                    .findFirst();
            
            if (pendingJob.isPresent()) {
                Job job = pendingJob.get();
                
                // Atomically change state to PROCESSING
                job.setState(Job.JobState.PROCESSING);
                job.setUpdatedAt(java.time.Instant.now());
                
                // Update in the list
                for (int i = 0; i < jobs.size(); i++) {
                    if (jobs.get(i).getId().equals(job.getId())) {
                        jobs.set(i, job);
                        break;
                    }
                }
                
                // Write back to file
                writeJobsToFile(jobs);
                
                return Optional.of(job);
            }
            
            return Optional.empty();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Adds a new job to the store
     */
    public void addJob(Job job) {
        writeLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            jobs.add(job);
            writeJobsToFile(jobs);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates an existing job in the store
     */
    public void updateJob(Job updatedJob) {
        writeLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get(i).getId().equals(updatedJob.getId())) {
                    jobs.set(i, updatedJob);
                    break;
                }
            }
            writeJobsToFile(jobs);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Retrieves all jobs
     */
    public List<Job> getAllJobs() {
        readLock.lock();
        try {
            return new ArrayList<>(readJobsFromFile());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Filters jobs by state
     */
    public List<Job> getJobsByState(Job.JobState state) {
        readLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            return jobs.stream()
                    .filter(job -> job.getState() == state)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Gets all jobs with state DEAD (Dead Letter Queue)
     */
    public List<Job> getDeadLetterQueue() {
        return getJobsByState(Job.JobState.DEAD);
    }

    /**
     * Finds a job by ID
     */
    public Optional<Job> findJobById(String jobId) {
        readLock.lock();
        try {
            List<Job> jobs = readJobsFromFile();
            return jobs.stream()
                    .filter(job -> job.getId().equals(jobId))
                    .findFirst();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Reads jobs from the file (internal helper method)
     */
    private List<Job> readJobsFromFile() {
        try {
            File file = new File(JOBS_FILE);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<Job>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read jobs from file", e);
        }
    }

    /**
     * Writes jobs to the file (internal helper method)
     */
    private void writeJobsToFile(List<Job> jobs) {
        try {
            objectMapper.writeValue(new File(JOBS_FILE), jobs);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write jobs to file", e);
        }
    }
}
