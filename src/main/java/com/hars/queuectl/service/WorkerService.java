package com.hars.queuectl.service;

import java.io.File;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hars.queuectl.model.Job;
import com.hars.queuectl.worker.JobExecutor;

public class WorkerService {
    
    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;
    private ExecutorService executorService;
    private volatile boolean running = false;
    
    // Configuration
    private static final long POLL_INTERVAL_MS = 500;
    private static final long BASE_BACKOFF_MS = 1000; // 1 second base delay
    private static final String STOP_SIGNAL_FILE = "worker.stop";
    
    public WorkerService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
        this.jobExecutor = new JobExecutor();
    }
    
    /**
     * Start worker threads
     * @param workerCount Number of worker threads to start
     */
    public void start(int workerCount) {
        if (running) {
            System.out.println("Workers are already running");
            return;
        }
        
        System.out.println("Starting " + workerCount + " worker(s)...");
        running = true;
        executorService = Executors.newFixedThreadPool(workerCount);
        
        // Submit worker tasks
        for (int i = 0; i < workerCount; i++) {
            final int workerId = i + 1;
            executorService.submit(() -> workerLoop(workerId));
        }
        
        System.out.println("Workers started successfully");
    }
    
    /**
     * Stop all worker threads gracefully
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        System.out.println("\nShutting down workers...");
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Workers stopped");
    }
    
    /**
     * Main worker loop that processes jobs
     * @param workerId The ID of this worker
     */
    private void workerLoop(int workerId) {
        System.out.println("Worker " + workerId + " started");
        
        while (running) {
            try {
                // Check for stop signal before picking up new job
                File stopSignal = new File(STOP_SIGNAL_FILE);
                if (stopSignal.exists()) {
                    System.out.println("Worker " + workerId + " received stop signal. Finishing current tasks...");
                    running = false;
                    break;
                }
                
                // Atomically find and lock the next pending job
                Optional<Job> optionalJob = jobRepository.findAndLockNextPendingJob();
                
                if (optionalJob.isPresent()) {
                    Job job = optionalJob.get();
                    
                    System.out.println("Worker " + workerId + " picked up job: " + job.getId());
                    
                    // Execute the job
                    int exitCode = jobExecutor.execute(job);
                    
                    // Handle the result
                    if (exitCode == 0) {
                        // Success
                        job.setState(Job.JobState.COMPLETED);
                        job.setUpdatedAt(Instant.now());
                        jobRepository.updateJob(job);
                        System.out.println("Worker " + workerId + " completed job: " + job.getId());
                    } else if (exitCode == JobExecutor.EXIT_CODE_TIMEOUT) {
                        // Timeout - treat as failure but with specific message
                        job.setAttempts(job.getAttempts() + 1);
                        job.setUpdatedAt(Instant.now());
                        
                        if (job.getAttempts() < job.getMaxRetries()) {
                            // Retry with exponential backoff
                            job.setState(Job.JobState.FAILED);
                            jobRepository.updateJob(job);
                            
                            long backoffDelay = calculateBackoff(job.getAttempts());
                            System.out.println("Worker " + workerId + " - Job " + job.getId() + 
                                    " timed out (attempt " + job.getAttempts() + "/" + job.getMaxRetries() + 
                                    "), retrying in " + backoffDelay + "ms");
                            
                            Thread.sleep(backoffDelay);
                            
                            // Set back to PENDING for retry
                            job.setState(Job.JobState.PENDING);
                            job.setUpdatedAt(Instant.now());
                            jobRepository.updateJob(job);
                        } else {
                            // Max retries reached, move to dead letter queue
                            job.setState(Job.JobState.DEAD);
                            jobRepository.updateJob(job);
                            System.out.println("Worker " + workerId + " - Job " + job.getId() + 
                                    " moved to dead letter queue after timeout (" + job.getAttempts() + " attempts)");
                        }
                    } else {
                        // Other failure
                        job.setAttempts(job.getAttempts() + 1);
                        job.setUpdatedAt(Instant.now());
                        
                        if (job.getAttempts() < job.getMaxRetries()) {
                            // Retry with exponential backoff
                            job.setState(Job.JobState.FAILED);
                            jobRepository.updateJob(job);
                            
                            long backoffDelay = calculateBackoff(job.getAttempts());
                            System.out.println("Worker " + workerId + " - Job " + job.getId() + 
                                    " failed (attempt " + job.getAttempts() + "/" + job.getMaxRetries() + 
                                    "), retrying in " + backoffDelay + "ms");
                            
                            Thread.sleep(backoffDelay);
                            
                            // Set back to PENDING for retry
                            job.setState(Job.JobState.PENDING);
                            job.setUpdatedAt(Instant.now());
                            jobRepository.updateJob(job);
                        } else {
                            // Max retries reached, move to dead letter queue
                            job.setState(Job.JobState.DEAD);
                            jobRepository.updateJob(job);
                            System.out.println("Worker " + workerId + " - Job " + job.getId() + 
                                    " moved to dead letter queue after " + job.getAttempts() + " attempts");
                        }
                    }
                } else {
                    // No jobs available, sleep for a bit
                    Thread.sleep(POLL_INTERVAL_MS);
                }
                
            } catch (InterruptedException e) {
                System.out.println("Worker " + workerId + " interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " encountered error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Worker " + workerId + " stopped");
    }
    
    /**
     * Calculate exponential backoff delay
     * @param attempts Number of attempts
     * @return Delay in milliseconds
     */
    private long calculateBackoff(int attempts) {
        return (long) (BASE_BACKOFF_MS * Math.pow(2, attempts - 1));
    }
    
    /**
     * Check if workers are running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
